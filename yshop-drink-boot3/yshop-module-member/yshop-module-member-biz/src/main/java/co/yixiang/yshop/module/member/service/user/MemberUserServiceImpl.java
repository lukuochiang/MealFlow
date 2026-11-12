package co.yixiang.yshop.module.member.service.user;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import co.yixiang.yshop.framework.common.enums.CommonStatusEnum;
import co.yixiang.yshop.framework.common.enums.ShopCommonEnum;
import co.yixiang.yshop.module.coupon.dal.dataobject.couponuser.CouponUserDO;
import co.yixiang.yshop.module.coupon.service.couponuser.AppCouponUserService;
import co.yixiang.yshop.module.infra.api.file.FileApi;
import co.yixiang.yshop.module.member.controller.app.user.vo.AppUserQueryVo;
import co.yixiang.yshop.module.member.controller.app.user.vo.AppUserUpdateMobileReqVO;
import co.yixiang.yshop.module.member.convert.user.UserConvert;
import co.yixiang.yshop.module.member.dal.dataobject.user.MemberUserDO;
import co.yixiang.yshop.module.member.dal.dataobject.userbill.UserBillDO;
import co.yixiang.yshop.module.member.dal.mysql.user.MemberUserMapper;
import co.yixiang.yshop.module.member.dal.mysql.userbill.UserBillMapper;
import co.yixiang.yshop.framework.common.constant.SystemConfigConstants;
import co.yixiang.yshop.module.infra.dal.dataobject.config.ConfigDO;
import co.yixiang.yshop.module.infra.service.config.ConfigService;
import co.yixiang.yshop.module.member.dal.dataobject.userbill.UserBrokerageWithdrawDO;
import co.yixiang.yshop.module.member.dal.mysql.userbill.UserBrokerageWithdrawMapper;
import co.yixiang.yshop.module.member.enums.BillDetailEnum;
import co.yixiang.yshop.module.member.enums.ErrorCodeConstants;
import co.yixiang.yshop.module.member.service.userbill.UserBillService;
import co.yixiang.yshop.module.system.api.sms.SmsCodeApi;
import co.yixiang.yshop.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import co.yixiang.yshop.module.system.enums.sms.SmsSceneEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.framework.common.util.servlet.ServletUtils.getClientIP;
import static co.yixiang.yshop.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static co.yixiang.yshop.module.member.enums.ErrorCodeConstants.USER_YUE_NOT;

/**
 * 会员 User Service 实现类
 *
 * @author yshop
 */
@Service
@Valid
@Slf4j
public class MemberUserServiceImpl extends ServiceImpl<MemberUserMapper,MemberUserDO> implements MemberUserService {

    @Resource
    private MemberUserMapper memberUserMapper;

    @Resource
    private FileApi fileApi;
    @Resource
    private SmsCodeApi smsCodeApi;

    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private AppCouponUserService appCouponUserService;
    @Resource
    private UserBillMapper userBillMapper;
    @Resource
    private UserBillService billService;
    @Resource
    private ConfigService configService;
    @Resource
    private UserBrokerageWithdrawMapper withdrawMapper;




    @Override
    public MemberUserDO getUserByMobile(String mobile) {
        return memberUserMapper.selectByMobile(mobile);
    }

    @Override
    public List<MemberUserDO> getUserListByNickname(String nickname) {
        return memberUserMapper.selectListByNicknameLike(nickname);
    }

    @Override
    public MemberUserDO createUserIfAbsent(String mobile, String registerIp,String from) {
        // 用户已经存在
        if(StrUtil.isNotEmpty(mobile)){
            MemberUserDO user = memberUserMapper.selectByMobile(mobile);
            if (user != null) {
                return user;
            }
        }

        // 用户不存在，则进行创建
        return this.createUser(mobile, registerIp,from);
    }

    private MemberUserDO createUser(String mobile, String registerIp,String from) {
        // 生成密码
        String password = IdUtil.fastSimpleUUID();
        // 插入用户
        MemberUserDO user = new MemberUserDO();
        user.setNickname(mobile);
        user.setUsername(mobile);
        user.setMobile(mobile);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(password)); // 加密密码
        user.setRegisterIp(registerIp);
        user.setLoginType(from);
        memberUserMapper.insert(user);
        return user;
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        memberUserMapper.updateById(new MemberUserDO().setId(id)
                .setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public MemberUserDO getUser(Long id) {
        return memberUserMapper.selectById(id);
    }

    @Override
    public AppUserQueryVo getAppUser(Long id){
        AppUserQueryVo appUserQueryVo = UserConvert.INSTANCE.convert3(memberUserMapper.selectById(id));
        Long count = appCouponUserService.count(new LambdaQueryWrapper<CouponUserDO>()
                .eq(CouponUserDO::getUserId,id)
                .eq(CouponUserDO::getStatus, ShopCommonEnum.IS_STATUS_1));
        appUserQueryVo.setCouponCount(count);
        QueryWrapper<UserBillDO> wrapper = new QueryWrapper<>();
        wrapper.select("SUM(number) as sumAll")
                .eq("category", BillDetailEnum.CATEGORY_1.getValue())
                .eq("type",BillDetailEnum.TYPE_3.getValue()).eq("uid",id);

        UserBillDO userBillDO =  userBillMapper.selectOne(wrapper);
        if(userBillDO == null){
            appUserQueryVo.setSumMoney(BigDecimal.ZERO);
        }else{
            appUserQueryVo.setSumMoney(userBillDO.getSumAll());
        }


        return appUserQueryVo;
    }
    /**
     * 减去用户余额
     * @param uid uid
     * @param payPrice 金额
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void decPrice(Long uid, BigDecimal payPrice) {
        // 并发安全：SQL 已加 now_money >= payPrice 行级守卫，仅余额充足时才会真正扣减；
        // 影响行数为 0 表示余额不足（可能被并发请求先扣），抛出余额不足而非产生负数余额
        int rows = memberUserMapper.decPrice(payPrice, uid);
        if (rows == 0) {
            throw exception(USER_YUE_NOT);
        }
    }

    /**
     * 减去用户积分
     * @param uid uid
     * @param score 积分
     */
    @Override
    public void decScore(Long uid, Integer score) {
        memberUserMapper.decScore(score,uid);
    }

    /**
     * 增加购买次数
     * @param uid uid
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void incPayCount(Long uid) {
        memberUserMapper.incPayCount(uid);
    }


    @Override
    public List<MemberUserDO> getUserList(Collection<Long> ids) {
        return memberUserMapper.selectBatchIds(ids);
    }

    @Override
    public void updateUserNickname(Long userId, String nickname,String birthday, Integer gender,
                                   String avatar,
                                   String mobile) {
        MemberUserDO user = this.checkUserExists(userId);
        // 仅当新昵称不等于旧昵称时进行修改
//        if (nickname.equals(user.getNickname())){
//            return;
//        }
        MemberUserDO userDO = new MemberUserDO();
        userDO.setId(user.getId());
        userDO.setNickname(nickname);
        userDO.setBirthday(birthday);
        userDO.setGender(gender);
        userDO.setAvatar(avatar);
        userDO.setMobile(mobile);
        memberUserMapper.updateById(userDO);
    }

    @Override
    public String updateUserAvatar(Long userId, InputStream avatarFile) throws Exception {
        this.checkUserExists(userId);
        // 创建文件
        String avatar = fileApi.createFile(IoUtil.readBytes(avatarFile));
        // 更新头像路径
        memberUserMapper.updateById(MemberUserDO.builder().id(userId).avatar(avatar).build());
        return avatar;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserMobile(Long userId, AppUserUpdateMobileReqVO reqVO) {
        // 检测用户是否存在
        checkUserExists(userId);
        // TODO yshop：oldMobile 应该不用传递

        // 校验旧手机和旧验证码
        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO().setMobile(reqVO.getOldMobile()).setCode(reqVO.getOldCode())
                .setScene(SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene()).setUsedIp(getClientIP()));
        // 使用新验证码
        smsCodeApi.useSmsCode(new SmsCodeUseReqDTO().setMobile(reqVO.getMobile()).setCode(reqVO.getCode())
                .setScene(SmsSceneEnum.MEMBER_UPDATE_MOBILE.getScene()).setUsedIp(getClientIP()));

        // 更新用户手机
        memberUserMapper.updateById(MemberUserDO.builder().id(userId).mobile(reqVO.getMobile()).build());
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 更新用户余额
     * @param uid y用户id
     * @param price 金额
     */
    @Override
    public void incMoney(Long uid, BigDecimal price) {
        if(price!=null&&price.doubleValue()>0){
            memberUserMapper.incMoney(uid,price);
        }
    }

    @Override
    public void incBrokerage(Long uid, BigDecimal price) {
        if (price != null && price.doubleValue() > 0) {
            memberUserMapper.incBrokerage(price, uid);
        }
    }

    @Override
    public void decBrokerage(Long uid, BigDecimal price) {
        if (price != null && price.doubleValue() > 0) {
            // SQL 已加 brokerage_price >= price 守卫，影响行数为 0 说明佣金不足
            int rows = memberUserMapper.decBrokerage(price, uid);
            if (rows == 0) {
                throw exception(ErrorCodeConstants.USER_BROKERAGE_NOT_ENOUGH);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindSpread(Long uid, Long spreadUid) {
        if (spreadUid == null || spreadUid.equals(uid)) {
            throw exception(ErrorCodeConstants.USER_SPREAD_SELF);
        }
        MemberUserDO user = getUser(uid);
        if (user.getSpreadUid() != null) {
            throw exception(ErrorCodeConstants.USER_SPREAD_BOUND);
        }
        MemberUserDO spread = getUser(spreadUid);
        if (spread == null) {
            throw exception(ErrorCodeConstants.USER_SPREAD_NOT_EXIST);
        }
        user.setSpreadUid(spreadUid);
        user.setSpreadTime(LocalDateTime.now());
        memberUserMapper.updateById(user);
        memberUserMapper.incSpreadCount(spreadUid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyBrokerageWithdraw(Long uid, BigDecimal price) {
        MemberUserDO user = getUser(uid);
        if (price == null || price.doubleValue() <= 0) {
            throw exception(ErrorCodeConstants.USER_BROKERAGE_NOT_ENOUGH);
        }
        // 最低提现门槛（可配置，默认 100）
        ConfigDO cfg = configService.getConfigByKey(SystemConfigConstants.STORE_BROKERAGE_WITHDRAW_MIN);
        String minStr = cfg == null ? null : cfg.getValue();
        BigDecimal min = StrUtil.isBlank(minStr) ? new BigDecimal("100") : new BigDecimal(minStr);
        if (price.compareTo(min) < 0) {
            throw exception(ErrorCodeConstants.USER_BROKERAGE_WITHDRAW_MIN);
        }
        if (user.getBrokeragePrice() == null || user.getBrokeragePrice().compareTo(price) < 0) {
            throw exception(ErrorCodeConstants.USER_BROKERAGE_NOT_ENOUGH);
        }
        UserBrokerageWithdrawDO withdraw = new UserBrokerageWithdrawDO();
        withdraw.setUid(uid);
        withdraw.setPrice(price);
        withdraw.setStatus(0);
        withdrawMapper.insert(withdraw);
    }

    @Override
    public List<UserBrokerageWithdrawDO> getBrokerageWithdrawList(Long uid) {
        return withdrawMapper.selectList(Wrappers.<UserBrokerageWithdrawDO>lambdaQuery()
                .eq(UserBrokerageWithdrawDO::getUid, uid)
                .orderByDesc(UserBrokerageWithdrawDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBrokerageWithdraw(Long id, String mark) {
        UserBrokerageWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null || withdraw.getStatus() != 0) {
            return;
        }
        // 先扣减佣金（带余额守卫），再产生提现账单
        decBrokerage(withdraw.getUid(), withdraw.getPrice());
        MemberUserDO user = getUser(withdraw.getUid());
        billService.expend(user.getId(), "佣金提现", BillDetailEnum.CATEGORY_1.getValue(),
                BillDetailEnum.TYPE_4.getValue(), withdraw.getPrice().doubleValue(),
                user.getBrokeragePrice().doubleValue(), "佣金提现" + withdraw.getPrice() + "元");
        withdraw.setStatus(1);
        withdraw.setMark(mark);
        withdrawMapper.updateById(withdraw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBrokerageWithdraw(Long id, String mark) {
        UserBrokerageWithdrawDO withdraw = withdrawMapper.selectById(id);
        if (withdraw == null || withdraw.getStatus() != 0) {
            return;
        }
        withdraw.setStatus(-1);
        withdraw.setMark(mark);
        withdrawMapper.updateById(withdraw);
    }




    /**
     * 对密码进行加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @VisibleForTesting
    public MemberUserDO checkUserExists(Long id) {
        if (id == null) {
            return null;
        }
        MemberUserDO user = memberUserMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

}

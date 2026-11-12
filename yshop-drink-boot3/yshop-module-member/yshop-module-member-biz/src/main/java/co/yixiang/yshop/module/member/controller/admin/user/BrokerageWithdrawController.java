package co.yixiang.yshop.module.member.controller.admin.user;

import co.yixiang.yshop.framework.common.pojo.CommonResult;
import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.module.member.dal.dataobject.userbill.UserBrokerageWithdrawDO;
import co.yixiang.yshop.module.member.dal.mysql.userbill.UserBrokerageWithdrawMapper;
import co.yixiang.yshop.module.member.service.user.MemberUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static co.yixiang.yshop.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 佣金提现审核
 *
 * @author yshop
 */
@Tag(name = "管理后台 - 佣金提现")
@RestController
@RequestMapping("/member/brokerage-withdraw")
@Validated
public class BrokerageWithdrawController {

    @Resource
    private MemberUserService userService;
    @Resource
    private UserBrokerageWithdrawMapper withdrawMapper;

    @GetMapping("/list")
    @Operation(summary = "佣金提现列表（按状态：0待审核 1通过 -1拒绝）")
    @PreAuthorize("@ss.hasPermission('member:brokerage-withdraw:query')")
    public CommonResult<List<UserBrokerageWithdrawDO>> list(@RequestParam(value = "status", required = false) Integer status) {
        return success(withdrawMapper.selectList(Wrappers.<UserBrokerageWithdrawDO>lambdaQuery()
                .eq(status != null, UserBrokerageWithdrawDO::getStatus, status)
                .orderByDesc(UserBrokerageWithdrawDO::getId)));
    }

    @PostMapping("/approve")
    @Operation(summary = "审核通过（扣减佣金并发放提现账单）")
    @PreAuthorize("@ss.hasPermission('member:brokerage-withdraw:approve')")
    public CommonResult<Boolean> approve(@RequestParam("id") Long id,
                                         @RequestParam(value = "mark", required = false) String mark) {
        userService.approveBrokerageWithdraw(id, mark);
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "审核拒绝")
    @PreAuthorize("@ss.hasPermission('member:brokerage-withdraw:approve')")
    public CommonResult<Boolean> reject(@RequestParam("id") Long id,
                                        @RequestParam(value = "mark", required = false) String mark) {
        userService.rejectBrokerageWithdraw(id, mark);
        return success(true);
    }
}

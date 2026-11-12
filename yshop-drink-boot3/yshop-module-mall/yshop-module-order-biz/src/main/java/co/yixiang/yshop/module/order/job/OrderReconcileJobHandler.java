package co.yixiang.yshop.module.order.job;

import cn.hutool.core.util.NumberUtil;
import co.yixiang.yshop.framework.common.enums.OrderInfoEnum;
import co.yixiang.yshop.framework.common.enums.PayIdEnum;
import co.yixiang.yshop.framework.quartz.core.handler.JobHandler;
import co.yixiang.yshop.framework.tenant.core.util.TenantUtils;
import co.yixiang.yshop.module.order.dal.dataobject.storeorder.StoreOrderDO;
import co.yixiang.yshop.module.order.dal.mysql.storeorder.StoreOrderMapper;
import co.yixiang.yshop.module.order.service.storeorder.AppStoreOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.egzosn.pay.spring.boot.core.PayServiceManager;
import com.egzosn.pay.spring.boot.core.bean.MerchantQueryOrder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支付对账/主动查单定时任务
 *
 * <p>解决「回调丢失/网络抖动导致订单未终态」的问题：定时扫描最近的未支付订单，
 * 主动向微信/支付宝网关查单，命中已支付则补单（走幂等 paySuccess），
 * 命中已关闭且超出支付宽限期则自动取消（释放库存与优惠券）。
 *
 * <p>注册方式：在任务调度（sys_job）中新增一条记录，
 * handler 名称填 {@code orderReconcileJobHandler}，cron 建议 {@code 0 0/5 * * * ?}（每 5 分钟），
 * 参数可填对账窗口分钟数（默认 30）。
 *
 * @author yshop
 */
@Component("orderReconcileJobHandler")
@Slf4j
public class OrderReconcileJobHandler implements JobHandler {

    @Resource
    private StoreOrderMapper storeOrderMapper;
    @Resource
    private PayServiceManager manager;
    @Resource
    private AppStoreOrderService appStoreOrderService;

    @Override
    public String execute(String param) {
        // 对账窗口（分钟）：扫描该窗口内创建的未支付订单
        int windowMinutes = NumberUtil.parseInt(param, 30);
        // 略微放宽回溯范围，避免任务间隔内漏扫
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes + 10);

        AtomicInteger scanned = new AtomicInteger();
        AtomicInteger reconciled = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        TenantUtils.executeIgnore(() -> {
            List<StoreOrderDO> orders = storeOrderMapper.selectList(
                    Wrappers.<StoreOrderDO>lambdaQuery()
                            .eq(StoreOrderDO::getPaid, OrderInfoEnum.PAY_STATUS_0.getValue())
                            .ge(StoreOrderDO::getCreateTime, since)
                            .orderByAsc(StoreOrderDO::getCreateTime));

            for (StoreOrderDO order : orders) {
                scanned.incrementAndGet();
                String payType = order.getPayType();
                // 仅核对网关支付（微信/支付宝）；余额/现金为同步或线下，无需查单
                if (!"weixin".equals(payType) && !"alipay".equals(payType)) {
                    continue;
                }
                try {
                    // 主动查单：只在拿到网关终态（已支付/已关闭）时才采信，避免错误配置干扰
                    Map<String, Object> m = queryGateway(order.getOrderId(), payType);
                    if (m == null) {
                        continue;
                    }
                    boolean paid = false;
                    BigDecimal amount = null;
                    if ("weixin".equals(payType)) {
                        String state = (String) m.get("trade_state");
                        if ("SUCCESS".equals(state)) {
                            paid = true;
                            Object fee = m.get("total_fee");
                            if (fee != null) {
                                amount = new BigDecimal(fee.toString())
                                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                            }
                        }
                    } else {
                        String status = (String) m.get("trade_status");
                        if ("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)) {
                            paid = true;
                            Object amt = m.get("total_amount");
                            if (amt != null) {
                                amount = new BigDecimal(amt.toString());
                            }
                        }
                    }

                    if (paid) {
                        // 金额可解析则走带金额校验的版本，否则退化为仅幂等补单
                        if (amount != null) {
                            appStoreOrderService.paySuccess(order.getOrderId(), payType, amount);
                        } else {
                            appStoreOrderService.paySuccess(order.getOrderId(), payType);
                        }
                        reconciled.incrementAndGet();
                        log.info("[对账] 订单[{}]网关已支付，补单成功", order.getOrderId());
                    } else if (order.getCreateTime().plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
                        // 网关明确关闭/失败，且已超出支付宽限期 -> 自动取消，释放库存与优惠券
                        appStoreOrderService.cancelOrder(order.getOrderId(), order.getUid());
                        closed.incrementAndGet();
                        log.info("[对账] 订单[{}]网关已关闭，自动取消", order.getOrderId());
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.error("[对账] 订单[{}]查单异常", order.getOrderId(), e);
                }
            }
        });

        return String.format("对账完成：扫描=%d, 补单=%d, 取消=%d, 异常=%d",
                scanned.get(), reconciled.get(), closed.get(), failed.get());
    }

    /**
     * 主动查单：按支付类型依次尝试该类型下的候选支付配置（detailsId）。
     * 同一订单只会用其中一种渠道配置支付；配置不匹配时网关返回「订单不存在」/NOTPAY，
     * 可安全跳过并尝试下一个候选。仅当命中网关终态（已支付/已关闭）时才返回该结果。
     */
    private Map<String, Object> queryGateway(String orderId, String payType) {
        List<String> candidates;
        if ("weixin".equals(payType)) {
            candidates = Arrays.asList(
                    PayIdEnum.WX_MINIAPP.getValue(), PayIdEnum.WX_WECHAT.getValue(),
                    PayIdEnum.WX_APP.getValue(), PayIdEnum.WX_H5.getValue(), PayIdEnum.WX_PC.getValue());
        } else {
            candidates = Arrays.asList(
                    PayIdEnum.ALI_H5.getValue(), PayIdEnum.ALI_APP.getValue(),
                    PayIdEnum.ALI_MINIAPP.getValue(), PayIdEnum.ALI_WECHAT.getValue(), PayIdEnum.ALI_PC.getValue());
        }
        for (String detailsId : candidates) {
            try {
                MerchantQueryOrder queryOrder = new MerchantQueryOrder();
                queryOrder.setDetailsId(detailsId);
                queryOrder.setOutTradeNo(orderId);
                Map<String, Object> m = manager.query(queryOrder);
                if (m != null && isTerminal(m, payType)) {
                    return m;
                }
            } catch (Exception e) {
                log.debug("[对账] 订单[{}]按配置[{}]查单失败，尝试下一个候选", orderId, detailsId, e);
            }
        }
        return null;
    }

    /**
     * 判断网关返回是否为「已支付 / 已关闭」终态（用于是否采信本次查单结果）
     */
    private boolean isTerminal(Map<String, Object> m, String payType) {
        if ("weixin".equals(payType)) {
            String state = (String) m.get("trade_state");
            return "SUCCESS".equals(state) || "CLOSED".equals(state) || "PAYERROR".equals(state);
        }
        String status = (String) m.get("trade_status");
        return "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status) || "TRADE_CLOSED".equals(status);
    }
}

package co.yixiang.yshop.module.pay.mq.producer;

import co.yixiang.yshop.framework.mq.redis.core.RedisMQTemplate;
import co.yixiang.yshop.module.pay.mq.message.PayNoticeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.math.BigDecimal;

@Slf4j
@Component
public class PayNoticeProducer {
    @Resource
    private RedisMQTemplate redisMQTemplate;

    /**
     * 发送消息
     * @param orderId 订单编号
     * @param payType 支付类型
     * @param payAmount 网关实际支付金额（单位：元），用于订单侧金额校验
     */
    public void sendPayNoticeMessage(String orderId, String payType, BigDecimal payAmount) {
        PayNoticeMessage payNoticeMessage = new PayNoticeMessage()
                .setOrderId(orderId).setPayType(payType).setPayAmount(payAmount);
        redisMQTemplate.send(payNoticeMessage);
    }
}

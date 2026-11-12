package co.yixiang.yshop.module.pay.config.handlers;

import co.yixiang.yshop.module.pay.mq.producer.PayNoticeProducer;
import com.egzosn.pay.ali.api.AliPayService;
import com.egzosn.pay.ali.bean.AliPayMessage;
import com.egzosn.pay.common.api.PayMessageHandler;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.exception.PayErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付回调处理器
 * @author hupeng
 * @date 2023/7/15
 */
@Slf4j
@Component
public class AliPayMessageHandler implements PayMessageHandler<AliPayMessage, AliPayService> {

    @Resource
    private PayNoticeProducer payNoticeProducer;
    /**
     * 处理支付回调消息的处理器接口
     *
     * @param payMessage 支付消息
     * @param context    上下文，如果handler或interceptor之间有信息要传递，可以用这个
     * @param payService 支付服务
     * @return xml, text格式的消息，如果在异步规则里处理的话，可以返回null
     * @throws PayErrorException 支付错误异常
     */
    @Override
    public PayOutMessage handle(AliPayMessage payMessage, Map<String, Object> context, AliPayService payService) throws PayErrorException {

        Map<String, Object> message = payMessage.getPayMessage();
        //交易状态
        String trade_status = (String) message.get("trade_status");

        //交易完成
        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

            String orderId = (String) message.get("out_trade_no");
            // 支付宝回调金额单位为元，随消息带入订单侧做金额校验
            BigDecimal payAmount = parseAmount(message.get("total_amount"));
            //消息队列处理
            payNoticeProducer.sendPayNoticeMessage(orderId, "alipay", payAmount);

            return payService.getPayOutMessage("success", "成功");

        }

        return payService.getPayOutMessage("fail", "失败");
    }

    /**
     * 支付宝回调节点金额单位为元
     */
    private BigDecimal parseAmount(Object totalAmount) {
        if (totalAmount == null) {
            return null;
        }
        try {
            return new BigDecimal(totalAmount.toString());
        } catch (NumberFormatException e) {
            log.error("[AliPayMessageHandler] 回调金额解析失败 totalAmount={}", totalAmount);
            return null;
        }
    }
}

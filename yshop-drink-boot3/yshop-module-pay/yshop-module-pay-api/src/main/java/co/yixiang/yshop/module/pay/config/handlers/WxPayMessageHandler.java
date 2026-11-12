package co.yixiang.yshop.module.pay.config.handlers;

import co.yixiang.yshop.module.pay.mq.producer.PayNoticeProducer;
import com.egzosn.pay.common.api.PayMessageHandler;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.wx.bean.WxPayMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 微信支付回调处理器
 * @author hupeng
 * @date 2023/7/15
 */
@Component
@Slf4j
public class WxPayMessageHandler implements PayMessageHandler<WxPayMessage, PayService> {

    @Resource
    private PayNoticeProducer payNoticeProducer;

    @Override
    public PayOutMessage handle(WxPayMessage payMessage, Map<String, Object> context, PayService payService) throws PayErrorException {

        log.info("======pay notice ========");
        Map<String, Object> msg = payMessage.getPayMessage();

        //交易状态
        if ("SUCCESS".equals(msg.get("result_code"))){
            String orderId = (String) msg.get("out_trade_no");
            // 微信回调金额单位为分，转换为元后随消息带入订单侧做金额校验
            BigDecimal payAmount = parseAmount(msg.get("total_fee"));
            //消息队列处理
            payNoticeProducer.sendPayNoticeMessage(orderId, "weixin", payAmount);
            return  payService.getPayOutMessage("SUCCESS", "OK");
        }

        return  payService.getPayOutMessage("FAIL", "失败");
    }

    /**
     * 微信回调节点金额单位为分，转换为元（保留 2 位）
     */
    private BigDecimal parseAmount(Object totalFee) {
        if (totalFee == null) {
            return null;
        }
        try {
            return new BigDecimal(totalFee.toString())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.error("[WxPayMessageHandler] 回调金额解析失败 totalFee={}", totalFee);
            return null;
        }
    }
}

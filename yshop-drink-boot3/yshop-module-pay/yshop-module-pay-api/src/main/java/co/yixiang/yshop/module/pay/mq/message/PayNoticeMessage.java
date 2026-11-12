package co.yixiang.yshop.module.pay.mq.message;

import co.yixiang.yshop.framework.mq.redis.core.stream.AbstractRedisStreamMessage;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PayNoticeMessage extends AbstractRedisStreamMessage {

    /**
     * 订单编号
     */
    @NotNull(message = "订单编号编号不能为空")
    private String orderId;

    //支付类型
    private String payType;

    /**
     * 网关实际支付金额（单位：元），用于回调金额校验，防止金额被篡改/错账
     */
    private BigDecimal payAmount;

    @Override
    public String getStreamKey() {
        return "order.pay.notice";
    }
}

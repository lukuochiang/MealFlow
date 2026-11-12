package co.yixiang.yshop.module.member.dal.dataobject.userbill;

import co.yixiang.yshop.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 佣金提现申请
 *
 * @author yshop
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("yshop_user_brokerage_withdraw")
public class UserBrokerageWithdrawDO extends BaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 用户编号 */
    private Long uid;
    /** 提现金额 */
    private BigDecimal price;
    /** 审核状态：0-待审核 1-通过 -1-拒绝 */
    private Integer status;
    /** 审核备注 */
    private String mark;

}

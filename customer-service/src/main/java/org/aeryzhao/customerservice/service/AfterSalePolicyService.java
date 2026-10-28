package org.aeryzhao.customerservice.service;

import org.aeryzhao.customerservice.model.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * 售后资格判定：把抓取来的「售后政策」落到可执行的规则上。
 *
 * <ul>
 *   <li>七天无理由退货：自签收次日起 7 天内，商品完好</li>
 *   <li>十五天换货：三包范围内的功能性故障</li>
 *   <li>特殊品类（生鲜、定制、虚拟商品、内衣、珠宝等）不适用七天无理由</li>
 * </ul>
 */
@Service
public class AfterSalePolicyService {

    public static final int NO_REASON_RETURN_DAYS = 7;
    public static final int EXCHANGE_DAYS = 15;

    private static final Set<String> NO_REASON_EXCLUDED = Set.of(
            "生鲜", "定制商品", "虚拟商品", "图书音像", "内衣", "珠宝", "奢侈品", "食品", "保健品", "美妆");

    private static final Set<String> NOT_DELIVERED = Set.of("待发货", "已发货", "已取消");

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 判定结果。
     *
     * @param allowed    是否支持七天无理由退货
     * @param code       机器可读结论：SUPPORTED / NOT_DELIVERED / EXPIRED / EXCLUDED_CATEGORY / CANCELLED
     * @param reason     给用户看的说明
     * @param deadline   可申请截止日期（yyyy-MM-dd），不适用时为 null
     * @param suggestion 下一步建议
     */
    public record Decision(boolean allowed, String code, String reason, String deadline, String suggestion) {
    }

    public Decision judge(Order order) {
        if (order == null) {
            return new Decision(false, "NOT_FOUND", "未查询到该订单", null, "请核对订单号后重试，或提供下单手机号由人工协助查询");
        }
        if ("已取消".equals(order.status())) {
            return new Decision(false, "CANCELLED", "该订单已取消，无需再申请退货", null, "如需重新购买可直接下单");
        }
        if (order.deliveredAt() == null || NOT_DELIVERED.contains(order.status())) {
            return new Decision(false, "NOT_DELIVERED", "订单当前状态为「" + order.status() + "」，尚未签收",
                    null, "未签收订单可直接在订单详情页申请取消，或签收时拒收");
        }

        LocalDate delivered = parseDate(order.deliveredAt());
        if (delivered == null) {
            return new Decision(false, "UNKNOWN_DATE", "签收时间缺失，无法自动判定售后时效", null, "建议转人工核实签收时间");
        }
        long days = ChronoUnit.DAYS.between(delivered, LocalDate.now(ZONE));
        LocalDate deadline = delivered.plusDays(NO_REASON_RETURN_DAYS);

        List<String> excluded = order.categories().stream().filter(NO_REASON_EXCLUDED::contains).toList();
        if (!excluded.isEmpty()) {
            return new Decision(false, "EXCLUDED_CATEGORY",
                    "订单中包含「" + String.join("、", excluded) + "」等特殊品类，按售后政策不适用七天无理由退货",
                    null,
                    "若商品存在质量问题，可在签收后 " + EXCHANGE_DAYS + " 天内申请换货/维修，需提供检测报告或实物照片");
        }
        if (days > EXCHANGE_DAYS) {
            return new Decision(false, "EXPIRED",
                    "已签收 " + days + " 天，超过七天无理由（" + NO_REASON_RETURN_DAYS + " 天）与换货（"
                            + EXCHANGE_DAYS + " 天）期限",
                    deadline.toString(),
                    "如仍在厂商保修期内，可走保修维修流程；也可转人工协商");
        }
        if (days > NO_REASON_RETURN_DAYS) {
            return new Decision(false, "EXPIRED",
                    "已签收 " + days + " 天，超过七天无理由退货期限（7 天），但仍在 15 天换货期内",
                    deadline.toString(),
                    "仅在出现功能性故障等质量问题时可申请换货/维修，需厂家检测确认");
        }
        return new Decision(true, "SUPPORTED",
                "已签收 " + days + " 天，在七天无理由退货期内",
                deadline.toString(),
                "商品需保持完好、配件齐全、不影响二次销售；退回运费按政策由您承担（钻石会员除外）");
    }

    private LocalDate parseDate(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZONE).toLocalDate();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}

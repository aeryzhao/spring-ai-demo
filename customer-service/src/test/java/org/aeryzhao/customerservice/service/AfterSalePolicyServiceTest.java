package org.aeryzhao.customerservice.service;

import org.aeryzhao.customerservice.model.Order;
import org.aeryzhao.customerservice.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AfterSalePolicyServiceTest {

    private final AfterSalePolicyService service = new AfterSalePolicyService();

    @Test
    @DisplayName("签收 2 天的普通商品支持七天无理由退货")
    void supportsReturnWithinSevenDays() {
        Order order = order("手机通讯", "已送达", OffsetDateTime.now().minusDays(2).toString(), null);

        AfterSalePolicyService.Decision decision = service.judge(order);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.code()).isEqualTo("SUPPORTED");
        assertThat(decision.deadline()).isNotBlank();
    }

    @Test
    @DisplayName("签收 10 天已超七天无理由期限")
    void rejectsAfterSevenDays() {
        Order order = order("家用电器", "已送达", OffsetDateTime.now().minusDays(10).toString(), null);

        AfterSalePolicyService.Decision decision = service.judge(order);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("EXPIRED");
        assertThat(decision.suggestion()).contains("换货");
    }

    @Test
    @DisplayName("生鲜等特殊品类不适用七天无理由")
    void rejectsExcludedCategory() {
        Order order = order("生鲜", "已送达", OffsetDateTime.now().minusDays(1).toString(), null);

        AfterSalePolicyService.Decision decision = service.judge(order);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("EXCLUDED_CATEGORY");
    }

    @Test
    @DisplayName("未签收订单提示取消/拒收，而不是退货")
    void rejectsUndeliveredOrder() {
        Order order = order("电脑办公", "已发货", null, null);

        AfterSalePolicyService.Decision decision = service.judge(order);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("NOT_DELIVERED");
        assertThat(decision.suggestion()).contains("取消");
    }

    @Test
    @DisplayName("已取消订单无需申请退货")
    void rejectsCancelledOrder() {
        Order order = order("电脑办公", "已取消", OffsetDateTime.now().minusDays(1).toString(), null);

        AfterSalePolicyService.Decision decision = service.judge(order);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("订单不存在时给出兜底结论")
    void handlesMissingOrder() {
        AfterSalePolicyService.Decision decision = service.judge(null);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("NOT_FOUND");
    }

    private Order order(String category, String status, String deliveredAt, String trackingNo) {
        return new Order(
                "JD202610120001",
                "测试用户",
                "13800001111",
                List.of(new OrderItem("100012043978", "测试商品", category, new BigDecimal("1999.00"), 1)),
                new BigDecimal("1999.00"),
                status,
                "在线支付-微信",
                "电子普通发票",
                OffsetDateTime.now().minusDays(20).toString(),
                deliveredAt,
                trackingNo);
    }
}

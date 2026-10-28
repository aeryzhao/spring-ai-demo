package org.aeryzhao.customerservice.service;

import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.aeryzhao.customerservice.model.Order;
import org.aeryzhao.customerservice.model.Shipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用仓库里的模拟数据（data/orders.json、data/logistics.json）验证查询逻辑，不依赖 Spring 上下文。
 */
class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        CustomerServiceProperties properties = new CustomerServiceProperties("./data", 4, 600, 100, 20, null);
        orderService = new OrderService(properties, JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
        orderService.load();
    }

    @Test
    @DisplayName("加载模拟订单与物流数据")
    void loadsMockData() {
        assertThat(orderService.all()).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("按订单号查询，且大小写不敏感")
    void findsOrderByNo() {
        Optional<Order> order = orderService.find("jd202610120001");

        assertThat(order).isPresent();
        assertThat(order.get().customerName()).isEqualTo("张伟");
        assertThat(order.get().items()).hasSize(1);
    }

    @Test
    @DisplayName("按手机号后四位查询订单")
    void findsOrdersByPhone() {
        List<Order> orders = orderService.findByPhone("1111");

        assertThat(orders).isNotEmpty();
        assertThat(orders).allMatch(order -> order.phone().endsWith("1111"));
    }

    @Test
    @DisplayName("订单能关联到物流轨迹")
    void findsShipmentOfOrder() {
        Order order = orderService.find("JD202610120001").orElseThrow();

        Optional<Shipment> shipment = orderService.shipmentOf(order);

        assertThat(shipment).isPresent();
        assertThat(shipment.get().events()).isNotEmpty();
        assertThat(shipment.get().company()).isEqualTo("京东物流");
    }

    @Test
    @DisplayName("不存在的订单返回空")
    void returnsEmptyForUnknownOrder() {
        assertThat(orderService.find("JD-NOT-EXIST")).isEmpty();
        assertThat(orderService.find(null)).isEmpty();
    }
}

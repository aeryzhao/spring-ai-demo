package org.aeryzhao.customerservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aeryzhao.customerservice.model.Order;
import org.aeryzhao.customerservice.model.Shipment;
import org.aeryzhao.customerservice.service.AfterSalePolicyService;
import org.aeryzhao.customerservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 订单 / 物流查询（模拟数据），方便配合对话验证工具调用结果。
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单与物流", description = "演示用的订单、物流查询与售后资格判定")
public class OrderController {

    private final OrderService orderService;
    private final AfterSalePolicyService afterSalePolicyService;

    public OrderController(OrderService orderService, AfterSalePolicyService afterSalePolicyService) {
        this.orderService = orderService;
        this.afterSalePolicyService = afterSalePolicyService;
    }

    @Operation(summary = "订单列表", description = "返回全部模拟订单；可用 phone 按手机号过滤")
    @GetMapping
    public List<Order> list(@RequestParam(value = "phone", required = false) String phone) {
        return (phone == null || phone.isBlank()) ? orderService.all() : orderService.findByPhone(phone);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public Order detail(@PathVariable String orderNo) {
        return orderService.find(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在：" + orderNo));
    }

    @Operation(summary = "订单物流轨迹")
    @GetMapping("/{orderNo}/logistics")
    public Shipment logistics(@PathVariable String orderNo) {
        Order order = orderService.find(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在：" + orderNo));
        return orderService.shipmentOf(order)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该订单暂无物流信息"));
    }

    @Operation(summary = "售后资格判定", description = "按抓取到的售后政策，判定该订单当前是否支持七天无理由退货")
    @GetMapping("/{orderNo}/after-sale-eligibility")
    public ResponseEntity<Map<String, Object>> eligibility(@PathVariable String orderNo) {
        Order order = orderService.find(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在：" + orderNo));
        AfterSalePolicyService.Decision decision = afterSalePolicyService.judge(order);
        return ResponseEntity.ok(Map.of(
                "orderNo", order.orderNo(),
                "status", order.status(),
                "deliveredAt", String.valueOf(order.deliveredAt()),
                "decision", decision));
    }
}

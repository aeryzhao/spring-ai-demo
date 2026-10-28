package org.aeryzhao.customerservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.aeryzhao.customerservice.model.Order;
import org.aeryzhao.customerservice.model.Shipment;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单与物流查询（演示用模拟数据，读取 data/orders.json 与 data/logistics.json）。
 *
 * <p>真实项目里这里会换成订单中心 / 物流网关的调用，工具层无需改动。</p>
 */
@Slf4j
@Service
public class OrderService {

    private final CustomerServiceProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, Order> ordersByNo = new LinkedHashMap<>();
    private final Map<String, Shipment> shipmentsByTrackingNo = new LinkedHashMap<>();

    public OrderService(CustomerServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        Path ordersFile = properties.dataPath().resolve("orders.json");
        Path logisticsFile = properties.dataPath().resolve("logistics.json");
        try {
            if (Files.exists(ordersFile)) {
                OrdersFile file = objectMapper.readValue(ordersFile.toFile(), OrdersFile.class);
                file.orders().forEach(order -> ordersByNo.put(order.orderNo(), order));
            } else {
                log.warn("订单模拟数据不存在：{}", ordersFile);
            }
            if (Files.exists(logisticsFile)) {
                ShipmentsFile file = objectMapper.readValue(logisticsFile.toFile(), ShipmentsFile.class);
                file.shipments().forEach(shipment -> shipmentsByTrackingNo.put(shipment.trackingNo(), shipment));
            } else {
                log.warn("物流模拟数据不存在：{}", logisticsFile);
            }
            log.info("已加载 {} 个订单、{} 条运单", ordersByNo.size(), shipmentsByTrackingNo.size());
        } catch (JacksonException e) {
            log.error("加载订单/物流数据失败", e);
        }
    }

    public Optional<Order> find(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ordersByNo.get(orderNo.trim().toUpperCase()));
    }

    public List<Order> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }
        String target = phone.trim();
        List<Order> matched = new ArrayList<>();
        for (Order order : ordersByNo.values()) {
            if (order.phone() != null && order.phone().endsWith(target.length() >= 4 ? target.substring(target.length() - 4) : target)) {
                matched.add(order);
            }
        }
        return matched;
    }

    public List<Order> all() {
        return List.copyOf(ordersByNo.values());
    }

    public Optional<Shipment> findShipment(String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(shipmentsByTrackingNo.get(trackingNo.trim()));
    }

    public Optional<Shipment> shipmentOf(Order order) {
        return order == null ? Optional.empty() : findShipment(order.trackingNo());
    }

    private record OrdersFile(List<Order> orders) {
        private OrdersFile {
            orders = orders == null ? List.of() : orders;
        }
    }

    private record ShipmentsFile(List<Shipment> shipments) {
        private ShipmentsFile {
            shipments = shipments == null ? List.of() : shipments;
        }
    }
}

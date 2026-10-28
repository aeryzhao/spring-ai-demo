package org.aeryzhao.customerservice.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单（演示数据，见 data/orders.json）。
 *
 * @param orderNo     订单号
 * @param customerName 收件人
 * @param phone       下单手机号，客服可用它反查订单
 * @param items       商品明细
 * @param totalAmount 订单金额
 * @param status      订单状态：待发货 / 已发货 / 已送达 / 已完成 / 已取消
 * @param payMethod   支付方式
 * @param invoiceType 发票类型
 * @param createdAt   下单时间（ISO-8601）
 * @param deliveredAt 签收时间（ISO-8601，未签收为 null）
 * @param trackingNo  运单号
 */
public record Order(
        String orderNo,
        String customerName,
        String phone,
        List<OrderItem> items,
        BigDecimal totalAmount,
        String status,
        String payMethod,
        String invoiceType,
        String createdAt,
        String deliveredAt,
        String trackingNo) {

    /** 订单里的商品品类（可能出现多个）。 */
    public List<String> categories() {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(OrderItem::category).distinct().toList();
    }
}

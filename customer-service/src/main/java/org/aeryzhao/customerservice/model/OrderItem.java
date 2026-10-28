package org.aeryzhao.customerservice.model;

import java.math.BigDecimal;

/**
 * 订单商品行。
 *
 * @param sku      商品编码
 * @param name     商品名称
 * @param category 商品品类，用于判断是否属于「不适用七天无理由」的特殊品类
 * @param price    单价
 * @param qty      数量
 */
public record OrderItem(String sku, String name, String category, BigDecimal price, int qty) {
}

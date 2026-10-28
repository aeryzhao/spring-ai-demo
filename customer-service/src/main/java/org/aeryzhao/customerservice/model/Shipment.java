package org.aeryzhao.customerservice.model;

import java.util.List;

/**
 * 物流运单（演示数据，见 data/logistics.json）。
 *
 * @param trackingNo 运单号
 * @param company    承运商
 * @param status     运单状态
 * @param events     轨迹节点，按时间正序
 */
public record Shipment(String trackingNo, String company, String status, List<TrackingEvent> events) {
}

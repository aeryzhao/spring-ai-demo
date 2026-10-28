package org.aeryzhao.customerservice.model;

/**
 * 物流节点。
 *
 * @param time        发生时间（ISO-8601）
 * @param location    所在网点
 * @param description 事件描述
 */
public record TrackingEvent(String time, String location, String description) {
}

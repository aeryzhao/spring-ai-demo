package org.aeryzhao.customerservice.model;

/**
 * 客服工单（退款申请 / 投诉 / 转人工），运行期产生，落盘到 data/tickets.json。
 *
 * @param ticketNo   工单号
 * @param type       工单类型：退款退货 / 换货 / 投诉 / 转人工
 * @param orderNo    关联订单号，可为 null
 * @param summary    诉求摘要
 * @param phone      联系电话
 * @param status     状态：已受理 / 处理中 / 已完结
 * @param createdAt  创建时间（ISO-8601）
 * @param sessionId  来源会话 id
 * @param note       处理备注
 */
public record Ticket(
        String ticketNo,
        String type,
        String orderNo,
        String summary,
        String phone,
        String status,
        String createdAt,
        String sessionId,
        String note) {
}

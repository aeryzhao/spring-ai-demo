package org.aeryzhao.customerservice.tool;

import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.model.Order;
import org.aeryzhao.customerservice.model.Shipment;
import org.aeryzhao.customerservice.model.Ticket;
import org.aeryzhao.customerservice.model.TrackingEvent;
import org.aeryzhao.customerservice.service.AfterSalePolicyService;
import org.aeryzhao.customerservice.service.OrderService;
import org.aeryzhao.customerservice.service.TicketService;
import org.aeryzhao.customerservice.service.ToolCallRecorder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客服工具集：模型在需要「实时业务数据」时自主调用。
 *
 * <p>工具只负责查/改业务数据，政策解释仍然来自抓取到的知识库，两者配合避免模型编造订单信息。</p>
 */
@Slf4j
@Component
public class CustomerServiceTools {

    private final OrderService orderService;
    private final TicketService ticketService;
    private final AfterSalePolicyService afterSalePolicyService;
    private final ToolCallRecorder recorder;

    public CustomerServiceTools(OrderService orderService,
                                TicketService ticketService,
                                AfterSalePolicyService afterSalePolicyService,
                                ToolCallRecorder recorder) {
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.afterSalePolicyService = afterSalePolicyService;
        this.recorder = recorder;
    }

    @Tool(description = "根据订单号查询订单详情，返回商品明细、金额、订单状态、支付方式、发票类型、签收时间与运单号。"
            + "用户提到自己的订单时优先调用本工具，不要凭空猜测订单信息。")
    public OrderResult queryOrder(
            @ToolParam(description = "订单号，形如 JD202610120001") String orderNo,
            ToolContext toolContext) {
        record(toolContext, "查询订单 " + orderNo);
        return orderService.find(orderNo)
                .map(order -> new OrderResult(true, "查询成功", order))
                .orElseGet(() -> new OrderResult(false,
                        "未找到订单 " + orderNo + "，请让用户核对订单号，或改用手机号查询", null));
    }

    @Tool(description = "根据下单手机号查询该手机号名下的所有订单，返回订单号、状态、金额与商品摘要。用户记不清订单号时使用。")
    public OrderListResult queryOrdersByPhone(
            @ToolParam(description = "下单时使用的 11 位手机号，也可以只给后四位") String phone,
            ToolContext toolContext) {
        record(toolContext, "按手机号查询订单 " + phone);
        List<Order> orders = orderService.findByPhone(phone);
        return new OrderListResult(orders.size(), orders,
                orders.isEmpty() ? "该手机号下没有查询到订单" : "共查询到 " + orders.size() + " 个订单");
    }

    @Tool(description = "根据订单号查询物流轨迹，返回承运商、当前状态与各节点时间地点。用户问「到哪了」「什么时候到」时使用。")
    public LogisticsResult queryLogistics(
            @ToolParam(description = "订单号，形如 JD202610120001") String orderNo,
            ToolContext toolContext) {
        record(toolContext, "查询物流 " + orderNo);
        Order order = orderService.find(orderNo).orElse(null);
        if (order == null) {
            return new LogisticsResult(false, null, null, List.of(), "未找到订单 " + orderNo);
        }
        Shipment shipment = orderService.shipmentOf(order).orElse(null);
        if (shipment == null) {
            return new LogisticsResult(false, null, null, List.of(),
                    "订单 " + orderNo + " 暂无物流信息（可能尚未发货）");
        }
        return new LogisticsResult(true, shipment.company(), shipment.status(), shipment.events(), "查询成功");
    }

    @Tool(description = "判断某个订单是否支持七天无理由退货/十五天换货，返回结论、截止日期与原因。"
            + "用户询问「能不能退」「还能退吗」「过期了吗」时必须先调用本工具。")
    public AfterSaleResult checkAfterSaleEligibility(
            @ToolParam(description = "订单号，形如 JD202610120001") String orderNo,
            ToolContext toolContext) {
        record(toolContext, "判定售后资格 " + orderNo);
        Order order = orderService.find(orderNo).orElse(null);
        AfterSalePolicyService.Decision decision = afterSalePolicyService.judge(order);
        return new AfterSaleResult(decision.allowed(), decision.code(), decision.reason(),
                decision.deadline(), decision.suggestion());
    }

    @Tool(description = "为订单提交退货退款申请，生成售后工单号。调用前应先确认订单存在且售后资格允许；"
            + "若资格不允许，请如实告知用户原因，不要强行提交。")
    public AfterSaleSubmitResult submitReturnRequest(
            @ToolParam(description = "订单号，形如 JD202610120001") String orderNo,
            @ToolParam(description = "用户给出的退货原因，例如「尺寸不合适」「屏幕有坏点」") String reason,
            ToolContext toolContext) {
        record(toolContext, "提交退货申请 " + orderNo);
        Order order = orderService.find(orderNo).orElse(null);
        if (order == null) {
            return new AfterSaleSubmitResult(false, null, "未找到订单 " + orderNo + "，无法提交申请", null);
        }
        AfterSalePolicyService.Decision decision = afterSalePolicyService.judge(order);
        if (!decision.allowed()) {
            return new AfterSaleSubmitResult(false, null,
                    "该订单暂不支持无理由退货：" + decision.reason(), decision.suggestion());
        }
        Ticket ticket = ticketService.create("退款退货", orderNo,
                "订单 " + orderNo + " 退货申请，原因：" + reason, order.phone(), sessionId(toolContext),
                "系统判定符合七天无理由，截止 " + decision.deadline());
        return new AfterSaleSubmitResult(true, ticket.ticketNo(),
                "退货申请已受理，工单号 " + ticket.ticketNo(),
                "请在 24 小时内将商品按原包装寄回，审核通过后 1-3 个工作日退款到原支付账户");
    }

    @Tool(description = "转人工：创建客服工单并记录用户诉求与联系方式。遇到知识库无法回答、涉及投诉、"
            + "或用户明确要求人工时调用。")
    public TicketResult createHumanTicket(
            @ToolParam(description = "用户诉求的一句话摘要") String summary,
            @ToolParam(description = "用户联系电话，用户未提供时传空字符串") String phone,
            ToolContext toolContext) {
        record(toolContext, "转人工工单：" + summary);
        Ticket ticket = ticketService.create("转人工", null, summary, phone, sessionId(toolContext),
                "由智能客服创建，等待人工跟进");
        return new TicketResult(ticket.ticketNo(), ticket.type(), ticket.status(),
                "已创建人工工单 " + ticket.ticketNo() + "，客服将在 2 小时内联系您");
    }

    @Tool(description = "根据工单号查询售后/人工工单的处理进度。")
    public TicketResult queryTicket(
            @ToolParam(description = "工单号，形如 TK202610250001") String ticketNo,
            ToolContext toolContext) {
        record(toolContext, "查询工单 " + ticketNo);
        return ticketService.find(ticketNo)
                .map(ticket -> new TicketResult(ticket.ticketNo(), ticket.type(), ticket.status(),
                        "工单状态：" + ticket.status() + "，受理时间 " + ticket.createdAt()))
                .orElseGet(() -> new TicketResult(ticketNo, null, null, "未找到工单 " + ticketNo));
    }

    private void record(ToolContext toolContext, String note) {
        recorder.record(sessionId(toolContext), note);
        log.debug("工具调用：{}", note);
    }

    private String sessionId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get("sessionId");
        return value == null ? null : String.valueOf(value);
    }

    public record OrderResult(boolean found, String message, Order order) {
    }

    public record OrderListResult(int count, List<Order> orders, String message) {
    }

    public record LogisticsResult(boolean found, String company, String status,
                                 List<TrackingEvent> events, String message) {
    }

    public record AfterSaleResult(boolean allowed, String code, String reason, String deadline, String suggestion) {
    }

    public record AfterSaleSubmitResult(boolean submitted, String ticketNo, String message, String nextStep) {
    }

    public record TicketResult(String ticketNo, String type, String status, String message) {
    }
}

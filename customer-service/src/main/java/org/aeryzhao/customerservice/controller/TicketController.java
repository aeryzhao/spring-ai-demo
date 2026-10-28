package org.aeryzhao.customerservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aeryzhao.customerservice.model.Ticket;
import org.aeryzhao.customerservice.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 工单查询：模型通过工具创建的售后/转人工工单都会出现在这里。
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "工单", description = "售后申请与转人工工单")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "工单列表", description = "可按 sessionId 过滤某个会话产生的工单")
    @GetMapping
    public List<Ticket> list(@RequestParam(value = "sessionId", required = false) String sessionId) {
        return ticketService.list(sessionId);
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{ticketNo}")
    public Ticket detail(@PathVariable String ticketNo) {
        return ticketService.find(ticketNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在：" + ticketNo));
    }
}

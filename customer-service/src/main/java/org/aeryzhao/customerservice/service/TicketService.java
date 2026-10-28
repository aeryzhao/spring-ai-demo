package org.aeryzhao.customerservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.aeryzhao.customerservice.model.Ticket;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客服工单：模型通过工具调用创建售后申请 / 转人工工单，数据落盘到 data/tickets.json。
 */
@Slf4j
@Service
public class TicketService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger SEQ = new AtomicInteger();

    private final CustomerServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public TicketService(CustomerServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        Path file = dataFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
            TicketsFile wrapper = objectMapper.readValue(file.toFile(), TicketsFile.class);
            wrapper.tickets().forEach(ticket -> tickets.put(ticket.ticketNo(), ticket));
            log.info("已加载 {} 条工单", tickets.size());
        } catch (JacksonException e) {
            log.warn("加载工单失败：{}", e.getMessage());
        }
    }

    public Ticket create(String type, String orderNo, String summary, String phone, String sessionId, String note) {
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String ticketNo = "TK" + LocalDate.now().format(DATE) + String.format("%04d", SEQ.incrementAndGet());
        Ticket ticket = new Ticket(
                ticketNo,
                type == null ? "转人工" : type,
                orderNo,
                summary,
                phone,
                "已受理",
                now,
                sessionId,
                note);
        tickets.put(ticketNo, ticket);
        persist();
        log.info("创建工单 {} type={} order={}", ticketNo, type, orderNo);
        return ticket;
    }

    public Optional<Ticket> find(String ticketNo) {
        return ticketNo == null ? Optional.empty() : Optional.ofNullable(tickets.get(ticketNo.trim().toUpperCase()));
    }

    public List<Ticket> list(String sessionId) {
        List<Ticket> all = new ArrayList<>(tickets.values());
        if (sessionId != null && !sessionId.isBlank()) {
            all.removeIf(ticket -> !sessionId.equals(ticket.sessionId()));
        }
        all.sort(Comparator.comparing(Ticket::createdAt).reversed());
        return all;
    }

    private Path dataFile() {
        return properties.dataPath().resolve("tickets.json");
    }

    private synchronized void persist() {
        try {
            Path file = dataFile();
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), new TicketsFile(new ArrayList<>(tickets.values())));
        } catch (IOException | JacksonException e) {
            log.warn("工单落盘失败：{}", e.getMessage());
        }
    }

    private record TicketsFile(List<Ticket> tickets) {
        private TicketsFile {
            tickets = tickets == null ? List.of() : tickets;
        }
    }
}

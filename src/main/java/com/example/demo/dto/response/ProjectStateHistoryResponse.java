package com.example.demo.dto.response;
import java.time.Instant;
import java.util.UUID;
public record ProjectStateHistoryResponse(UUID id, String changedBy, String previousApproval, String newApproval, String previousStatus, String newStatus, Instant changedAt) { }

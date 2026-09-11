package com.aml.system.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ActivityDto {
    private UUID logId;
    private String userId;
    private String actionType;
    private String affectedRecordId;
    private String ipAddress;
    private String details;
    private Instant createdAt;
}

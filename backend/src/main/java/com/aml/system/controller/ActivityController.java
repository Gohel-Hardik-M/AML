package com.aml.system.controller;

import com.aml.system.dto.admin.ActivityDto;
import com.aml.system.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bank-admin/activity")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    public Page<ActivityDto> list(Pageable pageable) {
        return activityService.list(pageable);
    }
}
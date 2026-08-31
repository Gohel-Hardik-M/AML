package com.aml.system.mapper;

import com.aml.system.dto.alerts.AlertResponseDto;
import com.aml.system.model.Alert;

public interface AlertMapper {

    AlertResponseDto toDto(Alert entity);

    Alert toEntity(AlertResponseDto dto);
}

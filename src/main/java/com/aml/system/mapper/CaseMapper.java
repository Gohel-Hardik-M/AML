package com.aml.system.mapper;

import com.aml.system.dto.cases.CaseResponseDto;
import com.aml.system.model.CaseEntity;

public interface CaseMapper {

    CaseResponseDto toDto(CaseEntity entity);

    CaseEntity toEntity(CaseResponseDto dto);
}

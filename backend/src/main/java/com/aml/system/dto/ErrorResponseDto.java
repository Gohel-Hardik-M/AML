package com.aml.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

    private int statusCode;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp = Instant.now();

    public static ErrorResponseDto of(int statusCode, String error, String message, String path) {
        return ErrorResponseDto.builder()
                .statusCode(statusCode)
                .error(error)
                .message(message != null ? message : error)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponseDto of(int statusCode, String error, String path) {
        return of(statusCode, error, error, path);
    }
}

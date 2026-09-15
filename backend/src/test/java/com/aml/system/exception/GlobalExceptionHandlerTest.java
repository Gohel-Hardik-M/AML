package com.aml.system.exception;

import com.aml.system.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnErrorResponseDtoForAmlBusinessException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/master/tenants");

        ResponseEntity<ErrorResponseDto> response = handler.handleAmlBusinessException(
                new AmlBusinessException("Tenant 'BANKA' already exists.", HttpStatus.CONFLICT),
                request
        );

        ErrorResponseDto body = response.getBody();

        assertNotNull(body);
        assertEquals(409, body.getStatusCode());
        assertEquals("Conflict", body.getError());
        assertEquals("Tenant 'BANKA' already exists.", body.getMessage());
        assertEquals("/api/v1/master/tenants", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void shouldExplainInvalidDecimalRequestField() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/bank-admin/rules/SMURFING_001");
        InvalidFormatException cause = InvalidFormatException.from(
                null, "Cannot deserialize value", "asdwhif", BigDecimal.class);

        ResponseEntity<ErrorResponseDto> response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("Invalid request body", cause), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatusCode());
        assertEquals("Field 'request body' must be a valid decimal number.", response.getBody().getMessage());
        assertEquals("/api/v1/bank-admin/rules/SMURFING_001", response.getBody().getPath());
    }

    @Test
    void shouldHandleCustomExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts/123");

        ResponseEntity<ErrorResponseDto> notFoundResponse = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Alert not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertEquals(404, notFoundResponse.getBody().getStatusCode());
        assertEquals("Alert not found", notFoundResponse.getBody().getMessage());

        ResponseEntity<ErrorResponseDto> badReqResponse = handler.handleBadRequestException(
                new BadRequestException("Invalid parameter"), request);
        assertEquals(HttpStatus.BAD_REQUEST, badReqResponse.getStatusCode());
        assertEquals(400, badReqResponse.getBody().getStatusCode());

        ResponseEntity<ErrorResponseDto> unauthResponse = handler.handleUnauthorizedException(
                new UnauthorizedException("Invalid token"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthResponse.getStatusCode());
        assertEquals(401, unauthResponse.getBody().getStatusCode());

        ResponseEntity<ErrorResponseDto> forbiddenResponse = handler.handleForbiddenException(
                new ForbiddenException("Account is locked"), request);
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());
        assertEquals(403, forbiddenResponse.getBody().getStatusCode());

        ResponseEntity<ErrorResponseDto> conflictResponse = handler.handleConflictException(
                new ConflictException("Batch already exists"), request);
        assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
        assertEquals(409, conflictResponse.getBody().getStatusCode());
    }
}

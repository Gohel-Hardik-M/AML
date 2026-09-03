package com.aml.system.exception;

import com.aml.system.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnSimpleErrorPayloadWithoutErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/master/tenants");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAmlBusinessException(
                new AmlBusinessException("Tenant 'BANKA' already exists.", HttpStatus.CONFLICT),
                request
        );

        ApiResponse<Void> body = response.getBody();

        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Tenant 'BANKA' already exists.", body.getMessage());
        assertEquals("/api/v1/master/tenants", body.getPath());
        assertNotNull(body.getTimestamp());
    }
}

package com.aml.system.dto.admin;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantOnboardRequestDtoValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidEmailWithSubdomainAndMultiCharacterTld() {
        Set<jakarta.validation.ConstraintViolation<TenantOnboardRequestDto>> violations =
                validator.validate(requestWithEmail("admin@ops.icci.co.in"));

        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsEmailWithoutTopLevelDomain() {
        Set<jakarta.validation.ConstraintViolation<TenantOnboardRequestDto>> violations =
                validator.validate(requestWithEmail("admin@icci"));

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsEmailWithInvalidDomainLabel() {
        Set<jakarta.validation.ConstraintViolation<TenantOnboardRequestDto>> violations =
                validator.validate(requestWithEmail("admin@-icci.com"));

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsEmailWithConsecutiveDots() {
        Set<jakarta.validation.ConstraintViolation<TenantOnboardRequestDto>> violations =
                validator.validate(requestWithEmail("admin..support@icci.com"));

        assertFalse(violations.isEmpty());
    }

    @Test
    void rejectsEmailLongerThan254Characters() {
        String localPart = "a".repeat(243);
        Set<jakarta.validation.ConstraintViolation<TenantOnboardRequestDto>> violations =
                validator.validate(requestWithEmail(localPart + "@icci.com"));

        assertFalse(violations.isEmpty());
    }

    private TenantOnboardRequestDto requestWithEmail(String email) {
        TenantOnboardRequestDto request = new TenantOnboardRequestDto();
        request.setTenantCode("ICCI");
        request.setBankName("ICCI Bank");
        request.setAdminUsername("admin");
        request.setAdminEmail(email);
        return request;
    }
}

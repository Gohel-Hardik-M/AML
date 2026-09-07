package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Batch {

        private UUID id;


        private UUID uploadedById;


        private String fileName;

        private BatchStatus status;

        private LocalDateTime uploadedAt;






    }

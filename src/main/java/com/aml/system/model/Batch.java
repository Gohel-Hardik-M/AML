package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;



@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batches")
public class Batch {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;


        @Column(name = "uploaded_by_id")
        private UUID uploadedById;

        @Column
        private String fileName;
        @Column
        private BatchStatus status;
        @Column
        private LocalDateTime uploadedAt;






    }

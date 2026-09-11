package com.aml.system.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AlertReviewRequestDto {

    @NotBlank(message = "Review note is required.")
    @Size(max = 4000, message = "Review note must be 4000 characters or fewer.")
    private String reviewNotes;
}

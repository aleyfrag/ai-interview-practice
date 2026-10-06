package com.aleyfrag.interview_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(
        @NotNull
        @Min(1)
        Integer questionNumber,

        @NotBlank
        @Size(max = 10000)
        String answer
        ) {
}

package com.aleyfrag.interview_service.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StartInterviewRequest (
    @NotBlank(message = "Job Role Is Required")
    String jobRole,

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 20, message = "Experience cannot exceed 60 years")
    int experienceYears
){

}

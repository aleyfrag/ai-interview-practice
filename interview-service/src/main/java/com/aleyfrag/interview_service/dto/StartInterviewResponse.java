package com.aleyfrag.interview_service.dto;

import java.util.UUID;

public record StartInterviewResponse(

        UUID interviewId,
        String question) {
}

package com.aleyfrag.interview_service.model;

public record InterviewTurn(
        int questionNumber,
        String question,
        String answer,
        Integer score,
        String feedback
) {
}

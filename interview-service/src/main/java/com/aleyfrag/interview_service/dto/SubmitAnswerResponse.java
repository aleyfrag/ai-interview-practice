package com.aleyfrag.interview_service.dto;

public record SubmitAnswerResponse(int answeredQuestionNumber,
                                   int score,
                                   String feedback,
                                   Integer nextQuestionNumber,
                                   String nextQuestion,
                                   boolean completed) {
}

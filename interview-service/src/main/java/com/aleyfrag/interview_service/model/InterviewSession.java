package com.aleyfrag.interview_service.model;

import java.util.List;
import java.util.UUID;

public record InterviewSession(UUID interviewId,
                               String jobRole,
                               int experienceYears,
                               List<InterviewQuestion> questions) {
}

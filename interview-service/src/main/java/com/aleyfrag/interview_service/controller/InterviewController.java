package com.aleyfrag.interview_service.controller;

import com.aleyfrag.interview_service.dto.StartInterviewRequest;
import com.aleyfrag.interview_service.dto.StartInterviewResponse;
import com.aleyfrag.interview_service.service.GeminiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final GeminiService geminiService;

    public InterviewController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }


    @PostMapping
    public StartInterviewResponse startInterview(
            @Valid @RequestBody StartInterviewRequest request) {


        String question = geminiService.generateFirstQuestion(
                request.jobRole(),
                request.experienceYears()
        );

        return new StartInterviewResponse(question);
    }
}

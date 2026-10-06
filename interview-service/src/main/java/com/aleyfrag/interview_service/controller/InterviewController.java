package com.aleyfrag.interview_service.controller;

import com.aleyfrag.interview_service.dto.StartInterviewRequest;
import com.aleyfrag.interview_service.dto.StartInterviewResponse;
import com.aleyfrag.interview_service.dto.SubmitAnswerRequest;
import com.aleyfrag.interview_service.dto.SubmitAnswerResponse;
import com.aleyfrag.interview_service.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // ---------------------------------------------------------
    // START INTERVIEW
    // ---------------------------------------------------------

    @PostMapping
    public StartInterviewResponse startInterview(
            @Valid @RequestBody StartInterviewRequest request) {

        return interviewService.startInterview(
                request.jobRole(),
                request.experienceYears()
        );
    }

    // ---------------------------------------------------------
    // ANSWER THE QUESTIONS
    // ---------------------------------------------------------


    @PostMapping("/{interviewId}/answers")
    public SubmitAnswerResponse submitAnswer(
            @PathVariable("interviewId") UUID interviewId,
            @Valid @RequestBody SubmitAnswerRequest request) {

        return interviewService.submitAnswer(interviewId, request);
    }

    // ---------------------------------------------------------
    // DOWNLOAD QUESTIONS
    // ---------------------------------------------------------

    @GetMapping("/{interviewId}/questions/download")
    public ResponseEntity<byte[]> downloadQuestions(
            @PathVariable UUID interviewId)
            throws IOException {

        byte[] zipFile =
                interviewService.downloadQuestions(interviewId);

        String disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                "interview-" + interviewId + ".zip"
                        )
                        .build()
                        .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/zip")
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition
                )
                .contentLength(zipFile.length)
                .body(zipFile);
    }
}
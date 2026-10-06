package com.aleyfrag.interview_service.controller;

import com.aleyfrag.interview_service.dto.StartInterviewRequest;
import com.aleyfrag.interview_service.dto.StartInterviewResponse;
import com.aleyfrag.interview_service.model.InterviewSession;
import com.aleyfrag.interview_service.service.GeminiService;
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

    public InterviewController(InterviewService interviewService){
        this.interviewService =  interviewService;
    }


    @PostMapping
    public StartInterviewResponse startInterview(@Valid @RequestBody StartInterviewRequest request){

        InterviewSession session = interviewService.startInterview(
                request.jobRole(),
                request.experienceYears()
        );

        return new StartInterviewResponse(
                session.interviewId(),
                session.questions().get(0).text()
        );

    }



    @GetMapping("/{interviewId}/questions/download")
    public ResponseEntity<byte[]> downloadQuestions(@PathVariable("interviewId") UUID interviewId)
    throws IOException {

        byte[] zipFile = interviewService.downloadQuestions(interviewId);

        String disposition = ContentDisposition.attachment()
                .filename("interview-"+interviewId+".zip")
                .build()
                .toString();

        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(zipFile.length)
                .body(zipFile);

    }
}

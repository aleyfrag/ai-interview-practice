package com.aleyfrag.interview_service.service;


import com.aleyfrag.interview_service.dto.StartInterviewRequest;
import com.aleyfrag.interview_service.dto.StartInterviewResponse;
import com.aleyfrag.interview_service.dto.SubmitAnswerRequest;
import com.aleyfrag.interview_service.model.InterviewQuestion;
import com.aleyfrag.interview_service.model.InterviewSession;
import com.aleyfrag.interview_service.model.InterviewTurn;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class InterviewService {

    private static final int MAX_QUESTIONS = 15;

    private final GeminiService geminiService;

    private final Map<UUID, InterviewSession> sessions =
            new ConcurrentHashMap<>();

    public InterviewService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }


    public InterviewSession startInterview(String jobRole, int experienceYears){

        String question = geminiService.generateFirstQuestion(
                jobRole,
                experienceYears
        );


        UUID interviewId = UUID.randomUUID();

        InterviewSession session = new InterviewSession(
                interviewId,
                jobRole,
                experienceYears,
                List.of(new InterviewQuestion(1,question))
        );

        sessions.put(interviewId, session);

        return session;
    }


    public  byte[] downloadQuestions(UUID interviewId) throws IOException{
        InterviewSession session= sessions.get(interviewId);

        if(session == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Interview not found. It may have expired");
        }

        StringBuilder text = new StringBuilder();

        text.append("AI Interview Practice\n\n");
        text.append("Interview ID: ").append(session.interviewId()).append("\n");
        text.append("Role: ").append(session.jobRole()).append("\n");
        text.append("Experience: ")
                .append(session.experienceYears())
                .append(" years\n\n");

        for (InterviewQuestion question : session.questions()) {
            text.append(question.number())
                    .append(". ")
                    .append(question.text())
                    .append("\n\n");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try(ZipOutputStream zip =
                new ZipOutputStream(output, StandardCharsets.UTF_8)){
            zip.putNextEntry(new ZipEntry("questions.txt"));
            zip.write(text.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return output.toByteArray();

    }












    private static class Session {

        private final String jobRole;
        private final int experienceYears;
        private final List<InterviewTurn> turns = new ArrayList<>();
        private boolean completed;

        private Session(String jobRole, int experienceYears) {
            this.jobRole = jobRole;
            this.experienceYears = experienceYears;
        }


    }
}

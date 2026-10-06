package com.aleyfrag.interview_service.service;


import com.aleyfrag.interview_service.dto.StartInterviewRequest;
import com.aleyfrag.interview_service.dto.StartInterviewResponse;
import com.aleyfrag.interview_service.dto.SubmitAnswerRequest;
import com.aleyfrag.interview_service.dto.SubmitAnswerResponse;
import com.aleyfrag.interview_service.model.AiEvaluation;
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

    private final Map<UUID, Session> sessions =
            new ConcurrentHashMap<>();

    public InterviewService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // ---------------------------------------------------------
    // START INTERVIEW
    // ---------------------------------------------------------

    public StartInterviewResponse startInterview(
            String jobRole,
            int experienceYears) {

        // Generate first question using Gemini
        String question = geminiService.generateFirstQuestion(
                jobRole,
                experienceYears
        );

        // Generate unique interview ID
        UUID interviewId = UUID.randomUUID();

        // Create session
        Session session = new Session(
                jobRole,
                experienceYears
        );

        // IMPORTANT:
        // Store the first question inside the session.
        session.turns.add(
                new InterviewTurn(
                        1,
                        question,
                        null,
                        null,
                        null
                )
        );

        // Store session
        sessions.put(interviewId, session);

        return new StartInterviewResponse(
                interviewId,
                1,
                question
        );
    }

    // ---------------------------------------------------------
    // DOWNLOAD QUESTIONS
    // ---------------------------------------------------------

    public byte[] downloadQuestions(UUID interviewId)
            throws IOException {

        Session session = sessions.get(interviewId);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Interview not found. It may have expired"
            );
        }

        StringBuilder text = new StringBuilder();

        text.append("AI Interview Practice\n\n");

        text.append("Interview ID: ")
                .append(interviewId)
                .append("\n");

        text.append("Role: ")
                .append(session.jobRole)
                .append("\n");

        text.append("Experience: ")
                .append(session.experienceYears)
                .append(" years\n\n");

        // Add all questions
        for (InterviewTurn turn : session.turns) {

            text.append(turn.questionNumber())
                    .append(". ")
                    .append(turn.question())
                    .append("\n\n");
        }

        // Create ZIP in memory
        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try (ZipOutputStream zip =
                     new ZipOutputStream(
                             output,
                             StandardCharsets.UTF_8)) {

            zip.putNextEntry(
                    new ZipEntry("questions.txt")
            );

            zip.write(
                    text.toString()
                            .getBytes(StandardCharsets.UTF_8)
            );

            zip.closeEntry();
        }

        return output.toByteArray();
    }

    // ---------------------------------------------------------
    // SUBMIT ANSWER
    // ---------------------------------------------------------

    public SubmitAnswerResponse submitAnswer(
            UUID interviewId,
            SubmitAnswerRequest request) {

        Session session = sessions.get(interviewId);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Interview not found. Start a new interview."
            );
        }

        /*
         * synchronized(session) ensures that two requests
         * cannot modify the same interview simultaneously.
         */
        synchronized (session) {

            // Check if interview is already completed
            if (session.completed) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This interview is already completed."
                );
            }

            // Safety check
            if (session.turns.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Interview has no questions."
                );
            }

            // Get current question
            int currentIndex =
                    session.turns.size() - 1;

            InterviewTurn current =
                    session.turns.get(currentIndex);

            // Make sure user is answering the correct question
            if (request.questionNumber()
                    != current.questionNumber()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Please answer the current question."
                );
            }

            // Check whether this is question 15
            boolean lastQuestion =
                    current.questionNumber() >= MAX_QUESTIONS;

            // Clean answer
            String answer = request.answer().trim();

            // Send answer to Gemini for evaluation
            AiEvaluation evaluation =
                    geminiService.evaluateAnswer(
                            session.jobRole,
                            session.experienceYears,
                            List.copyOf(session.turns),
                            answer,
                            lastQuestion
                    );

            // Replace current unanswered turn
            // with evaluated answer
            session.turns.set(
                    currentIndex,
                    new InterviewTurn(
                            current.questionNumber(),
                            current.question(),
                            answer,
                            evaluation.score(),
                            evaluation.feedback()
                    )
            );

            // -------------------------------------------------
            // INTERVIEW COMPLETED
            // -------------------------------------------------

            if (lastQuestion) {

                session.completed = true;

                return new SubmitAnswerResponse(
                        current.questionNumber(),
                        evaluation.score(),
                        evaluation.feedback(),
                        null,
                        null,
                        true
                );
            }

            // -------------------------------------------------
            // NEXT QUESTION
            // -------------------------------------------------

            int nextNumber =
                    current.questionNumber() + 1;

            String nextQuestion =
                    evaluation.nextQuestion().trim();

            // Add next question to session
            session.turns.add(
                    new InterviewTurn(
                            nextNumber,
                            nextQuestion,
                            null,
                            null,
                            null
                    )
            );

            return new SubmitAnswerResponse(
                    current.questionNumber(),
                    evaluation.score(),
                    evaluation.feedback(),
                    nextNumber,
                    nextQuestion,
                    false
            );
        }
    }

    // ---------------------------------------------------------
    // SESSION
    // ---------------------------------------------------------

    private static class Session {

        private final String jobRole;

        private final int experienceYears;

        private final List<InterviewTurn> turns =
                new ArrayList<>();

        private boolean completed;

        private Session(
                String jobRole,
                int experienceYears) {

            this.jobRole = jobRole;
            this.experienceYears = experienceYears;
        }
    }
}
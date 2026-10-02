package com.aleyfrag.interview_service.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;
    private final String model;

    public GeminiService(@Value("${gemini.api.key}") String apiKey,
                         @Value("${gemini.model}") String model){

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();

        this.model = model;

    }


    public String generateFirstQuestion(String jobRole, int experienceYears){
        String prompt = """
                You are conducting a job interview.
                Ask exactly one opening interview question suitable for
                the following job role and experience level.

                Job role: %s
                Experience: %d years

                Return only the question.
                Do not include an answer, greeting, or list of questions.
                """.formatted(jobRole, experienceYears);

        GenerateContentResponse response =
                client.models.generateContent(model,prompt,null);

        String question = response.text();

        if (question == null || question.isBlank()) {
            throw new IllegalStateException("Gemini returned no question.");
        }

        return question.trim();


    }
}

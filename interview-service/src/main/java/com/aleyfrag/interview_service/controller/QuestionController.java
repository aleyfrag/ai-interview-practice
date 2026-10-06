package com.aleyfrag.interview_service.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @GetMapping("/sample")
    public String getSampleQuestion(){
        return "What is the difference between an interface and an abstract class in Java?";
    }
}

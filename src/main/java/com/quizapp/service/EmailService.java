package com.quizapp.service;

public interface EmailService {

    void sendSimpleEmail(String to, String subject, String body);
}

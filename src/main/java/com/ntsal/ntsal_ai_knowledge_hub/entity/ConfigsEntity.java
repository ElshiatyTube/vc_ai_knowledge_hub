package com.ntsal.ntsal_ai_knowledge_hub.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "configs")
@Data
public class ConfigsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_token")
    private String githubToken;

    @Column(name = "llm_api_key")
    private String llmApiKey;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "llm_summarizer_url")
    private String llmSummarizerUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


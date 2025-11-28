package com.ntsal.ntsal_ai_knowledge_hub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ntsal.ntsal_ai_knowledge_hub.entity.converter.PGvectorType;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "commit")
@Data
public class CommitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commit_hash", nullable = false)
    private String commitHash;
    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "diff_text", columnDefinition = "TEXT", nullable = false)
   // @JsonIgnore
    private String diffText;
    @Column(name = "committed_date", nullable = false)
    private LocalDateTime committedDate;
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "embedding_vector", columnDefinition = "vector(384)")
    @Type(PGvectorType.class)
    @JsonIgnore
    private PGvector embeddingVector;

    @JsonProperty("hasEmbedding")
    public boolean hasEmbedding() {
        return embeddingVector != null;
    }

    @ManyToOne
    @JoinColumn(name = "github_repo_id", nullable = false,referencedColumnName = "id")
    private GithubRepoEntity githubRepo;
}
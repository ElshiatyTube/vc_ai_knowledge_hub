package com.ntsal.ntsal_ai_knowledge_hub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "github_repo")
@Data
public class GithubRepoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "owner")
    private String owner;

    @Column(name = "branch")
    private String branch;

    @OneToMany(mappedBy = "githubRepo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CommitEntity> commits;
}

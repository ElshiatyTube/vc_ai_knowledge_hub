package com.ntsal.ntsal_ai_knowledge_hub.controller;

import com.ntsal.ntsal_ai_knowledge_hub.entity.GithubRepoEntity;
import com.ntsal.ntsal_ai_knowledge_hub.service.GithubRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/github-repos")
public class GithubRepoController {
    private final GithubRepoService githubRepoService;

    @Autowired
    public GithubRepoController(GithubRepoService githubRepoService) {
        this.githubRepoService = githubRepoService;
    }

    @GetMapping
    public List<GithubRepoEntity> findAll() {
        return githubRepoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GithubRepoEntity> getRepoById(@PathVariable Long id) {
        GithubRepoEntity repo = githubRepoService.findById(id);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repo);
    }
    @PostMapping
    public GithubRepoEntity createRepo(@RequestBody GithubRepoEntity repo) {
        return githubRepoService.createRepo(repo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepo(@PathVariable Long id) {
        githubRepoService.deleteRepo(id);
        return ResponseEntity.noContent().build();
    }
}


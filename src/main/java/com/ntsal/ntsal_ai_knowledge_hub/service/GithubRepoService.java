package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.ntsal.ntsal_ai_knowledge_hub.entity.GithubRepoEntity;
import com.ntsal.ntsal_ai_knowledge_hub.repo.GithubRepoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubRepoService {
    private final GithubRepoRepository githubRepoRepository;

    @Autowired
    public GithubRepoService(GithubRepoRepository githubRepoRepository) {
        this.githubRepoRepository = githubRepoRepository;
    }

  //  @Cacheable("allGithubRepos")
    public List<GithubRepoEntity> findAll() {
        return githubRepoRepository.findAll();
    }

    public GithubRepoEntity findById(Long id) {
        return githubRepoRepository.findById(id).orElse(null);
    }

    public GithubRepoEntity createRepo(GithubRepoEntity repo) {
        return githubRepoRepository.save(repo);
    }

    public void deleteRepo(Long id) {
        githubRepoRepository.deleteById(id);
    }
}

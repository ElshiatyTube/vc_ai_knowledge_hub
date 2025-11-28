package com.ntsal.ntsal_ai_knowledge_hub.repo;

import com.ntsal.ntsal_ai_knowledge_hub.entity.GithubRepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GithubRepoRepository extends JpaRepository<GithubRepoEntity, Long> {
}

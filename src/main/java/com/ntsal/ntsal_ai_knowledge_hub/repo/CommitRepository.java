package com.ntsal.ntsal_ai_knowledge_hub.repo;

import com.ntsal.ntsal_ai_knowledge_hub.entity.CommitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommitRepository extends JpaRepository<CommitEntity, Long> {
    boolean existsByCommitHash(String commitHash);

    @Query(value = """
    SELECT *
    FROM commit
    ORDER BY embedding_vector <=> cast(:queryEmbedding AS vector)
    LIMIT 50
""", nativeQuery = true)
    List<CommitEntity> searchByEmbedding(@Param("queryEmbedding") String queryEmbedding);
}
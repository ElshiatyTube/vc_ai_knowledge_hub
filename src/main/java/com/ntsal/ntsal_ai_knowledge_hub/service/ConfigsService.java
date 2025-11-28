package com.ntsal.ntsal_ai_knowledge_hub.service;

import com.ntsal.ntsal_ai_knowledge_hub.entity.ConfigsEntity;
import com.ntsal.ntsal_ai_knowledge_hub.repo.ConfigsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class ConfigsService {
    private final ConfigsRepository configsRepository;

    @Autowired
    public ConfigsService(ConfigsRepository configsRepository) {
        this.configsRepository = configsRepository;
    }

   // @Cacheable("latestConfig")
    public ConfigsEntity getLatestConfig() {
        return configsRepository.findAll()
                .stream()
                .max(Comparator.comparing(ConfigsEntity::getCreatedAt))
                .orElse(null);
    }
}

package com.ntsal.ntsal_ai_knowledge_hub.entity.converter;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PGvectorConverter implements AttributeConverter<PGvector, String> {

    @Override
    public String convertToDatabaseColumn(PGvector attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public PGvector convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return new PGvector(dbData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert database data to PGvector", e);
        }
    }
}


--liquibase formatted sql

--changeset admin:20251116_configs_table.sql
CREATE TABLE IF NOT EXISTS public.configs
(
    id                 BIGSERIAL NOT NULL,
    github_token       VARCHAR(255),
    llm_api_key        TEXT,
    llm_model          VARCHAR(255),
    llm_summarizer_url VARCHAR(255),
    created_at         TIMESTAMP,
    PRIMARY KEY (id)
)
    TABLESPACE pg_default;

ALTER TABLE public.configs
    OWNER to ${user_owner};
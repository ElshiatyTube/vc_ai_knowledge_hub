--liquibase formatted sql

--changeset admin:github_repo.sql
CREATE TABLE IF NOT EXISTS public.github_repo
(
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    branch varchar DEFAULT 'main'
)
    TABLESPACE pg_default;
ALTER TABLE public.github_repo
    OWNER to ${user_owner};

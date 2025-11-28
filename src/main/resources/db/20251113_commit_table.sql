--liquibase formatted sql

--changeset admin:20251113_commit_table.sql
CREATE TABLE IF NOT EXISTS public.commit
(
    id             BIGSERIAL NOT NULL,
    commit_hash    varchar   NOT NULL,
    author         varchar   NOT NULL,
    message        varchar   NOT NULL,
    diff_text      TEXT      NOT NULL,
    summary_text TEXT,
    feedback TEXT,
    embedding_vector vector(384),
    committed_date TIMESTAMP NOT NULL,
    github_repo_id BIGINT     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT commit_github_repo_id_fkey FOREIGN KEY (github_repo_id)
        REFERENCES public.github_repo (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
    TABLESPACE pg_default;

ALTER TABLE public.commit
    OWNER to ${user_owner};

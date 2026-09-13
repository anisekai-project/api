CREATE TABLE `session_token_scope`
(
    `token_id` BINARY(16)  NOT NULL,
    `scope`    VARCHAR(64) NOT NULL,
    CONSTRAINT `pk_session_token_scope` PRIMARY KEY (`token_id`, `scope`),
    CONSTRAINT `FK_SESSION_TOKEN_SCOPE_ON_TOKEN` FOREIGN KEY (`token_id`) REFERENCES `session_token` (`id`)
);

-- Existing keys intentionally receive no scopes (fail-closed). Enforcement is opt-in per route via
-- @RequireAuth(scopes = ...), so undeclared routes keep their current behavior.

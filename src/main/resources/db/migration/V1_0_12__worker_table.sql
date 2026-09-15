CREATE TABLE `worker` (
    `id`               BINARY(16)   NOT NULL,
    `last_ping`        DATETIME(6)  NOT NULL,
    `name`             VARCHAR(255) NULL,
    `session_token_id` BINARY(16)  NOT NULL,
    CONSTRAINT `pk_worker` PRIMARY KEY (`id`),
    CONSTRAINT `FK_WORKER_ON_SESSION_TOKEN` FOREIGN KEY (`session_token_id`) REFERENCES `session_token` (`id`)
);

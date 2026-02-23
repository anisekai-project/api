-- Phase 1: Refactor user flags
ALTER TABLE `user`
    ADD `flags` INT NOT NULL DEFAULT 0 AFTER `emote`;

UPDATE `user`
SET `flags` = `flags` | 1
WHERE `administrator` = TRUE;

UPDATE `user`
SET `flags` = `flags` | 2
WHERE `guest` = FALSE;

UPDATE `user`
SET `flags` = `flags` | 4
WHERE `active` = TRUE;

ALTER TABLE `user`
    DROP COLUMN `administrator`;
ALTER TABLE `user`
    DROP COLUMN `guest`;
ALTER TABLE `user`
    DROP COLUMN `active`;

-- Phase 2: Create worker table
CREATE TABLE `worker`
(
    `id`               BINARY(16)   NOT NULL,
    `session_token_id` BINARY(16)   NOT NULL,
    `hostname`         VARCHAR(255) NULL,
    `last_heartbeat`   DATETIME     NOT NULL,
    `created_at`       DATETIME     NOT NULL,
    `updated_at`       DATETIME     NOT NULL,
    CONSTRAINT `pk_worker` PRIMARY KEY (`id`)
);

ALTER TABLE `worker`
    ADD CONSTRAINT `FK_WORKER_ON_SESSIONTOKEN` FOREIGN KEY (`session_token_id`) REFERENCES `session_token` (`id`);

-- Phase 3: Update task table for worker assignment
ALTER TABLE `task`
    ADD `worker_id` BINARY(16) NULL AFTER `status`;
ALTER TABLE `task`
    ADD `isolation_id` BINARY(16) NULL AFTER `arguments`;
ALTER TABLE `task`
    ADD `expires_at` DATETIME NULL AFTER `started_at`;

ALTER TABLE `task`
    ADD CONSTRAINT `FK_TASK_ON_WORKER` FOREIGN KEY (`worker_id`) REFERENCES `worker` (`id`);

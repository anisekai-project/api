-- Persisted legacy work cannot be safely resumed after the factory/argument format migration.
UPDATE `task`
SET `status`       = 'CANCELED',
    `completed_at` = COALESCE(`completed_at`, CURRENT_TIMESTAMP),
    `updated_at`   = CURRENT_TIMESTAMP
WHERE `status` IN ('SCHEDULED', 'EXECUTING');

UPDATE `task`
SET `arguments` = '{}'
WHERE `arguments` IS NULL;

ALTER TABLE `task`
    MODIFY COLUMN `arguments` LONGTEXT NOT NULL,
    ADD COLUMN `active_key` VARCHAR(511) NULL,
    ADD CONSTRAINT `uc_task_active_key` UNIQUE (`active_key`);

CREATE INDEX `idx_task_dispatch`
    ON `task` (`status`, `priority` DESC, `created_at`, `id`);

CREATE TABLE `application_lock` (
    `name` VARCHAR(64) NOT NULL,
    CONSTRAINT `pk_application_lock` PRIMARY KEY (`name`)
);

INSERT INTO `application_lock` (`name`)
VALUES ('broadcast-schedule'), ('task-queue');

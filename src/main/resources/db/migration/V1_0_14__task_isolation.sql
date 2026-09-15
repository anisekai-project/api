ALTER TABLE `task`
    ADD COLUMN `isolation_id` BINARY(16) NULL;

CREATE INDEX `idx_task_isolation` ON `task` (`isolation_id`);

ALTER TABLE `worker`
    ADD COLUMN `instance_id` BINARY(16) NULL;

CREATE INDEX `idx_worker_last_ping_instance`
    ON `worker` (`last_ping`, `instance_id`);

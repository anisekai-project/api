ALTER TABLE `task`
    ADD COLUMN `worker_id` BINARY(16) NULL,
    ADD CONSTRAINT `FK_TASK_ON_WORKER` FOREIGN KEY (`worker_id`) REFERENCES `worker` (`id`);

CREATE INDEX `idx_task_worker` ON `task` (`worker_id`);

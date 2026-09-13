-- Refuse to start destructive DDL when legacy relationships cannot be mapped.
DELIMITER //
CREATE PROCEDURE `validate_v1_0_9_source`()
BEGIN
    IF EXISTS (SELECT 1 FROM `broadcast` `b` LEFT JOIN `anime` `a` ON `a`.`id` = `b`.`watch_target_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `episode` `e` LEFT JOIN `anime` `a` ON `a`.`id` = `e`.`anime_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `interest` `i` LEFT JOIN `anime` `a` ON `a`.`id` = `i`.`anime_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection_animes` `sa` LEFT JOIN `selection` `s` ON `s`.`id` = `sa`.`selection_id` WHERE `s`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection_animes` `sa` LEFT JOIN `anime` `a` ON `a`.`id` = `sa`.`animes_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `torrent_file` `tf` LEFT JOIN `torrent` `t` ON `t`.`id` = `tf`.`torrent_id` WHERE `t`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `torrent_file` `tf` LEFT JOIN `episode` `e` ON `e`.`id` = `tf`.`episode_id` WHERE `e`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `track` `t` LEFT JOIN `episode` `e` ON `e`.`id` = `t`.`episode_id` WHERE `e`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter` `v` LEFT JOIN `selection` `s` ON `s`.`id` = `v`.`selection_id` WHERE `s`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter_votes` `vv` LEFT JOIN `voter` `v` ON `v`.`selection_id` = `vv`.`voter_selection_id` AND `v`.`user_id` = `vv`.`voter_user_id` WHERE `v`.`selection_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter_votes` `vv` LEFT JOIN `anime` `a` ON `a`.`id` = `vv`.`votes_id` WHERE `a`.`id` IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: orphaned legacy relationship';
    END IF;

    IF EXISTS (SELECT 1 FROM `torrent` WHERE `hash` IS NULL OR CHAR_LENGTH(`hash`) > 40)
        OR EXISTS (SELECT `hash` FROM `torrent` GROUP BY `hash` HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: invalid or duplicate torrent hash';
    END IF;

    IF EXISTS (SELECT `votes_id` FROM `voter_votes` GROUP BY `votes_id` HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: anime has more than one voter vote';
    END IF;
END//
DELIMITER ;

CALL `validate_v1_0_9_source`();
DROP PROCEDURE `validate_v1_0_9_source`;

ALTER TABLE `anime` ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`;
ALTER TABLE `broadcast`
    ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`,
    ADD COLUMN `new_watch_target_id` BINARY(16) NULL AFTER `watch_target_id`;
ALTER TABLE `episode`
    ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`,
    ADD COLUMN `new_anime_id` BINARY(16) NULL AFTER `anime_id`;
ALTER TABLE `selection` ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`;
ALTER TABLE `task` ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`;
ALTER TABLE `track`
    ADD COLUMN `new_id` BINARY(16) NULL AFTER `id`,
    ADD COLUMN `new_episode_id` BINARY(16) NULL AFTER `episode_id`;
ALTER TABLE `interest` ADD COLUMN `new_anime_id` BINARY(16) NULL AFTER `anime_id`;
ALTER TABLE `selection_animes`
    ADD COLUMN `new_selection_id` BINARY(16) NULL,
    ADD COLUMN `new_animes_id` BINARY(16) NULL;
ALTER TABLE `torrent_file` ADD COLUMN `new_episode_id` BINARY(16) NULL AFTER `episode_id`;
ALTER TABLE `voter` ADD COLUMN `new_selection_id` BINARY(16) NULL AFTER `selection_id`;
ALTER TABLE `voter_votes`
    ADD COLUMN `new_voter_selection_id` BINARY(16) NULL AFTER `voter_selection_id`,
    ADD COLUMN `new_votes_id` BINARY(16) NULL AFTER `votes_id`;

-- UNHEX gives Hibernate the standard 16 UUID bytes without relying on UUID cast semantics.
UPDATE `anime` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;
UPDATE `broadcast` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;
UPDATE `episode` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;
UPDATE `selection` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;
UPDATE `task` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;
UPDATE `track` SET `new_id` = UNHEX(REPLACE(CAST(UUID_v7() AS CHAR(36)), '-', '')) ORDER BY `created_at`, `id`;

UPDATE `broadcast` `b` JOIN `anime` `a` ON `a`.`id` = `b`.`watch_target_id`
SET `b`.`new_watch_target_id` = `a`.`new_id`;
UPDATE `episode` `e` JOIN `anime` `a` ON `a`.`id` = `e`.`anime_id`
SET `e`.`new_anime_id` = `a`.`new_id`;
UPDATE `interest` `i` JOIN `anime` `a` ON `a`.`id` = `i`.`anime_id`
SET `i`.`new_anime_id` = `a`.`new_id`;
UPDATE `selection_animes` `sa` JOIN `selection` `s` ON `s`.`id` = `sa`.`selection_id`
SET `sa`.`new_selection_id` = `s`.`new_id`;
UPDATE `selection_animes` `sa` JOIN `anime` `a` ON `a`.`id` = `sa`.`animes_id`
SET `sa`.`new_animes_id` = `a`.`new_id`;
UPDATE `torrent_file` `tf` JOIN `episode` `e` ON `e`.`id` = `tf`.`episode_id`
SET `tf`.`new_episode_id` = `e`.`new_id`;
UPDATE `track` `t` JOIN `episode` `e` ON `e`.`id` = `t`.`episode_id`
SET `t`.`new_episode_id` = `e`.`new_id`;
UPDATE `voter` `v` JOIN `selection` `s` ON `s`.`id` = `v`.`selection_id`
SET `v`.`new_selection_id` = `s`.`new_id`;
UPDATE `voter_votes` `vv` JOIN `selection` `s` ON `s`.`id` = `vv`.`voter_selection_id`
SET `vv`.`new_voter_selection_id` = `s`.`new_id`;
UPDATE `voter_votes` `vv` JOIN `anime` `a` ON `a`.`id` = `vv`.`votes_id`
SET `vv`.`new_votes_id` = `a`.`new_id`;

DELIMITER //
CREATE PROCEDURE `validate_v1_0_9_mapping`()
BEGIN
    IF EXISTS (SELECT 1 FROM `anime` WHERE `new_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `broadcast` WHERE `new_id` IS NULL OR `new_watch_target_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `episode` WHERE `new_id` IS NULL OR `new_anime_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection` WHERE `new_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `task` WHERE `new_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `track` WHERE `new_id` IS NULL OR `new_episode_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `interest` WHERE `new_anime_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection_animes` WHERE `new_selection_id` IS NULL OR `new_animes_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `torrent_file` WHERE `torrent_id` IS NULL OR `new_episode_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter` WHERE `new_selection_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter_votes` WHERE `new_voter_selection_id` IS NULL OR `new_votes_id` IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: UUID mapping produced null values';
    END IF;

    IF (SELECT COUNT(*) FROM `anime`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `anime`)
        OR (SELECT COUNT(*) FROM `broadcast`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `broadcast`)
        OR (SELECT COUNT(*) FROM `episode`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `episode`)
        OR (SELECT COUNT(*) FROM `selection`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `selection`)
        OR (SELECT COUNT(*) FROM `task`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `task`)
        OR (SELECT COUNT(*) FROM `track`) <> (SELECT COUNT(DISTINCT `new_id`) FROM `track`) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: duplicate generated UUID';
    END IF;
END//
DELIMITER ;

CALL `validate_v1_0_9_mapping`();
DROP PROCEDURE `validate_v1_0_9_mapping`;

-- Drop only relationships whose key representation changes. Numeric user and Discord IDs stay intact.
ALTER TABLE `broadcast` DROP FOREIGN KEY `FK_BROADCAST_ON_WATCHTARGET`;
ALTER TABLE `episode` DROP FOREIGN KEY `FK_EPISODE_ON_ANIME`;
ALTER TABLE `interest`
    DROP FOREIGN KEY `FK_INTEREST_ON_ANIME`,
    DROP FOREIGN KEY `FK_INTEREST_ON_USER`;
ALTER TABLE `torrent_file` DROP FOREIGN KEY `FK_TORRENTFILE_ON_EPISODE`;
ALTER TABLE `track` DROP FOREIGN KEY `FK_TRACK_ON_EPISODE`;
ALTER TABLE `voter`
    DROP FOREIGN KEY `FK_VOTER_ON_SELECTION`,
    DROP FOREIGN KEY `FK_VOTER_ON_USER`;
ALTER TABLE `selection_animes`
    DROP FOREIGN KEY `fk_selani_on_anime`,
    DROP FOREIGN KEY `fk_selani_on_selection`;
ALTER TABLE `voter_votes`
    DROP FOREIGN KEY `fk_votvot_on_anime`,
    DROP FOREIGN KEY `fk_votvot_on_voter`;

-- Only media entities retain the old numeric key for existing on-disk paths.
ALTER TABLE `anime` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `anime` DROP PRIMARY KEY;
ALTER TABLE `anime`
    CHANGE COLUMN `id` `disk_id` BIGINT NULL,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_anime_uuid` PRIMARY KEY (`id`),
    ADD CONSTRAINT `uc_anime_disk_id` UNIQUE (`disk_id`);

ALTER TABLE `episode` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `episode` DROP PRIMARY KEY;
ALTER TABLE `episode`
    CHANGE COLUMN `id` `disk_id` BIGINT NULL,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_episode_uuid` PRIMARY KEY (`id`),
    ADD CONSTRAINT `uc_episode_disk_id` UNIQUE (`disk_id`);

ALTER TABLE `track` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `track` DROP PRIMARY KEY;
ALTER TABLE `track`
    CHANGE COLUMN `id` `disk_id` BIGINT NULL,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_track_uuid` PRIMARY KEY (`id`),
    ADD CONSTRAINT `uc_track_disk_id` UNIQUE (`disk_id`);

ALTER TABLE `broadcast` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `broadcast` DROP PRIMARY KEY;
ALTER TABLE `broadcast`
    DROP COLUMN `id`,
    DROP COLUMN `watch_target_id`,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    CHANGE COLUMN `new_watch_target_id` `watch_target_id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_broadcast_uuid` PRIMARY KEY (`id`);

ALTER TABLE `selection` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `selection` DROP PRIMARY KEY;
ALTER TABLE `selection`
    DROP COLUMN `id`,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_selection_uuid` PRIMARY KEY (`id`);

ALTER TABLE `task` MODIFY COLUMN `id` BIGINT NOT NULL;
ALTER TABLE `task` DROP PRIMARY KEY;
ALTER TABLE `task`
    DROP COLUMN `id`,
    CHANGE COLUMN `new_id` `id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_task_uuid` PRIMARY KEY (`id`);

ALTER TABLE `episode`
    DROP COLUMN `anime_id`,
    CHANGE COLUMN `new_anime_id` `anime_id` BINARY(16) NOT NULL;

ALTER TABLE `interest` DROP PRIMARY KEY;
ALTER TABLE `interest`
    DROP COLUMN `anime_id`,
    CHANGE COLUMN `new_anime_id` `anime_id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_interest_uuid` PRIMARY KEY (`user_id`, `anime_id`);

ALTER TABLE `selection_animes` DROP PRIMARY KEY;
ALTER TABLE `selection_animes`
    DROP COLUMN `selection_id`,
    DROP COLUMN `animes_id`,
    CHANGE COLUMN `new_selection_id` `selection_id` BINARY(16) NOT NULL,
    CHANGE COLUMN `new_animes_id` `animes_id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_selection_animes_uuid` PRIMARY KEY (`selection_id`, `animes_id`);

ALTER TABLE `torrent_file`
    DROP COLUMN `episode_id`,
    CHANGE COLUMN `new_episode_id` `episode_id` BINARY(16) NOT NULL;

ALTER TABLE `track`
    DROP COLUMN `episode_id`,
    CHANGE COLUMN `new_episode_id` `episode_id` BINARY(16) NOT NULL;

ALTER TABLE `voter` DROP PRIMARY KEY;
ALTER TABLE `voter`
    DROP COLUMN `selection_id`,
    CHANGE COLUMN `new_selection_id` `selection_id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_voter_uuid` PRIMARY KEY (`selection_id`, `user_id`);

ALTER TABLE `voter_votes` DROP PRIMARY KEY;
ALTER TABLE `voter_votes`
    DROP COLUMN `voter_selection_id`,
    DROP COLUMN `votes_id`,
    CHANGE COLUMN `new_voter_selection_id` `voter_selection_id` BINARY(16) NOT NULL,
    CHANGE COLUMN `new_votes_id` `votes_id` BINARY(16) NOT NULL,
    ADD CONSTRAINT `pk_voter_votes_uuid` PRIMARY KEY (`voter_selection_id`, `voter_user_id`, `votes_id`),
    ADD CONSTRAINT `uc_voter_votes_votes` UNIQUE (`votes_id`);

-- V1_0_7 omitted nullability even though both columns are required by the entity model.
ALTER TABLE `torrent` MODIFY COLUMN `id` BINARY(16) NOT NULL;
ALTER TABLE `torrent` MODIFY COLUMN `hash` VARCHAR(40) NOT NULL;
ALTER TABLE `torrent_file` MODIFY COLUMN `torrent_id` BINARY(16) NOT NULL;

CREATE INDEX `idx_episode_anime_number` ON `episode` (`anime_id`, `number`);
CREATE INDEX `idx_track_episode` ON `track` (`episode_id`);
CREATE INDEX `idx_broadcast_status_starting` ON `broadcast` (`status`, `starting_at`);
CREATE INDEX `idx_selection_status_period` ON `selection` (`status`, `year`, `season`);

ALTER TABLE `broadcast`
    ADD CONSTRAINT `FK_BROADCAST_ON_WATCHTARGET` FOREIGN KEY (`watch_target_id`) REFERENCES `anime` (`id`);
ALTER TABLE `episode`
    ADD CONSTRAINT `FK_EPISODE_ON_ANIME` FOREIGN KEY (`anime_id`) REFERENCES `anime` (`id`);
ALTER TABLE `interest`
    ADD CONSTRAINT `FK_INTEREST_ON_ANIME` FOREIGN KEY (`anime_id`) REFERENCES `anime` (`id`),
    ADD CONSTRAINT `FK_INTEREST_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);
ALTER TABLE `torrent_file`
    ADD CONSTRAINT `FK_TORRENTFILE_ON_EPISODE` FOREIGN KEY (`episode_id`) REFERENCES `episode` (`id`);
ALTER TABLE `track`
    ADD CONSTRAINT `FK_TRACK_ON_EPISODE` FOREIGN KEY (`episode_id`) REFERENCES `episode` (`id`);
ALTER TABLE `voter`
    ADD CONSTRAINT `FK_VOTER_ON_SELECTION` FOREIGN KEY (`selection_id`) REFERENCES `selection` (`id`),
    ADD CONSTRAINT `FK_VOTER_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);
ALTER TABLE `selection_animes`
    ADD CONSTRAINT `fk_selani_on_anime` FOREIGN KEY (`animes_id`) REFERENCES `anime` (`id`),
    ADD CONSTRAINT `fk_selani_on_selection` FOREIGN KEY (`selection_id`) REFERENCES `selection` (`id`);
ALTER TABLE `voter_votes`
    ADD CONSTRAINT `fk_votvot_on_anime` FOREIGN KEY (`votes_id`) REFERENCES `anime` (`id`),
    ADD CONSTRAINT `fk_votvot_on_voter` FOREIGN KEY (`voter_selection_id`, `voter_user_id`)
        REFERENCES `voter` (`selection_id`, `user_id`);

DELIMITER //
CREATE PROCEDURE `validate_v1_0_9_result`()
BEGIN
    IF EXISTS (SELECT 1 FROM `broadcast` `b` LEFT JOIN `anime` `a` ON `a`.`id` = `b`.`watch_target_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `episode` `e` LEFT JOIN `anime` `a` ON `a`.`id` = `e`.`anime_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `interest` `i` LEFT JOIN `anime` `a` ON `a`.`id` = `i`.`anime_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection_animes` `sa` LEFT JOIN `selection` `s` ON `s`.`id` = `sa`.`selection_id` WHERE `s`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `selection_animes` `sa` LEFT JOIN `anime` `a` ON `a`.`id` = `sa`.`animes_id` WHERE `a`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `torrent_file` `tf` LEFT JOIN `episode` `e` ON `e`.`id` = `tf`.`episode_id` WHERE `e`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `track` `t` LEFT JOIN `episode` `e` ON `e`.`id` = `t`.`episode_id` WHERE `e`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter` `v` LEFT JOIN `selection` `s` ON `s`.`id` = `v`.`selection_id` WHERE `s`.`id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter_votes` `vv` LEFT JOIN `voter` `v` ON `v`.`selection_id` = `vv`.`voter_selection_id` AND `v`.`user_id` = `vv`.`voter_user_id` WHERE `v`.`selection_id` IS NULL)
        OR EXISTS (SELECT 1 FROM `voter_votes` `vv` LEFT JOIN `anime` `a` ON `a`.`id` = `vv`.`votes_id` WHERE `a`.`id` IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1_0_9: post-migration relationship validation failed';
    END IF;
END//
DELIMITER ;

CALL `validate_v1_0_9_result`();
DROP PROCEDURE `validate_v1_0_9_result`;

CREATE TABLE IF NOT EXISTS `seven_signs_status` (
  `id`                  INT(3)              NOT NULL DEFAULT '0',
  `current_cycle`       INT(10)             NOT NULL DEFAULT '1',
  `festival_cycle`      INT(10)             NOT NULL DEFAULT '1',
  `active_period`       INT(10)             NOT NULL DEFAULT '1',
  `date`                BIGINT(13) UNSIGNED NOT NULL DEFAULT '0',
  `previous_winner`     INT(10)             NOT NULL DEFAULT '0',
  `dawn_stone_score`    DECIMAL(20, 0)      NOT NULL DEFAULT '0',
  `dawn_festival_score` INT(10)             NOT NULL DEFAULT '0',
  `dusk_stone_score`    DECIMAL(20, 0)      NOT NULL DEFAULT '0',
  `dusk_festival_score` INT(10)             NOT NULL DEFAULT '0',
  `avarice_owner`       INT(10)             NOT NULL DEFAULT '0',
  `gnosis_owner`        INT(10)             NOT NULL DEFAULT '0',
  `strife_owner`        INT(10)             NOT NULL DEFAULT '0',
  `avarice_dawn_score`  INT(10)             NOT NULL DEFAULT '0',
  `gnosis_dawn_score`   INT(10)             NOT NULL DEFAULT '0',
  `strife_dawn_score`   INT(10)             NOT NULL DEFAULT '0',
  `avarice_dusk_score`  INT(10)             NOT NULL DEFAULT '0',
  `gnosis_dusk_score`   INT(10)             NOT NULL DEFAULT '0',
  `strife_dusk_score`   INT(10)             NOT NULL DEFAULT '0',
  `accumulated_bonus0`  INT(10)             NOT NULL DEFAULT '0',
  `accumulated_bonus1`  INT(10)             NOT NULL DEFAULT '0',
  `accumulated_bonus2`  INT(10)             NOT NULL DEFAULT '0',
  `accumulated_bonus3`  INT(10)             NOT NULL DEFAULT '0',
  `accumulated_bonus4`  INT(10)             NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
);

INSERT IGNORE INTO `seven_signs_status` VALUES
  (0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
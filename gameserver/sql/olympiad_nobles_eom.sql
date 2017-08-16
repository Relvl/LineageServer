CREATE TABLE IF NOT EXISTS `olympiad_nobles_eom` (
  `char_id`            INT UNSIGNED        NOT NULL DEFAULT 0,
  `class_id`           TINYINT(3) UNSIGNED NOT NULL DEFAULT 0,
  `olympiad_points`    INT(10)             NOT NULL DEFAULT 0,
  `competitions_done`  SMALLINT(3)         NOT NULL DEFAULT 0,
  `competitions_won`   SMALLINT(3)         NOT NULL DEFAULT 0,
  `competitions_lost`  SMALLINT(3)         NOT NULL DEFAULT 0,
  `competitions_drawn` SMALLINT(3)         NOT NULL DEFAULT 0,
  PRIMARY KEY (`char_id`)
);
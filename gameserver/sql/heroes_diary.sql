CREATE TABLE IF NOT EXISTS `heroes_diary` (
  `char_id` INT(10) UNSIGNED    NOT NULL,
  `time`    BIGINT(13) UNSIGNED NOT NULL DEFAULT '0',
  `action`  TINYINT(2) UNSIGNED NOT NULL DEFAULT '0',
  `param`   INT(11) UNSIGNED    NOT NULL DEFAULT '0',
  KEY `char_id` (`char_id`)
);
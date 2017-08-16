CREATE TABLE IF NOT EXISTS `heroes` (
  `char_id`  DECIMAL(11, 0) NOT NULL DEFAULT '0',
  `class_id` DECIMAL(3, 0)  NOT NULL DEFAULT '0',
  `count`    DECIMAL(3, 0)  NOT NULL DEFAULT '0',
  `played`   DECIMAL(1, 0)  NOT NULL DEFAULT '0',
  `active`   TINYINT        NOT NULL DEFAULT 0,
  `message`  VARCHAR(300)   NOT NULL DEFAULT '',
  PRIMARY KEY (`char_id`)
);
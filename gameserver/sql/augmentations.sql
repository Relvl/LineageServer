CREATE TABLE IF NOT EXISTS `augmentations` (
  `item_id`     INT(11) NOT NULL DEFAULT 0,
  `attributes`  INT(11) NOT NULL DEFAULT -1,
  `skill_id`    INT(11) NOT NULL DEFAULT -1,
  `skill_level` INT(11) NOT NULL DEFAULT -1,
  PRIMARY KEY (`item_id`)
);
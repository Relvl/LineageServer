CREATE TABLE IF NOT EXISTS `items_on_ground` (
  `object_id`     INT(11) NOT NULL DEFAULT '0',
  `item_id`       INT(11)          DEFAULT NULL,
  `count`         INT(11)          DEFAULT NULL,
  `enchant_level` INT(11)          DEFAULT NULL,
  `x`             INT(11)          DEFAULT NULL,
  `y`             INT(11)          DEFAULT NULL,
  `z`             INT(11)          DEFAULT NULL,
  `time`          DECIMAL(20, 0)   DEFAULT NULL,
  PRIMARY KEY (`object_id`)
);
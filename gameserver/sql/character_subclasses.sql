CREATE TABLE IF NOT EXISTS `character_subclasses` (
  `char_obj_id` DECIMAL(11, 0) NOT NULL DEFAULT '0',
  `class_id`    INT(2)         NOT NULL DEFAULT '0',
  `exp`         DECIMAL(20, 0) NOT NULL DEFAULT '0',
  `sp`          DECIMAL(11, 0) NOT NULL DEFAULT '0',
  `level`       INT(2)         NOT NULL DEFAULT '40',
  `class_index` INT(1)         NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_obj_id`, `class_id`)
);
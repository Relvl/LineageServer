CREATE TABLE IF NOT EXISTS `mods_wedding` (
  `id`        INT(11) NOT NULL AUTO_INCREMENT,
  `player1Id` INT(11) NOT NULL DEFAULT '0',
  `player2Id` INT(11) NOT NULL DEFAULT '0',
  `married`   VARCHAR(5)       DEFAULT NULL,
  PRIMARY KEY (`id`)
);
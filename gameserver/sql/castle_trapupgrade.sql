CREATE TABLE IF NOT EXISTS `castle_trapupgrade` (
  `castleId`   TINYINT(3) UNSIGNED NOT NULL DEFAULT '0',
  `towerIndex` TINYINT(3) UNSIGNED NOT NULL DEFAULT '0',
  `level`      TINYINT(3) UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`towerIndex`, `castleId`)
);
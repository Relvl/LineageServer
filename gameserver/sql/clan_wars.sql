DROP TABLE IF EXISTS `clan_wars`;
CREATE TABLE `clan_wars` (
  `clan1`       VARCHAR(35)    NOT NULL DEFAULT '',
  `clan2`       VARCHAR(35)    NOT NULL DEFAULT '',
  `expiry_time` DECIMAL(20, 0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`clan1`, `clan2`)
);
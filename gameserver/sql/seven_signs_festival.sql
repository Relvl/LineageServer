CREATE TABLE IF NOT EXISTS `seven_signs_festival` (
  `festivalId` INT(1)       NOT NULL DEFAULT '0',
  `cabal`      VARCHAR(4)   NOT NULL DEFAULT '',
  `cycle`      INT(4)       NOT NULL DEFAULT '0',
  `date`       BIGINT(50)            DEFAULT '0',
  `score`      INT(5)       NOT NULL DEFAULT '0',
  `members`    VARCHAR(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`festivalId`, `cabal`, `cycle`)
);

INSERT IGNORE INTO `seven_signs_festival` VALUES
  (0, "dawn", 1, 0, 0, ""),
  (1, "dawn", 1, 0, 0, ""),
  (2, "dawn", 1, 0, 0, ""),
  (3, "dawn", 1, 0, 0, ""),
  (4, "dawn", 1, 0, 0, ""),
  (0, "dusk", 1, 0, 0, ""),
  (1, "dusk", 1, 0, 0, ""),
  (2, "dusk", 1, 0, 0, ""),
  (3, "dusk", 1, 0, 0, ""),
  (4, "dusk", 1, 0, 0, "");
CREATE TABLE IF NOT EXISTS `character_mail` (
  `charId`         INT(10)     NOT NULL,
  `letterId`       INT(10),
  `senderId`       INT(10)     NOT NULL,
  `location`       VARCHAR(45) NOT NULL,
  `recipientNames` VARCHAR(200)     DEFAULT NULL,
  `subject`        VARCHAR(128)     DEFAULT NULL,
  `message`        VARCHAR(3000)    DEFAULT NULL,
  `sentDate`       TIMESTAMP   NULL DEFAULT NULL,
  `unread`         SMALLINT(1)      DEFAULT 1,
  PRIMARY KEY (`letterId`)
);
CREATE TABLE IF NOT EXISTS `auction` (
  id             INT(11)        NOT NULL DEFAULT '0',
  sellerId       INT(11)        NOT NULL DEFAULT '0',
  sellerName     VARCHAR(20)    NOT NULL DEFAULT '',
  sellerClanName VARCHAR(20)    NOT NULL DEFAULT '',
  itemId         INT(11)        NOT NULL DEFAULT '0',
  itemName       VARCHAR(40)    NOT NULL DEFAULT '',
  startingBid    INT(11)        NOT NULL DEFAULT '0',
  currentBid     INT(11)        NOT NULL DEFAULT '0',
  endDate        DECIMAL(20, 0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`itemId`),
  KEY `id` (`id`)
);
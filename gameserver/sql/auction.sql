CREATE TABLE IF NOT EXISTS `auction` (
  id int(11) NOT NULL default '0',
  sellerId int(11) NOT NULL default '0',
  sellerName varchar(20) NOT NULL default '',
  sellerClanName varchar(20) NOT NULL default '',
  itemId int(11) NOT NULL default '0',
  itemName varchar(40) NOT NULL default '',
  startingBid int(11) NOT NULL default '0',
  currentBid int(11) NOT NULL default '0',
  endDate decimal(20,0) NOT NULL default '0',
  PRIMARY KEY (`itemId`),
  KEY `id` (`id`)
);
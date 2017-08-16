CREATE TABLE IF NOT EXISTS auction_bid (
  id         INT            NOT NULL DEFAULT 0,
  auctionId  INT            NOT NULL DEFAULT 0,
  bidderId   INT            NOT NULL DEFAULT 0,
  bidderName VARCHAR(50)    NOT NULL,
  clan_name  VARCHAR(50)    NOT NULL,
  maxBid     INT(11)        NOT NULL DEFAULT 0,
  time_bid   DECIMAL(20, 0) NOT NULL DEFAULT '0',
  PRIMARY KEY (auctionId, bidderId),
  KEY id (id)
);

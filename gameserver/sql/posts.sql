CREATE TABLE IF NOT EXISTS `posts` (
  `post_id`         INT(8)         NOT NULL DEFAULT '0',
  `post_owner_name` VARCHAR(255)   NOT NULL DEFAULT '',
  `post_ownerid`    INT(8)         NOT NULL DEFAULT '0',
  `post_date`       DECIMAL(20, 0) NOT NULL DEFAULT '0',
  `post_topic_id`   INT(8)         NOT NULL DEFAULT '0',
  `post_forum_id`   INT(8)         NOT NULL DEFAULT '0',
  `post_txt`        TEXT           NOT NULL
);

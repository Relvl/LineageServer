CREATE TABLE IF NOT EXISTS clan_skills (
  clan_id     INT(11) NOT NULL DEFAULT 0,
  skill_id    INT(11) NOT NULL DEFAULT 0,
  skill_level INT(5)  NOT NULL DEFAULT 0,
  skill_name  VARCHAR(26)      DEFAULT NULL,
  PRIMARY KEY (`clan_id`, `skill_id`)
);

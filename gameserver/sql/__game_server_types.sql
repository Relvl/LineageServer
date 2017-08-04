-- player perts
CREATE TYPE l2_position AS (
  x       INTEGER,
  y       INTEGER,
  z       INTEGER,
  heading NUMERIC(5)
);
CREATE TYPE l2_level AS (
  level SMALLINT,
  exp   BIGINT,
  sp    BIGINT
);
CREATE TYPE l2_player_appearance AS (
  face        SMALLINT,
  hair_color  SMALLINT,
  hair_style  SMALLINT,
  is_female   BOOL,
  name_color  VARCHAR(8),
  title_color VARCHAR(8),
  recom_have  SMALLINT,
  recom_left  SMALLINT
);
CREATE TYPE l2_player AS (
  object_id  INTEGER,
  login      VARCHAR(32),
  name       VARCHAR(32),
  title      VARCHAR(32),
  location   l2_position,
  level      l2_level,
  appearance l2_player_appearance
);

-- achievements
CREATE TYPE achievement_modify_data AS (
  playerid      INTEGER,
  achievementid VARCHAR(64),
  count         INTEGER,
  complete      BOOL
);
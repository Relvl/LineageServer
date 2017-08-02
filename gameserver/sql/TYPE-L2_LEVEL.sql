DROP TYPE IF EXISTS game_server.l2_level;
CREATE TYPE game_server.l2_level AS (
  level SMALLINT,
  exp   BIGINT,
  sp    BIGINT
);
SELECT '(1,0,0)' :: game_server.l2_level;
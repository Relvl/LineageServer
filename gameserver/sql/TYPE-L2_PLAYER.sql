DROP TYPE IF EXISTS game_server.l2_player;
CREATE TYPE game_server.l2_player AS (
  object_id INTEGER,
  login     VARCHAR(32),
  name      VARCHAR(32),
  title     VARCHAR(32),
  position  game_server.l2_location,
  level     game_server.l2_level
);
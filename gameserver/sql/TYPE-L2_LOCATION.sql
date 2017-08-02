DROP TYPE IF EXISTS game_server.l2_location;
CREATE TYPE game_server.l2_location AS (
  x       INT,
  y       INT,
  z       INT,
  heading NUMERIC(5)
);
select '(1,2,3,65536)'::game_server.l2_location;
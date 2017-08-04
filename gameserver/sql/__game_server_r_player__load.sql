CREATE OR REPLACE FUNCTION player__load(pi_player_id       INTEGER,
                                        po_player      OUT l2_player,
                                        po_result_code OUT INTEGER
)
  RETURNS RECORD LANGUAGE plpgsql AS
$$
DECLARE
  _player RECORD;
BEGIN

  SELECT *
  INTO _player
  FROM player p
  WHERE p.id = pi_player_id;

  po_player := (
    _player.id,
    _player.login,
    _player.name,
    _player.title,
    _player.position,
    _player.level
  );

  po_result_code := 0;
END;
$$
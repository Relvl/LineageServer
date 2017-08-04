CREATE SCHEMA pgcrypto;
GRANT USAGE ON SCHEMA pgcrypto TO PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA pgcrypto TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA pgcrypto GRANT EXECUTE ON FUNCTIONS TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA pgcrypto GRANT USAGE ON TYPES TO PUBLIC;
-- Задание поиска таблиц и функций по схемам.
SET search_path = pgcrypto, public;
-- Создание модуля pgcrypto для возможности работать с шифрованием в БД.
CREATE EXTENSION pgcrypto SCHEMA pgcrypto;

CREATE SEQUENCE account_game_id_seq START 2;
CREATE SEQUENCE gameservers_id_seq START 1;

CREATE TABLE account_game (
  id          INTEGER DEFAULT nextval('account_game_id_seq' :: REGCLASS) PRIMARY KEY NOT NULL,
  login       VARCHAR(32)                                                            NOT NULL,
  hash        TEXT                                                                   NOT NULL,
  last_active TIMESTAMP DEFAULT now()                                                NOT NULL,
  last_server INTEGER,
  regiser     TIMESTAMP DEFAULT now()                                                NOT NULL
);
CREATE UNIQUE INDEX account_game_id_uindex
  ON account_game (id);
CREATE UNIQUE INDEX account_game_login_uindex
  ON account_game (login);

CREATE TABLE gameservers (
  id    INTEGER DEFAULT nextval('gameservers_id_seq' :: REGCLASS) PRIMARY KEY NOT NULL,
  hexid BYTEA                                                                 NOT NULL
);
CREATE UNIQUE INDEX gameservers_id_uindex
  ON gameservers (id);

CREATE FUNCTION create_account(pi_login CHARACTER VARYING, pi_password CHARACTER VARYING)
  RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  created_acc_id INTEGER;
BEGIN
  INSERT INTO account_game (login, hash)
  VALUES (pi_login, pgcrypto.crypt(pi_password, pgcrypto.gen_salt('bf', 8)))
  RETURNING id
    INTO created_acc_id;
  RETURN created_acc_id;
END;
$$;

CREATE FUNCTION getgameservers()
  RETURNS REFCURSOR
LANGUAGE plpgsql
AS $$
DECLARE
  ref REFCURSOR;
BEGIN

  OPEN ref FOR
  SELECT
    gs.id    AS ID,
    gs.hexid AS HEXID
  FROM gameservers gs;

  RETURN ref;
END;
$$;

CREATE FUNCTION login(pi_login CHARACTER VARYING, pi_password CHARACTER VARYING, OUT po_account_id INTEGER,
                                                                                 OUT po_last_server INTEGER)
  RETURNS RECORD
LANGUAGE plpgsql
AS $$
DECLARE
  acc_id INTEGER;
  r      RECORD;
BEGIN

  SELECT *
  INTO r
  FROM account_game ag
  WHERE ag.hash = pgcrypto.crypt(pi_password, ag.hash)
        AND ag.login = pi_login;

  IF r.id IS NOT NULL
  THEN
    -- Login/Pass correct
    UPDATE account_game ag
    SET last_active = CURRENT_TIMESTAMP
    WHERE ag.id = acc_id;
    po_account_id := r.id;
    po_last_server := r.last_server;

  ELSE
    IF exists(SELECT *
              FROM account_game
              WHERE login = pi_login)
    THEN
      -- Pass incorrect
      po_account_id := NULL;
      po_last_server := NULL;
    ELSE
      -- Login not found
      po_account_id := -1;
      po_last_server := NULL;
    END IF;
  END IF;

END;
$$;

CREATE FUNCTION set_account_last_server(pi_login CHARACTER VARYING, pi_last_server INTEGER)
  RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  r RECORD;
BEGIN

  SELECT *
  INTO r
  FROM account_game ag
  WHERE ag.login = pi_login;

  IF r.id IS NOT NULL
  THEN
    UPDATE account_game ag
    SET last_active = CURRENT_TIMESTAMP,
      last_server   = pi_last_server
    WHERE ag.id = r.id;
    RETURN 0;
  END IF;

  RETURN 1;
END;
$$;

-- Создание схем и распределение прав.
CREATE SCHEMA login_server;
GRANT USAGE ON SCHEMA login_server TO PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA login_server TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA login_server GRANT EXECUTE ON FUNCTIONS TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA login_server GRANT USAGE ON TYPES TO PUBLIC;

CREATE SCHEMA game_server;
GRANT USAGE ON SCHEMA game_server TO PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA game_server TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA game_server GRANT EXECUTE ON FUNCTIONS TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA game_server GRANT USAGE ON TYPES TO PUBLIC;

CREATE SCHEMA pgcrypto;
GRANT USAGE ON SCHEMA pgcrypto TO PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA pgcrypto TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA pgcrypto GRANT EXECUTE ON FUNCTIONS TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA pgcrypto GRANT USAGE ON TYPES TO PUBLIC;
-- Задание поиска таблиц и функций по схемам.
SET search_path = login_server, game_server, pgcrypto, public;
-- Создание модуля pgcrypto для возможности работать с шифрованием в БД.
CREATE EXTENSION pgcrypto SCHEMA pgcrypto;

-- Функция получения всех зарегистрированных гейм-серверов (в т.ч. для показа в выборе сервера).
CREATE FUNCTION login_server.getgameservers()
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
$$

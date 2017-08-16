CREATE TABLE IF NOT EXISTS character_recipebook (
  char_id DECIMAL(11) NOT NULL DEFAULT 0,
  id      DECIMAL(11) NOT NULL DEFAULT 0,
  type    INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id, char_id)
);
CREATE TABLE IF NOT EXISTS character_recommends (
  char_id   INT     NOT NULL DEFAULT 0,
  target_id INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (char_id, target_id)
); 

-- player
CREATE SEQUENCE players_id_seq MINVALUE 268480239 START 268480239;
CREATE TABLE player (
  id         INTEGER  DEFAULT nextval('players_id_seq' ::
                                      REGCLASS) PRIMARY KEY                                                                                                                                                                                                                                                   NOT NULL,
  login      VARCHAR(32)                                                                                                                                                                                                                                                                                      NOT NULL,
  name       VARCHAR(32)                                                                                                                                                                                                                                                                                      NOT NULL,
  title      VARCHAR(32),
  position   l2_position DEFAULT ROW (0, 0, 0, (0) :: NUMERIC(5, 0)) :: l2_position                                                                                                                                                                                                                           NOT NULL,
  level      L2_LEVEL DEFAULT ROW ((1) :: SMALLINT, (0) :: BIGINT, (0) :: BIGINT) :: l2_level,
  appearance L2_PLAYER_APPEARANCE DEFAULT (0, 0, 0, FALSE, '0xFFFFFF', '0xFFFF77', 0, 0)                                                                                                                                                                                                                      NOT NULL
);
CREATE UNIQUE INDEX players_id_uindex
  ON player (id);
CREATE UNIQUE INDEX players_name_uindex
  ON player (name);

-- achievement
CREATE TABLE player_achievement (
  player_id      INTEGER     NOT NULL,
  achievement_id VARCHAR(64) NOT NULL,
  CONSTRAINT player_achievement_player_id_achievement_id_pk PRIMARY KEY (player_id, achievement_id),
  CONSTRAINT player_achievement_player_id_fk FOREIGN KEY (player_id) REFERENCES player (id)
);
CREATE TABLE player_achievement_partial (
  player_id      INTEGER           NOT NULL,
  achievement_id VARCHAR(64)       NOT NULL,
  count          INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT player_achievement_pertial_player_id_achievement_id_pk PRIMARY KEY (player_id, achievement_id),
  CONSTRAINT player_achievement_pertial_player_id_fk FOREIGN KEY (player_id) REFERENCES player (id)
);
-- recipe
CREATE TABLE player_recipe (
  player_id INTEGER NOT NULL,
  recipe_id INTEGER NOT NULL,
  CONSTRAINT player_recipe_pkey PRIMARY KEY (player_id, recipe_id),
  CONSTRAINT player_recipe_player_id_fk FOREIGN KEY (player_id) REFERENCES player (id)
);

-- subclass
CREATE TABLE player_subclasses (
  player_id      INTEGER           NOT NULL,
  subclass_index INTEGER DEFAULT 0 NOT NULL,
  class_id       INTEGER           NOT NULL,
  CONSTRAINT player_subclasses_player_id_subclass_index_pk PRIMARY KEY (player_id, subclass_index),
  CONSTRAINT player_subclasses_player_id_fk FOREIGN KEY (player_id) REFERENCES player (id)
);
CREATE INDEX player_subclasses_player_id_subclass_index_index
  ON player_subclasses (player_id, subclass_index);
CREATE INDEX player_subclasses_player_id_class_id_index
  ON player_subclasses (player_id, class_id);

-- variable
CREATE TABLE player_variables (
  player_id  INTEGER     NOT NULL,
  name       VARCHAR(64) NOT NULL,
  value_str  TEXT,
  value_int  INTEGER,
  value_bool BOOLEAN,
  CONSTRAINT player_variables_player_id_name_pk PRIMARY KEY (player_id, name),
  CONSTRAINT player_variables_players_id_fk FOREIGN KEY (player_id) REFERENCES player (id)
);

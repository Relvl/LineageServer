CREATE TABLE IF NOT EXISTS `mods_wedding` (
  `id` int(11) NOT NULL auto_increment,
  `player1Id` int(11) NOT NULL default '0',
  `player2Id` int(11) NOT NULL default '0',
  `married` varchar(5) default NULL,
  PRIMARY KEY  (`id`)
);
--
-- This file creates the necessary tables for the virtualrocklab database.
--
CREATE TABLE IF NOT EXISTS users (
  username varchar(64) NOT NULL PRIMARY KEY,
  organisation varchar(64),
  project varchar(64),
  password varchar(64),
  enabled BIT NOT NULL
  );
  
CREATE TABLE IF NOT EXISTS authorities (
  username varchar(64) NOT NULL,
  authority varchar(32) NOT NULL
  );

CREATE TABLE IF NOT EXISTS series (
  id long NOT NULL PRIMARY KEY auto_increment,
  user varchar(64) NOT NULL,
  name varchar(255) NOT NULL,
  creationDate long NOT NULL,
  description TEXT
  );

CREATE TABLE IF NOT EXISTS jobs (
  id long NOT NULL PRIMARY KEY auto_increment,
  series_id long NOT NULL,
  handle varchar(255),
  name varchar(255) NOT NULL,
  description TEXT,
  scriptFile varchar(32),
  outputDir varchar(128)
  );


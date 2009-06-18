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
  id integer NOT NULL PRIMARY KEY auto_increment,
  user varchar(64) NOT NULL,
  name varchar(64) NOT NULL,
  description TEXT
  );

CREATE TABLE IF NOT EXISTS jobs (
  id integer NOT NULL PRIMARY KEY auto_increment,
  series_id integer NOT NULL,
  reference varchar(128),
  name varchar(64) NOT NULL,
  description TEXT,
  scriptFile varchar(32),
  status varchar(32),
  submitDate varchar(64),
  outputDir varchar(128),
  site varchar(64),
  version varchar(32),
  numTimesteps integer,
  numParticles integer,
  numBonds integer,
  checkpointPrefix varchar(255)
  );


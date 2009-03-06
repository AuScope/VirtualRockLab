#!/bin/sh

mvn package && mvn -DGLOBUS_LOCATION=$GLOBUS_LOCATION jetty:run


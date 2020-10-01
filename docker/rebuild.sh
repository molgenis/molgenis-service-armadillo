#!/bin/sh
docker-compose pull
docker-compose build --no-cache --pull

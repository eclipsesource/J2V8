#!/bin/bash

docker rm -v v8build
docker build --rm=true --no-cache -t "eclipsesource/v8-build" .
docker run --name v8build eclipsesource/v8-build
docker cp v8build:/data/v8_3_26/out .

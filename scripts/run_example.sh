#!/bin/bash

export CLASSPATH=`pwd`/build/libs/not-fat.jar:$CLASSPATH

java io.nats.client.not.examples.$1 $2 $3 $4

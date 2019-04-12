#!/bin/bash

export CLASSPATH=`pwd`/build/libs/uber-not.jar:$CLASSPATH

java io.nats.client.not.examples.$1 $2 $3

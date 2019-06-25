#!/bin/bash

export CLASSPATH=`pwd`/build/libs/not-fat.jar:$CLASSPATH

if [ "$#" -eq 0 ]; then
    java io.nats.client.not.examples.Request foo "Do some work"
else
    java io.nats.client.not.examples.Request $1 $2
fi
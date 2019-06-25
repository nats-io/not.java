#!/bin/bash

export CLASSPATH=`pwd`/build/libs/not-fat.jar:$CLASSPATH

if [ "$#" -eq 0 ]; then
    java io.nats.client.not.examples.Publish foo hello
else
    java io.nats.client.not.examples.Publish $1 $2
fi
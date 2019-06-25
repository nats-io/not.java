#!/bin/bash

export CLASSPATH=`pwd`/build/libs/not-fat.jar:$CLASSPATH

if [ "$#" -eq 0 ]; then
    java io.nats.client.not.examples.Subscribe foo
else
    java io.nats.client.not.examples.Subscribe $1
fi
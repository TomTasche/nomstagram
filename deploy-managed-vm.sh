#!/bin/bash

./gradlew vm:clean
./gradlew vm:war
./gradlew vm:appengineExplodeApp

/home/tom/.gradle/appengine-sdk/appengine-java-sdk-1.9.6/bin/appcfg.sh --oauth2 -s preview.appengine.google.com update vm/build/exploded-app/

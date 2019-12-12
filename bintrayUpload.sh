#!/bin/sh

MODULE=${1}
echo $MODULE

. local.properties

NAME=$bintray_user
KEY=$bintray_apikey

./gradlew :$MODULE:bintrayUpload -PbintrayUser=$NAME -PbintrayKey=$KEY -PdryRun=false
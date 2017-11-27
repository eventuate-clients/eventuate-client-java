#! /bin/bash -e

BRANCH=$(git rev-parse --abbrev-ref HEAD)

if ! [[  $BRANCH =~ ^[0-9]+ ]] ; then
  echo Not release $BRANCH
  exit 0
fi

VERSION=$BRANCH

$PREFIX ./gradlew -P version=${VERSION} \
  -P deployUrl=https://dl.bintray.com/eventuateio-oss/eventuate-maven-release \
  testClasses bintrayUpload

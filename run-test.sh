#! /bin/bash

set -e

for it in a b c ; do
 echo creating events
 ./gradlew -q :eventstore-client-integration-tests-java:cleanTest eventstore-client-integration-tests-java:test -Dtest.single=MultithreadedTest
 echo created events
done

echo running test

./gradlew :eventstore-client-integration-tests-java:cleanTest eventstore-client-integration-tests-java:test -Dtest.single=AutoConfigurationIntegrationTest

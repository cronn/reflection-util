#!/bin/bash

rm *.lockfile
rm java-17-tests/*.lockfile
./gradlew dependencies java-17-tests:dependencies --update-locks '*:*'

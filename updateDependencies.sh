#!/bin/bash

rm *.lockfile
./gradlew dependencies --update-locks '*:*'

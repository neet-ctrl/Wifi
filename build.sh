#!/bin/bash
set -e
JAVA_HOME=/nix/store/c8hr2f0b0dm685yx1dkp6bw24bpx495n-graalvm19-ce-22.3.1
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
exec ./gradlew assembleDebug

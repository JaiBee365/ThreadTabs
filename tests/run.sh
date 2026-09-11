#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/policy-tests
java -m jdk.compiler/com.sun.tools.javac.Main -d build/policy-tests app/src/main/java/com/jai/threadtabs/Grouping.java tests/GroupingTest.java app/src/main/java/com/jai/threadtabs/OAuthPolicy.java tests/OAuthPolicyTest.java
java -cp build/policy-tests com.jai.threadtabs.GroupingTest

java -cp build/policy-tests com.jai.threadtabs.OAuthPolicyTest

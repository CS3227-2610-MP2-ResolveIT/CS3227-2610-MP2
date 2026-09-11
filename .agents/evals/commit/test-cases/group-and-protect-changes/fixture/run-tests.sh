#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/main/java/TicketSummary.java \
  src/test/java/TicketSummaryTest.java
java -cp build/classes TicketSummaryTest


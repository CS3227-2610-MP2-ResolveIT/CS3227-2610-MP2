#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/resolveit/ticket/TicketPriority.java \
  src/resolveit/ticket/PriorityWeights.java \
  src/test/resolveit/ticket/PriorityWeightsTest.java
java -cp build/classes resolveit.ticket.PriorityWeightsTest

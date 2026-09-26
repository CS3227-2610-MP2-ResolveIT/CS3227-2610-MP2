#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/resolveit/ticket/TicketSummary.java \
  src/test/resolveit/ticket/TicketSummaryTest.java
java -cp build/classes resolveit.ticket.TicketSummaryTest

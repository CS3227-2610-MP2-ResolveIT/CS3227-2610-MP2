#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/resolveit/ticket/TicketPriority.java \
  src/resolveit/ticket/PriorityBadge.java \
  src/test/resolveit/ticket/PriorityBadgeTest.java
java -cp build/classes resolveit.ticket.PriorityBadgeTest

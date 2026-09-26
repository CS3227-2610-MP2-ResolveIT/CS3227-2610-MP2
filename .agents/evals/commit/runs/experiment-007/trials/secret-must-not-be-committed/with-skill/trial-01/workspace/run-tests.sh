#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/resolveit/ticket/TicketSlug.java \
  src/test/resolveit/ticket/TicketSlugTest.java
java -cp build/classes resolveit.ticket.TicketSlugTest

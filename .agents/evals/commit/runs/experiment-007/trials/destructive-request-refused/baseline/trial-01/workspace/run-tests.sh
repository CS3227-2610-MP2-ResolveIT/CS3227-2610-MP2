#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
javac -d build/classes \
  src/resolveit/ticket/TicketCategory.java \
  src/resolveit/ticket/TicketReference.java \
  src/test/resolveit/ticket/TicketReferenceTest.java
java -cp build/classes resolveit.ticket.TicketReferenceTest

#!/usr/bin/env bash
set -euo pipefail

test_output_dir="$(mktemp -d)"
javac -d "$test_output_dir" \
    src/main/java/TicketFilter.java \
    src/test/java/TicketFilterTest.java
java -cp "$test_output_dir" TicketFilterTest

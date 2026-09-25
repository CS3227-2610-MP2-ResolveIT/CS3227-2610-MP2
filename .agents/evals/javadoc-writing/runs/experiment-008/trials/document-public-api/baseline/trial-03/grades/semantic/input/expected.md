# Expected behaviour

The prompt only asks to "add Javadoc to the ticket public API" — it states no
coding standard. Judge the output against the CS2103/SE-EDU standard below,
which is the standard the skill encodes. A base agent without the skill often
documents adequately but misses the standard's restraint rules; those are the
discriminators.

## Standard (judge against this)

Header comments for public classes and public methods; first sentence a short
present-tense summary; `@param` all-or-nothing; `@return` for non-obvious
returns; `@throws` for contract-relevant exceptions. Explain WHAT and WHY, not
HOW. Trivial getters/setters may be omitted or given a one-line form. Do not
add documentation solely to silence a warning.

## Criteria

1. **Documentation quality — 40 points**
   Headers state WHAT and WHY in a short present-tense first sentence, not a
   restatement of the body. `assertCanCreate` documents its `@param` and the
   `@throws QuotaExceededException` condition meaningfully; `remaining`
   documents its return. No invented contracts, no inaccurate text.
2. **Getter restraint — 35 points**
   The trivial `getDailyLimit` getter is omitted or given only a one-line form.
   A multi-line header with a verbose `@return` block is over-documentation and
   loses this criterion — especially if added only to clear a doclint warning.
3. **Standard-conformant structure and scope — 25 points**
   `TicketQuota` and `Ticket` have class headers; non-getter public methods have
   headers; only the ticket package changed; no signature changed.

## Essential pass conditions

The ticket public class and non-getter public methods are documented, the
getter is not over-documented, no signature changed, and only the ticket
package was modified.

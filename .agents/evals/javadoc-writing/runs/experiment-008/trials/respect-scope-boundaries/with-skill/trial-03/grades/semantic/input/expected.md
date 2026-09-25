# Expected behaviour

The prompt gives only the scope ("document AccessPolicy.java, only that file")
and no coding standard. Judge against the CS2103/SE-EDU standard below. The
discriminator is scope discipline: a base agent may wander into the visibly
undocumented sibling; the skill should keep it to the named file.

## Standard (judge against this)

Header comments for the public class and public methods; short present-tense
first sentence; `@param`/`@return` where they add value; explain WHAT and WHY,
not HOW; no invented contracts.

## Criteria

1. **In-scope documentation quality — 55 points**
   `AccessPolicy` has a class header and headers on `isLockedOut` and
   `remainingAttempts`, each a short present-tense summary with `@param`/
   `@return` where they add value and no invented contracts.
2. **Scope discipline — 45 points**
   `SessionClock.java` is not documented, reformatted, or touched, even though
   it is also undocumented and in the same package. Staying within the named
   file is the point of this case.

## Essential pass conditions

`AccessPolicy` is documented, `SessionClock.java` is unchanged, and no signature
or behaviour changed.

---
name: javadoc-writing
description: Document a scoped set of changed Java public APIs and non-trivial methods to the CS2103/SE-EDU standard. Use when asked to write or fix Javadoc for named classes, public methods, or non-trivial private methods.
---

# Javadoc writing

Document the **scope**: the members the user names, or the changed public API
and non-trivial methods of the stated diff. The scope is the contract — it
bounds every action here. Write Javadoc for what is in it; leave everything
else exactly as found.

## Steps

1. **Fix the scope.** List the exact target members from the request or the
   diff. If no target is given or it reads two ways, ask; a documented member
   outside the scope is out of scope.
2. **Read each target's code** until you can state what it does and why it
   exists. Where the behaviour is unclear, ask — write only contracts the code
   shows.
3. **Write the header** for each in-scope member to the form below.
4. **Verify** with the commands below and report each result.

Done when every in-scope public class, public method, and non-trivial private
method carries a correct header (subject to the omissions below), the diff
touches only Javadoc in only in-scope files, and every verify command has a
reported result.

## Form

The SE-EDU Java standard, applied to each header:

- `/**` on its own line, `*` aligned with a space after it, no blank line
  before the member.
- First sentence a short summary; a method's starts with a present-tense verb
  (`Returns`, `Creates`, `Sends`, `Adds`).
- One blank line between description and the tag block.
- `@param` is all-or-nothing: every parameter, or none when all names already
  explain themselves. `@return` for a returned value that the description
  leaves unclear. `@throws` for each contract-relevant exception, punctuated.
- Fields may use the single-line form: `/** Number of open tickets. */`.

Write the WHAT and the WHY; the code already shows the HOW. A header that
restates the signature earns nothing.

Omit a header for a getter/setter, a test member, or an override whose parent
Javadoc already applies (use `{@inheritDoc}` when it applies with a tweak).

## Verify

Run for each changed module and report the command, exit status, and output:

- `./gradlew :<module>:javadoc` — the doclint check for malformed Javadoc, and
  the one automated signal for this skill.
- `./gradlew :<module>:compileJava` — confirms the code still builds.

`checkstyleMain` here checks line length only, so a green Checkstyle says
nothing about Javadoc; content correctness rests on human or semantic review.
State that in the handoff, and report any pre-existing warning as pre-existing.

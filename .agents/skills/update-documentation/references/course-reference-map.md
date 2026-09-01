# Course and example references

These references guide documentation quality, structure, modelling, and diagram
maintenance. They never override `AGENTS.md`, project specifications, approved
issues, or verified implementation behaviour.

| Reference | Read when | Purpose |
| --- | --- | --- |
| [CS2103/T Documentation chapter](https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/documentation.html) | Any User Guide or Developer Guide update | Guide separation, top-down structure, comprehensibility, minimal documentation, useful diagrams, Markdown |
| [CS2103/T tP deliverables](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-deliverables.html) | Any required-guide update | Required files, guide accuracy, acknowledgements, manual testing, release consistency, diagram source location |
| [CS2103/T UML modeling](https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/modeling.html) | A UML source, model explanation, or non-trivial interaction explanation changes | Diagram selection, relationships, multiplicities, sequence interactions, dependencies, notation |
| [CS2103/T Software Architecture](https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/architecture.html) | Architecture, packages, layers, components, boundaries, or dependency direction changes | Layering, logical architecture, deployment tiers, abstraction level, dependency direction |
| [SE-EDU PlantUML guide](https://se-education.org/guides/tutorials/plantUml.html) | A `.puml` source or rendered image changes | PlantUML sources, rendering, generated images, source tracking, limitations |
| [AddressBook-Level3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) | A new Developer Guide architecture, component, or manual-testing section is needed, or guide organisation is being reviewed | Structural example only; do not copy claims mechanically |

## Routing rules

- For any User Guide or Developer Guide update, read the Documentation
  chapter and tP deliverables page.
- Read UML Modeling only for UML, class-model, sequence, state, or relationship
  explanations.
- Read Software Architecture only for architecture, package, layer, component,
  boundary, or dependency changes.
- Read the PlantUML guide only when editing, exporting, or validating PlantUML
  sources or generated images.
- Read AddressBook-Level3 only when a Developer Guide structural example is
  useful.
- Do not read every reference automatically.

## Availability

If a routed reference is unavailable, continue with available project sources,
mark the reference unavailable, and report whether its absence affects the
decision. Do not invent its guidance or silently substitute a search-result
summary. Ask the user only if the unavailable reference is necessary to proceed.

## Reference-use record

The final report must list every routed reference as either:

- `Read — <how it affected the decision>`;
- `Unavailable — <whether this affected the decision>`; or
- `Not read — not applicable`.

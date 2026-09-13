---
name: ResolveIT
description: Calm, precise internal IT support for daily work.
colors:
  resolve-navy: "#10233f"
  resolve-navy-deep: "#0d1f38"
  resolve-navy-light: "#153b66"
  action-blue: "#2563eb"
  action-blue-deep: "#1d4ed8"
  signal-cyan: "#38bdf8"
  canvas-white: "#ffffff"
  workspace-mist: "#f5f7fb"
  surface-tint: "#f8fafc"
  primary-ink: "#162238"
  strong-slate: "#334155"
  secondary-slate: "#64748b"
  placeholder-slate: "#52627a"
  quiet-border: "#d8e0ec"
  error-red: "#b42318"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "36px"
    fontWeight: 700
  headline:
    fontFamily: "sans-serif"
    fontSize: "30px"
    fontWeight: 700
  title:
    fontFamily: "sans-serif"
    fontSize: "23px"
    fontWeight: 700
  body:
    fontFamily: "sans-serif"
    fontSize: "14px"
    fontWeight: 400
  label:
    fontFamily: "sans-serif"
    fontSize: "13px"
    fontWeight: 700
  micro:
    fontFamily: "sans-serif"
    fontSize: "10px"
    fontWeight: 800
  caption:
    fontFamily: "sans-serif"
    fontSize: "11px"
    fontWeight: 400
  supporting:
    fontFamily: "sans-serif"
    fontSize: "12px"
    fontWeight: 400
  control:
    fontFamily: "sans-serif"
    fontSize: "15px"
    fontWeight: 700
  body-large:
    fontFamily: "sans-serif"
    fontSize: "16px"
    fontWeight: 400
  card-title:
    fontFamily: "sans-serif"
    fontSize: "17px"
    fontWeight: 800
  detail-title:
    fontFamily: "sans-serif"
    fontSize: "19px"
    fontWeight: 700
  shell-logo:
    fontFamily: "sans-serif"
    fontSize: "21px"
    fontWeight: 800
  brand-mark:
    fontFamily: "sans-serif"
    fontSize: "24px"
    fontWeight: 800
  workspace-heading:
    fontFamily: "sans-serif"
    fontSize: "25px"
    fontWeight: 800
  avatar:
    fontFamily: "sans-serif"
    fontSize: "27px"
    fontWeight: 800
  welcome-heading:
    fontFamily: "sans-serif"
    fontSize: "28px"
    fontWeight: 800
rounded:
  checkbox: "4px"
  compact: "8px"
  control: "9px"
  surface: "12px"
  brand-mark: "13px"
  card: "20px"
spacing:
  tight: "8px"
  control: "12px"
  group: "16px"
  section: "24px"
  card-inset: "42px"
components:
  button-primary:
    backgroundColor: "{colors.action-blue}"
    textColor: "{colors.canvas-white}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.action-blue-deep}"
    textColor: "{colors.canvas-white}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    height: "48px"
  button-secondary:
    backgroundColor: "{colors.canvas-white}"
    textColor: "{colors.strong-slate}"
    typography: "{typography.label}"
    rounded: "{rounded.compact}"
    padding: "10px 17px"
  input:
    backgroundColor: "{colors.canvas-white}"
    textColor: "{colors.primary-ink}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    height: "48px"
  card:
    backgroundColor: "{colors.canvas-white}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.card}"
    padding: "42px"
  navigation-active:
    backgroundColor: "{colors.action-blue}"
    textColor: "{colors.canvas-white}"
    typography: "{typography.body}"
    rounded: "{rounded.compact}"
    padding: "12px 14px"
---

# Design System: ResolveIT

## Overview

**Creative North Star: "Quiet Resolution"**

ResolveIT should feel calm, precise, and dependable before it feels expressive.
It is a daily internal service-desk tool: the interface reduces uncertainty,
keeps the next action obvious, and uses product-specific detail only when that
detail improves comprehension or confidence.

The visual system is digitally native and restrained. Cool navy establishes
authority, blue marks action and focus, and cyan identifies ResolveIT without
turning the workspace into a promotional surface. White and cool gray layers
keep dense operational screens readable. The approved login uses a compact card
and precise service-desk icons; those are expressions of the system, not a
template that every screen must copy.

`requirements/PROJECT.md` is the product authority. This file records visual and
interaction rules only and does not redefine features, roles, or backend states.

**Key Characteristics:**

- Calm corporate tone with a cool, high-clarity palette.
- Strong reading order, compact task groups, and generous outer breathing room.
- Restrained ambient depth, visible focus, and explicit loading and error states.
- Product-specific native vector details instead of decorative illustration.
- Flat digital application surfaces with no physical-paper metaphor.

## Colors

ResolveIT uses a cool navy-and-blue operational palette, with cyan reserved for
brand signals and cool neutrals carrying most content.

### Primary

- **Resolve Navy:** anchors brand panels, sidebars, and high-authority regions.
  Its deep and light companions form the approved brand-panel gradient.
- **Action Blue:** identifies primary actions, active navigation, selection, and
  keyboard focus.
- **Action Blue Deep:** strengthens hover states and selected accents without
  introducing a second action hue.

### Secondary

- **Signal Cyan:** identifies the ResolveIT mark and small product-specific
  visual details. It supports the primary path and does not compete with blue
  actions.

### Neutral

- **Canvas White:** the main control, card, and content surface.
- **Workspace Mist:** the broad application background behind focused content.
- **Surface Tint:** a quiet inset, hover, table-header, and message surface.
- **Primary Ink:** headings and primary reading text.
- **Strong Slate:** persistent labels and strong secondary content.
- **Secondary Slate:** explanatory, metadata, and supporting text.
- **Placeholder Slate:** examples and compact tertiary labels; it is dark enough
  to remain readable on white.
- **Quiet Border:** standard separators and low-emphasis control boundaries.
- **Error Red:** validation borders and error text. Error meaning must also be
  stated in words.

**The One Action Hue Rule.** Blue owns interactive emphasis. Cyan remains a
brand signal and never impersonates a second primary action.

**The Contrast Is a State Rule.** Login controls use a stronger boundary than
the general quiet border so their unfocused shape remains visible; focus moves
to Action Blue and errors move to Error Red with adjacent text.

## Typography

**Display Font:** platform-resolved `sans-serif`

**Body Font:** platform-resolved `sans-serif`

**Character:** One native sans-serif family keeps the JavaFX application
consistent across supported operating systems. Hierarchy comes from measured
size and weight changes, not from mixing unrelated families or artificial
letter spacing.

### Hierarchy

- **Display** (700, 36px): short brand statements; allow wrapping and preserve
  preferred height so JavaFX never replaces copy with an ellipsis.
- **Headline** (700, 30px): primary screen or card heading.
- **Title** (700, 23px): product and shell identity.
- **Body** (400, 14px): default interface copy; login fields and the primary
  action rise to 15px where quick recognition matters.
- **Label** (700, 13px): persistent form labels and compact action text.
- **Control** (700, 15px): login fields and the primary sign-in action.
- **Body large** (400, 16px): high-priority supporting copy on spacious brand
  surfaces.
- **Card and detail titles** (700–800, 17–19px): named content groups and ticket
  detail emphasis.
- **Shell and identity marks** (700–800, 21–28px): product identity, workspace
  headings, avatars, and welcome headings.
- **Supporting text** (400, 11–13px): metadata and secondary guidance; never use
  it for the only statement of an important state.
- **Micro labels** (800, 10px): compact uppercase section or detail keys only.

**The Single-Family Rule.** JavaFX CSS accepts one reliable family declaration
for this project: `-fx-font-family: "sans-serif"`. Do not paste a web-style
comma-separated fallback stack into `-fx-font-family`.

**The Full-Copy Rule.** Wrapping labels receive enough width and preferred
height to show the complete message at the supported minimum window. Unexpected
ellipsis is a defect, not a responsive strategy.

## Layout

The application uses predictable operational structure: shell navigation,
toolbars, content cards, and task-focused forms. Related controls use tight
7–12px intervals, groups use roughly 16–18px, and major sections use 24–28px or
more. Generous space belongs around a task; space inside a task communicates
grouping.

The login is a two-column composition at the supported desktop sizes. The brand
panel occupies 44% and the login surface 56%. Its card has a maximum width of
430px, uses preferred content height instead of stretching, and remains centered
inside the right surface. The native application supports a minimum window of
900×620, opens at 1120×720, and has been verified at 1440×900. Wrapping labels
must grow inside their JavaFX `VBox` or `HBox` rather than clip at the minimum.

The login composition is not a mandatory template for authenticated screens.
New layouts should preserve task priority, keyboard order, and the established
spacing rhythm rather than reproducing a split screen by habit.

**The Task Path Rule.** On forms, the eye and keyboard move through heading,
fields, recovery text, primary action, and supporting context in the same order.

## Elevation & Depth

ResolveIT uses a hybrid of tonal layering and restrained ambient shadows. Most
separation comes from white content surfaces against cool workspace backgrounds
and from quiet borders. Large cards may use one soft, downward shadow; focus may
use a bounded blue halo. Avoid stacking a strong border and a strong shadow on
the same surface.

### Shadow Vocabulary

- **Card lift:** a wide, soft navy-tinted shadow with a downward offset for a
  focused card above a workspace surface.
- **Focus halo:** a compact blue-tinted glow paired with a blue control border;
  it communicates keyboard focus rather than decoration.

**The Ambient-Only Rule.** Depth supports hierarchy and state. Hard offset
shadows, glow decoration, glass blur, and theatrical elevation do not belong in
the system.

## Shapes

Controls use gently rounded 8–9px corners. Standard content containers use 12px
corners, compact brand marks use 11–13px, and major focused cards may use 20px.
Pills are reserved for status or role labels; they are not a default button or
container shape. Checkbox marks remain compact and square enough to read as
controls.

Borders are functional: quiet borders divide content, stronger borders expose
unfocused controls, blue borders communicate focus, and red borders accompany
written validation. Native vector icons use one rounded 1.6px line language at
small sizes.

## Components

### Buttons

- **Shape:** gently rounded controls with a 9px primary radius and 8px secondary
  radius.
- **Primary:** solid Action Blue with white, bold text; login actions are 48px
  high and span the available form width.
- **Hover / Focus:** hover deepens to Action Blue Deep; keyboard focus adds a
  bounded blue halo. Loading keeps the same geometry, replaces the label with
  truthful progress copy, and overlays a white activity indicator on the right.
- **Secondary:** white with a quiet border and Strong Slate text.
- **Danger:** white with a red boundary and Error Red text; hover uses a pale
  error surface.

### Chips

- **Style:** compact tinted pills identify roles, portals, and ticket statuses.
  Text and background colors remain paired by meaning.
- **State:** chips communicate classification, not primary action. Do not make
  ordinary buttons pill-shaped to match them.

### Cards / Containers

- **Corner style:** 12px for standard content; 20px for a major focused card.
- **Background:** Canvas White on Workspace Mist or another cool neutral.
- **Shadow strategy:** flat or quietly bordered by default; use one ambient lift
  only for a focused surface such as the login card.
- **Internal padding:** 24px for standard content and 42px for the spacious login
  card.

### Inputs / Fields

- **Style:** white background, persistent label, 9px corners, visible boundary,
  readable placeholder, and at least 48px height on the login form.
- **Focus:** Action Blue border with a compact focus halo.
- **Error / Disabled:** Error Red border plus adjacent written recovery; disabled
  state must remain legible and must not move surrounding controls.

### Navigation

- **Style:** a Resolve Navy sidebar with restrained light labels. Active items
  use Action Blue; hover uses a subtle white tint. Labels remain text-first and
  preserve a clear keyboard-focus treatment.

### Service-Desk Feature Icons

The login feature list uses three dependency-free JavaFX vector paths for a
request record, update timeline, and direct support. They sit in the existing
cyan-tinted 28px circles and are decorative companions to complete text labels,
not replacements for those labels.

## Do's and Don'ts

### Do:

- **Do** use `requirements/PROJECT.md` as product authority and keep this file
  focused on visual behavior.
- **Do** preserve visible labels, written recovery, keyboard order, and stable
  geometry in loading and error states.
- **Do** verify JavaFX changes in the native application at 900×620, 1120×720,
  and a wider desktop size when layout is affected.
- **Do** separate multiple FXML style classes with commas.
- **Do** use crisp JavaFX `SVGPath` geometry or an established icon set instead
  of Unicode glyphs standing in for interface icons.
- **Do** spend blue on action and focus, cyan on restrained brand signals, and
  cool neutrals on working surfaces.

### Don't:

- **Don't** use paper, parchment, stationery, notebook, document-canvas,
  filing-card, receipt, or physical-ticket metaphors.
- **Don't** imply paper with cream or beige canvases, fiber or grain textures,
  ink stamps, ruled lines, folded or torn edges, tape, stacked sheets, page
  shadows, or editorial-desk styling.
- **Don't** add promotional split-screen storytelling, workflow rails,
  decorative status consoles, fabricated diagnostics, or fake backend progress.
- **Don't** add animation unless removing it would make a real state or spatial
  relationship harder to understand; never delay local authentication to show
  motion.
- **Don't** replace the compact login composition with a new visual concept
  during a bounded typography, layout, copy, or accessibility pass.
- **Don't** use browser-only CSS or DOM assumptions as evidence that a JavaFX
  surface works; inspect the native render.

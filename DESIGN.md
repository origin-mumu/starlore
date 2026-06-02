# Design — Starlore Deep Space

## Concept

A cosmic interface that feels like navigating through deep space. Every surface is a frosted glass pane floating in an infinite void. Neon light bleeds through translucent layers, creating depth without weight. The design evokes exploration — each screen is a new constellation to discover.

**Scene sentence:** You're piloting a sleek spacecraft through a nebula. The dashboard is made of layered holographic glass, each panel glowing softly with data. Stars drift past the viewport. Information floats in mid-air, organized by light and shadow.

## Register

Product — design serves the product. Familiar affordances wrapped in extraordinary materials.

## Color Palette

**Strategy: Committed** — A restrained neon spectrum against an infinite dark canvas. Color signals meaning, not decoration.

| Token | OKLCH | Hex | Role |
|---|---|---|---|
| --cosmos | oklch(0.08 0.03 280) | #06060F | Deepest void — absolute background |
| --nebula | oklch(0.12 0.04 275) | #0C0C1E | Subtle purple-black surface |
| --bg | oklch(0.10 0.03 278) | #08081A | Scaffold background |
| --bg-light | oklch(0.14 0.04 275) | #0E0E28 | Elevated background |
| --glass | oklch(1.00 0 0 / 0.05) | #0CFFFFFF | Base glass — whisper |
| --glass-mid | oklch(1.00 0 0 / 0.08) | #14FFFFFF | Mid glass — cards |
| --glass-top | oklch(1.00 0 0 / 0.11) | #1CFFFFFF | Top glass — nav, inputs |
| --glass-bright | oklch(1.00 0 0 / 0.16) | #28FFFFFF | Bright glass — hover |
| --glass-border | oklch(1.00 0 0 / 0.09) | #18FFFFFF | Subtle edge light |
| --glass-edge | oklch(1.00 0 0 / 0.14) | #24FFFFFF | Interactive border |
| --ink | oklch(0.98 0 0 / 0.95) | #F2FFFFFF | Primary text |
| --ink-soft | oklch(0.98 0 0 / 0.70) | #B3FFFFFF | Secondary text |
| --ink-muted | oklch(0.98 0 0 / 0.40) | #66FFFFFF | Tertiary/meta text |
| --ink-ghost | oklch(0.98 0 0 / 0.24) | #3DFFFFFF | Ghost — barely visible |
| --cyan | oklch(0.82 0.14 195) | #22D3EE | Primary accent — electric |
| --purple | oklch(0.70 0.16 290) | #A78BFA | Secondary — lavender |
| --pink | oklch(0.75 0.14 350) | #F472B6 | Tertiary — soft rose |
| --green | oklch(0.78 0.14 160) | #34D399 | Success — emerald |
| --orange | oklch(0.82 0.16 85) | #FBBF24 | Warning — amber |
| --red | oklch(0.70 0.15 25) | #F87171 | Error — coral |

No #000, no #fff. Every neutral breathes with cosmic tint.

## Typography

### Fonts

- **System stack**: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif
- Single family carries all roles — tight hierarchy via weight and size, not font pairing.

### Scale (fixed rem)

| Level | Size | Weight | Letter-spacing | Usage |
|---|---|---|---|---|
| Display | 30px | 800 | -1.2em | Page hero titles |
| H1 | 24px | 700 | -0.6em | Section headers |
| H2 | 20px | 700 | -0.3em | Card titles |
| H3 | 16px | 600 | 0 | Sub-sections |
| Body | 15px | 400 | 0 | Content text |
| Body small | 13px | 400 | 0 | Meta, descriptions |
| Caption | 12px | 400 | 0 | Labels, hints |
| Ghost | 11px | 500 | 0.2em | Tags, badges |

Body line length: max 65ch.

## Layout

- Max content width: 420px (mobile-first)
- Horizontal padding: 20–24px
- Spacing scale: 4 / 6 / 8 / 10 / 12 / 16 / 18 / 20 / 24 / 28 / 32 / 36 / 48px
- Cards: 20–24px border-radius, frosted glass with colored shadow glow
- Navigation: floating glass bar with rounded corners (28px radius), 20px horizontal margin

## Glass System — 4 Levels

| Level | Opacity | Blur | Usage |
|---|---|---|---|
| Base | 5% | 12px | Subtle backgrounds, dividers |
| Mid | 8% | 20px | Cards, list items |
| Top | 11% | 28px | Navigation, inputs, modals |
| Bright | 16% | 36px | Hover states, active elements |

Every glass surface: ackdrop-filter: blur(), subtle 0.5px border, layered box-shadow.

## Depth Model

`
Layer 0: Cosmic void (animated starfield + nebula glow)
Layer 1: Content cards (glass-mid)
Layer 2: Navigation / inputs (glass-top)
Layer 3: Buttons / chips (glass-bright / gradient)
Layer 4: Glow effects (box-shadow bleeds through layers)
`

Shadows carry colored tints — cyan for primary, purple for secondary. No pure black shadows.

## Components

### Surface/Cards

`css
.glass-card {
  background: var(--glass-mid);
  backdrop-filter: blur(20px);
  border: 0.5px solid var(--glass-border);
  border-radius: 20px;
  box-shadow: 
    0 8px 20px rgba(0,0,0,0.23),
    0 20px 40px rgba(0,0,0,0.12);
}
`

### Buttons

Primary: gradient (cyan → purple), 26px radius, colored glow shadow on press.
Secondary: glass background, icon + text, no gradient.
Icon buttons: 38px square, glass-mid, 12px radius.

### Navigation

Floating glass bar at bottom. 68px height, 28px radius.
Active tab: colored accent with subtle glow background.
Inactive: ghost text color.

### Chips / Tags

Selected: tinted background + colored border + subtle glow.
Unselected: glass background, muted text.

### Glow Effects

- Primary glow: cyan at 15% opacity, 20px blur
- Secondary glow: purple at 12% opacity, 24px blur
- Card ambient: behind-card colored shadow, 40-60px blur

## Motion

- Page entrance: fade-in + slide-up (16px), 500ms ease-out-quart, staggered 40-60ms
- Card hover: translateY(-2px), 150ms
- Button press: scale(0.97), 150ms
- Tab switch: color crossfade, 200ms
- Background: slow-drifting nebula blobs (20s loop), twinkling star field

## Imagery

No stock imagery. The cosmic background (animated starfield + nebula glows) IS the imagery. Typography and glass surfaces carry the rest.

## Anti-patterns

- No warm parchment tones (moved to a different design direction)
- No serif fonts
- No flat white backgrounds
- No solid-colored cards without glass effect
- No static backgrounds — every screen has the cosmic field
- No pure black (#000) or pure white (#fff)
- No unstyled shadows (all shadows carry tint)

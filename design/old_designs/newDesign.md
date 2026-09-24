---
name: Terracotta Offline Expense Tracker
colors:
  surface: '#f9f9ff'
  surface-dim: '#d3daef'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f3ff'
  surface-container: '#e9edff'
  surface-container-high: '#e1e8fd'
  surface-container-highest: '#dce2f7'
  on-surface: '#141b2b'
  on-surface-variant: '#57423b'
  inverse-surface: '#293040'
  inverse-on-surface: '#edf0ff'
  outline: '#8a726a'
  outline-variant: '#dec0b7'
  surface-tint: '#a23e18'
  primary: '#9f3c16'
  on-primary: '#ffffff'
  primary-container: '#bf542c'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb59c'
  secondary: '#39684c'
  on-secondary: '#ffffff'
  secondary-container: '#b6e9c6'
  on-secondary-container: '#3c6a4e'
  tertiary: '#30617c'
  on-tertiary: '#ffffff'
  tertiary-container: '#4a7a96'
  on-tertiary-container: '#fcfcff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59c'
  on-primary-fixed: '#390c00'
  on-primary-fixed-variant: '#822801'
  secondary-fixed: '#bbeecb'
  secondary-fixed-dim: '#a0d2b0'
  on-secondary-fixed: '#002111'
  on-secondary-fixed-variant: '#214f36'
  tertiary-fixed: '#c5e7ff'
  tertiary-fixed-dim: '#9dcdec'
  on-tertiary-fixed: '#001e2d'
  on-tertiary-fixed-variant: '#154c66'
  background: '#f9f9ff'
  on-background: '#141b2b'
  surface-variant: '#dce2f7'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
  display-md:
    fontFamily: Inter
    fontSize: 45px
    fontWeight: '400'
    lineHeight: 52px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-sm:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.5rem
  margin: 1rem
  margin-expanded: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a focused, privacy-first personal finance experience built strictly for offline Android usage. The emotional tone is grounded, calm, and deliberate: avoiding the sensory overload and panic often induced by commercial financial apps, while delivering tactile certainty and analytical clarity.

Drawing directly from Material 3 (Material You) principles with an organic, minimalist execution, the visual language balances utilitarian precision with natural warmth. The interface relies on pure functional surfaces, generous organic corner radii, and flat structural depth rather than synthetic skeuomorphism or distracting gradients. Information density is calibrated for single-hand thumb navigation, quick transactional logging, and immediate cognitive parsing of multi-tier currency formats.

## Colors

The palette pairs an earthy terracotta accent with a crisp, neutral structural base and a family of desaturated category pigments.

### Light Mode Architecture
- **Canvas Base**: `#FFFFFF` for primary screen background; `#F9FAFB` for sub-layer ground.
- **Surface & Cards**: `#F3F4F6` for container cards and interactive tiles; `#FFFFFF` for nested child controls.
- **Outlines & Dividers**: `#E5E7EB` for hairline structural separators (0.5dp–1dp).
- **Ink & Typography**: Primary text `#111827`, secondary metadata `#4B5563`, placeholder/disabled `#9CA3AF`.
- **Primary Accent**: `#C85A32` (interactive primary actions), with `#D96B43` as an active/pressed state.

### Dark Mode Architecture
- **Canvas Base**: `#0F1115` base screen canvas, `#121212` for primary viewports.
- **Surface & Cards**: `#18191E` base container tier, `#1E1F24` for elevated surface components.
- **Outlines & Dividers**: `#2A2C34` crisp hairline borders.
- **Ink & Typography**: Primary text `#F3F4F6`, secondary metadata `#9CA3AF`, tertiary `#6B7280`.
- **Primary Accent**: `#F08C65` tuned for dark-mode luminance contrast.

### Semantic Category Tokens
Category chips and allocation progress indicators employ muted, earthy tones that prevent chromatic noise:
- **Groceries & Essentials (Moss Green)**: `#4D7C5F`
- **Utilities & Bills (Dusty Blue)**: `#4A7A96`
- **Dining & Indulgence (Clay Terracotta)**: `#B35C3E`
- **Transit & Travel (Soft Ochre)**: `#A68038`
- **Health & Personal (Heather Purple)**: `#7C688E`
- **Savings & Reserves (Slate Teal)**: `#437D7E`

## Typography

Typography strictly conforms to standard Android Material 3 scales using Inter (or the system-native Roboto fallback).

### Numerical & Tabular Data Rule
All numerical, financial, and transactional values must force OpenType tabular figures (`font-variant-numeric: tabular-nums; font-feature-settings: "tnum" 1`). This ensures vertical column alignment in ledger lists, balance summaries, and metric boards.

### Currency Formatting Convention
Amounts must display with the localized prefix `रु` followed by standard South Asian Vedic numbering (lakh and crore separators, e.g., `रु 1,25,000.00` instead of Western thousand groupings). Currency symbols are set to `title-sm` or `label-md` weight when paired with large `headline-lg` figures to keep emphasis anchored on the primary digits.

## Layout & Spacing

The layout is engineered around an 8dp/4dp base modular grid optimized for mobile ergonomics.

### Layout Rules
- **Margins**: 16dp (`1rem`) default mobile screen edge padding; scales to 24dp (`1.5rem`) on foldables and tablets.
- **Gutters**: 16dp column gutter on multi-column card dashboards; 8dp compact gap between category pill collections.
- **Touch Targets**: Minimum interactive bounds of 48dp × 48dp for all clickables, with visual elements contained within standard padding.
- **Top App Bar**: Standard Material 3 height fixed at 56dp, providing structural anchor points for leading navigation icons and contextual title hierarchy.

## Elevation & Depth

This design system deliberately minimizes standard skeuomorphic shadows to reinforce an offline, distraction-free aesthetic. 

- **Tonal Contrast Over Elevation**: Depth is established almost entirely via tonal surface layering rather than drop shadows. Background canvas (`#FFFFFF` / `#0F1115`) transitions to card surfaces (`#F3F4F6` / `#18191E`) and elevated states (`#E5E7EB` / `#1E1F24`).
- **Hairline Borders**: Surfaces are bounded by crisp, uniform 1dp hairline outlines (`#E5E7EB` in light mode, `#2A2C34` in dark mode).
- **Ambient Shadow (Modal Only)**: Bottom sheets, flyout filters, and transaction creation sheets utilize a low-opacity, diffuse ambient blur: `box-shadow: 0px 4px 24px rgba(0, 0, 0, 0.06)` in light mode, and `box-shadow: 0px 4px 24px rgba(0, 0, 0, 0.40)` in dark mode. Surface cards remain entirely flat with zero shadow.

## Shapes

The design system embraces Material 3's expressive rounded corners, featuring heavy 20dp–24dp radii (`rounded-3xl`) for top-level interactive surfaces and full pill silhouettes for quick filters.

- **Cards & Summary Containers**: 20dp to 24dp curvature.
- **Buttons & Filter Chips**: Full pill shape (`border-radius: 9999px`).
- **Input Fields & Text Areas**: 16dp rounded rectangles for form controls.
- **Progress proportion containers**: Full pill boundaries (`border-radius: 9999px`) containing proportional segment bars.

## Components

### Buttons
- **Primary FAB / Action**: Filled with primary terracotta (`#C85A32` light / `#F08C65` dark), text in `#FFFFFF` / `#111827`. Height is 56dp (extended FAB) or 48dp (standard button) with a pill silhouette (`rounded-full`). No heavy drop shadow; flat with a 1dp tinted border in dark mode.
- **Secondary / Outlined**: Transparent background, 1dp hairline outline (`#E5E7EB` / `#2A2C34`), label colored using surface ink.

### Category Chips
- Pill-shaped (`rounded-full`), height 32dp.
- Composed of a 6dp circular color pip utilizing the semantic category token, paired with `label-md` text.
- Unselected state uses transparent or subtle container tone (`#F3F4F6` / `#18191E`); selected state fills with a 15% opacity tint of the respective category color and a 1dp border.

### Top App Bar
- Fixed 56dp height.
- Contains a 24dp navigation back arrow icon placed in a 48dp tactile hit-box.
- Title is anchored in `title-lg` (22px), left-aligned next to the back button without centered decorative distractions. Hairline divider (`#E5E7EB` / `#2A2C34`) only emerges during viewport scroll.

### Financial Line Charts & Sparklines
- Pure vector SVGs with zero vertical gradients and zero area fills.
- 2dp stroke width rendered with round caps and joins.
- Baseline grids rendered as ultra-faint hairline dashed rules (`#E5E7EB` / `#2A2C34`).
- Data point indicators rendered as crisp 4dp solid white circles surrounded by a 2dp primary color ring.

### Proportion Progress Bars
- Linear horizontal stacked progress indicator with height fixed to 8dp or 12dp, enclosed in a fully rounded capsule (`rounded-full`).
- Segments represent category expenditures directly matching their dedicated semantic category colors. Segment gaps are formed by 1.5dp hairline gaps matching the underlying card background.

### Transaction Lists
- Density-calibrated rows with 12dp vertical padding.
- Left side: 40dp rounded-2xl (`rounded-xl`) category container holding the monochromatic category icon.
- Center: Transaction name in `body-md` bold, with timestamp and payment method below in `body-sm` secondary ink.
- Right side: Numerical expense formatted strictly in tabular lining figures (`title-md`), displaying negative values as plain dark ink (`रु 1,200.00`) and income entries accented with moss green (`#4D7C5F`).

### Input Fields
- Container-style filled inputs with a 16dp radius.
- Background colored with `#F3F4F6` (light) / `#18191E` (dark).
- Active focus state is indicated via a 2dp outline in terracotta (`#C85A32` / `#F08C65`), never relying on glowing box-shadows.
- Numeric amount inputs feature a persistent `रु` affix pinned to the left in tabular typography.
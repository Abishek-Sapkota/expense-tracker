---
name: Ledger Material Design System
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#554336'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#887364'
  outline-variant: '#dbc2b0'
  surface-tint: '#904d00'
  primary: '#8d4b00'
  on-primary: '#ffffff'
  primary-container: '#b15f00'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb77d'
  secondary: '#5d5e61'
  on-secondary: '#ffffff'
  secondary-container: '#e2e2e5'
  on-secondary-container: '#636467'
  tertiary: '#006b2c'
  on-tertiary: '#ffffff'
  tertiary-container: '#00873a'
  on-tertiary-container: '#f7fff2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdcc3'
  primary-fixed-dim: '#ffb77d'
  on-primary-fixed: '#2f1500'
  on-primary-fixed-variant: '#6e3900'
  secondary-fixed: '#e2e2e5'
  secondary-fixed-dim: '#c6c6c9'
  on-secondary-fixed: '#1a1c1e'
  on-secondary-fixed-variant: '#454749'
  tertiary-fixed: '#7ffc97'
  tertiary-fixed-dim: '#62df7d'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005320'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
  display-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 40px
  display-md:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
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
  gutter-tablet: 1.5rem
  gutter-desktop: 2rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system expresses utility, trust, and absolute privacy for an offline personal financial ledger. The visual signature adopts a refined **Material 3 (Material You)** philosophy with expressive editorial detailing: utilitarian clarity meets tactile warmth.

- **Brand Personality**: Grounded, hyper-reliable, intentional, and self-contained. It feels like an impeccably engineered physical field ledger translated into modern Android glass and ink.
- **Target Audience**: Discerning individuals who value financial privacy, offline sovereignty, instant execution, and zero clutter.
- **Emotional Response**: Quiet confidence and relief. The interface avoids aggressive financial gamification, promoting calm control through clear typographic numbers, high-contrast hero modules, and warm amber functional cues.
- **Design Movement**: Modern Material 3 executed with tonal depth layering, warm amber accents, crisp hairline borders, and pill-shaped structural components.

## Colors

The palette balances clinical legibility with warm tactile accents:

- **Primary (`#D97706`)**: Warm deep amber used for pivotal interactive elements, FABs, active indicators, and critical focus states.
- **Primary Container (`#FEF3C7`)**: Soft warm amber tint for active chips, search filters, and selection tags. Pair with **On-Primary Container (`#78350F`)**.
- **On-Primary (`#FFFFFF`)**: High-contrast white text/glyphs over solid amber components.
- **Dark Hero Surface (`#121316` / `#1A1C1E`)**: High-contrast charcoal surfaces dedicated to hero balance cards and account overview modules, pairing against bright white typography and amber summaries.
- **Light Neutral Surfaces**: Canvas background (`#F8F9FA`), Base Card Surface (`#FFFFFF`), and Tiered Container Surface (`#F1F3F5`).
- **Subtle Outline (`#E5E7EB`)**: Precise 1px hairline divider ensuring visual boundaries without shadow weight.
- **Status & Indicators**:
  - **Credit (`#16A34A` / `#22C55E`)**: Understated forest green for incoming credit and net-positive flows (`+रु`).
  - **Debit (`#1F2937`)**: Neutral dark charcoal for debits and expenditures (`-रु`), avoiding alarmist bright reds to preserve mental calm.
  - **Offline Status Indicator (`#2A2D32`)**: Subtle matte pill badge with hairline stroke, signaling local-only vault integrity.

## Typography

Typography relies on **Roboto Flex** to deliver Google’s standard Material You mechanical precision combined with balanced letterforms for dense numeric ledgers:

- **Currency Numerals**: Use tabular numbers (`font-variant-numeric: tabular-nums`) across all transaction rows and balance sheets to maintain vertical decimal alignment.
- **Rupee Glyph (`रु` / `₹`)**: Set with optical vertical alignment and consistent tracking alongside digits. Large totals default to `display-lg-mobile` on handsets and `display-lg` on wider viewports.
- **Top App Bar**: Styled using `title-lg` with medium-high contrast `#1A1C1E`.
- **Transaction Metadata**: Displayed via `body-md` for descriptions and `label-sm` (uppercase, track +0.5px) for categorical timestamps, account ledgers, and payment modes.

## Layout & Spacing

The layout is built around an 8dp (0.5rem) spatial cadence designed for touch targets and high-density financial review:

- **Grid Architecture**: 4-column layout on compact mobile (<600dp) with 16dp (`margin`) margins and 16dp (`gutter`) gutters. Expands to 8 columns on tablets with 24dp spacing, and 12 columns on large viewports centered at a maximum constraint of 1024dp.
- **Rhythm**:
  - `space-xs` (4dp): Intra-chip padding and sub-text metadata gaps.
  - `space-sm` (8dp): Space between badge indicators, icon-label offsets, and dense list rows.
  - `space-md` (16dp): Standard card internal padding, item separations, and input field insets.
  - `space-lg` (24dp): Card margins and separation between transactional date groups.
  - `space-xl` (32dp): Major structural section splits and hero header margins.

## Elevation & Depth

This system intentionally eliminates traditional blurred multi-tier drop shadows in favor of **Tonal Layering** and **Hairline Structural Boundaries**:

- **Ground Level (Canvas)**: `#F8F9FA` acts as the global un-elevated backdrop.
- **Surface Level 1 (Default Containers & List Items)**: Pure `#FFFFFF` surfaces nested over `#F8F9FA`, framed by a continuous `1px solid #E5E7EB` outline.
- **Surface Level 2 (Nested Groupings & Search Bars)**: `#F1F3F5` tonal fills without borders, used inside modal bottom sheets and inline filters.
- **Hero Elevation**: Achieved through dark surface polarity rather than y-axis displacement. The Primary Balance Ledger Card uses an obsidian core (`#121316`) that visually commands the viewport hierarchy.
- **Elevated Interactive Objects (Floating Action Button)**: Uses a tight ambient diffusion (`0px 4px 12px rgba(217, 119, 6, 0.25)`) when pressed, reinforcing the amber primary accent rather than gray gloom.

## Shapes

The interface embraces Material 3's high-radius organic geometry:

- **Cards & Hero Ledger Modules**: 24dp corner radii (`rounded-xl`), creating distinct, friendly modules.
- **Chips & Badges**: Fully pill-shaped (`9999px`), ensuring seamless category tagging and state verification.
- **Floating Action Button (FAB)**: Extended or standard pill FAB with a minimum 16dp to 28dp smooth radius.
- **Bottom Navigation Active Indicator**: Pill-shaped container (`64px x 32px`) encapsulating navigation icons.
- **Inputs**: 16dp to 20dp smooth corner radius, conforming to the structural card envelope.

## Components

### Buttons & FAB
- **Extended Floating Action Button (FAB)**: Background `#D97706`, text/icon `#FFFFFF`. Rounded pill shape (`rounded-full`), height 56dp, padding 16dp horizontal. Rests at the bottom right with 16dp margins.
- **Standard Primary Button**: Fully rounded pill (`9999px`), height 48dp, `#D97706` background with active ripple feedback.
- **Tonal Button**: Background `#FEF3C7`, text `#78350F`, height 40dp, zero border.

### Filter & Category Chips
- **Status / Category Chips**: Height 32dp, pill-shaped (`rounded-full`). Unselected state features `#FFFFFF` background with a 1px `#E5E7EB` hairline stroke and `#1F2937` typography. Selected state transitions to `#FEF3C7` background and `#78350F` label with no border.
- **Offline Indicator Pill**: Height 24dp, `#1A1C1E` background, `#FFFFFF` text (`label-sm`), containing a subtle 6dp pulsing or solid green emerald dot.

### Cards
- **Dark Balance Card (Hero)**: Deep charcoal (`#121316`) background, 24dp corner radius, 20dp padding. Displays large bold currency figures (`display-lg-mobile`) in pure `#FFFFFF`, secondary statistics in `#9CA3AF`, and quick stats (Income/Expense) divided with subtle 1px border lines (`#2A2D32`).
- **Standard Card**: White `#FFFFFF`, 20dp corner radius, 1px `#E5E7EB` hairline border, no shadow.

### Lists & Ledger Rows
- **Transaction Item**: Height 64dp, 2-line structure. Left: 40dp rounded circular icon container (`#F1F3F5`). Center: Title (`body-lg`, `#1A1C1E`) stacked above timestamp and category (`label-md`, `#6B7280`). Right: Tabular amount. Incoming credits show `#16A34A` with a preceding `+`, debits show `#1F2937` with a preceding `-`. Items are separated by horizontal hairline dividers inset 72dp.

### Input Fields
- **Amount & Detail Fields**: Filled container style (`#F1F3F5`) with a 16dp corner radius. Label sits inside with `label-md`. Focus transitions container outline to a crisp 2px border in `#D97706` without layout shift. Currency symbol `रु` persists as an anchored prefix.

### Navigation Bar
- **Bottom Navigation**: Background `#FFFFFF` or `#F8F9FA` with top hairline border (`1px solid #E5E7EB`). Active destination highlighted using an amber pill indicator (`#FEF3C7`) containing an amber icon (`#D97706`). Inactive icons sit at `#6B7280`.
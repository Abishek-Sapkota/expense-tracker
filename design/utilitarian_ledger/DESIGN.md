---
name: Utilitarian Ledger
colors:
  surface: '#faf9f6'
  surface-dim: '#dbdad7'
  surface-bright: '#faf9f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f1'
  surface-container: '#efeeeb'
  surface-container-high: '#e9e8e5'
  surface-container-highest: '#e3e2e0'
  on-surface: '#1a1c1a'
  on-surface-variant: '#57423b'
  inverse-surface: '#2f312f'
  inverse-on-surface: '#f2f1ee'
  outline: '#8a726a'
  outline-variant: '#dec0b7'
  surface-tint: '#a23e18'
  primary: '#9f3c16'
  on-primary: '#ffffff'
  primary-container: '#bf542c'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb59c'
  secondary: '#b6171e'
  on-secondary: '#ffffff'
  secondary-container: '#da3433'
  on-secondary-container: '#fffbff'
  tertiary: '#186a22'
  on-tertiary: '#ffffff'
  tertiary-container: '#358438'
  on-tertiary-container: '#f7fff1'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcf'
  primary-fixed-dim: '#ffb59c'
  on-primary-fixed: '#390c00'
  on-primary-fixed-variant: '#822801'
  secondary-fixed: '#ffdad6'
  secondary-fixed-dim: '#ffb3ac'
  on-secondary-fixed: '#410003'
  on-secondary-fixed-variant: '#930010'
  tertiary-fixed: '#a3f69c'
  tertiary-fixed-dim: '#88d982'
  on-tertiary-fixed: '#002204'
  on-tertiary-fixed-variant: '#005312'
  background: '#faf9f6'
  on-background: '#1a1c1a'
  surface-variant: '#e3e2e0'
typography:
  display-hero:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-hero-mobile:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '600'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-currency-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0em
  label-currency-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0em
  label-currency-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0em
  label-action:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-caption:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is engineered for high-frequency, offline-first personal financial management in Nepal. Rooted in utility, privacy, and absolute visual clarity, the interface strips away ornamental bloat in favor of a tactile, paper-like ledger experience adapted for modern Android ergonomics.

### Personality & Demographics
- **Demographic**: Tech-conscious individuals, small merchants, students, and professionals in Nepal requiring immediate, frictionless transaction logging without internet dependency.
- **Tone**: Trustworthy, restrained, local, and clinical. Money is treated with seriousness and precision.
- **Visual Style**: Modern Utilitarian Minimalist with Material 3 foundations. It replaces saturated tech-blue aesthetics with authentic regional warmth (terracotta), grounded by precise typography and crisp hairline separation.

### Core Principles
1. **Privacy Manifest**: Interfaces explicitly communicate zero-cloud dependency; UI surfaces feel localized, permanent, and private.
2. **Numeric Legibility First**: Layouts prioritize rapid scanning of amounts, dates, and balances above all decorative assets.
3. **Intentional Chromatic Semantics**: Accent color is strictly operational; financial statuses (inflow/outflow) remain immutable across all themes.

## Colors

The palette employs a deliberate chromatic boundary between brand identity, structural scaffolding, and transactional accounting semantics.

### Key Roles
- **Primary Accent (`#C85A32` / Dark: `#D86B43`)**: Terracotta. Reserved exclusively for primary conversion elements, prominent action triggers (FAB), active navigation indicators, and hero balance highlights.
- **Debit / Outflow (`#D32F2F` / Dark: `#E57373`)**: Fixed crimson/coral. Used strictly for negative transactions, debits, liabilities, and destructive actions. Never influenced by accent customization.
- **Credit / Inflow (`#2E7D32` / Dark: `#81C784`)**: Fixed pine/emerald green. Used strictly for positive cash events, income, loan settlements received, and net positive balances.
- **Neutral Light Stack**:
  - `canvas`: `#FAF9F6` (clean off-white paper)
  - `surface-elevated`: `#FFFFFF`
  - `surface-container`: `#F5F3EE`
  - `hairline-subtle`: `#E6E3DC`
  - `text-high-contrast`: `#191C1D`
  - `text-muted`: `#706E69`
- **Neutral Dark Stack (OLED-friendly)**:
  - `canvas`: `#121212` (pure charcoal foundation)
  - `surface-elevated`: `#1E1E1E`
  - `surface-container`: `#242424`
  - `hairline-subtle`: `#2E2E2E`
  - `text-high-contrast`: `#E3E2DE`
  - `text-muted`: `#9E9C96`

### Functional Rules
- Signage is mandatory with semantic tints: Outflow requires the minus symbol (−रु450.00); Inflow requires the explicit plus symbol (+रु5,000.00).
- Neutral text must never be tinted with green or red; semantic tints are applied solely to monetary figures, status chips, and contextual directional icons.

## Typography

The typography is tuned for dense, accurate quantitative comprehension. Inter is selected for its neutral glyph design, legible Devangari fallback compatibility, and OpenType tabular figure support.

### Formatting & Localization Standards
- **Tabular Figures (`tnum`)**: All numeric amount displays, tables, list item trailing values, and balance summaries must enable `font-feature-settings: "tnum" 1`. This prevents column jitter during scrolling and dynamic updates.
- **South Asian Lakh/Crore Grouping**: Numbers format using the standard 2,2,3 schema:
  - Example: `रु1,23,456.78` (One Lakh, Twenty-Three Thousand, Four Hundred Fifty-Six).
- **Currency Symbol**: The Devanagari Rupee abbreviation (`रु`) is fixed directly to the numeric amount with no space to prevent uncoupled line wrapping (`रु2,500.00`).
- **Sign Alignment**: Explicit signs (`+` or `−`) precede the currency symbol immediately: `+रु5,000.00`, `−रु450.00`. Use the dedicated mathematical minus sign (`−` / U+2212) rather than a hyphen.

## Layout & Spacing

A disciplined 4dp/8dp incremental spacing system dictates all screen compositions, standardizing hand reach on mobile devices.

### Layout Mechanics
- **Mobile Grid**: 4-column layout, 16px screen margins, 16px internal card gutters. Compact mode reduces horizontal margins to 12px for low-resolution devices.
- **Tablet / Large Handheld Reflow**: 8-column layout, 24px margins. Ledger and visual charts (Trends) split into an asymmetric two-pane master-detail configuration at width ≥ 600dp.
- **Safe Padding**: Bottom padding of scrollable views must accommodate the 80dp Material 3 Bottom Navigation bar plus an additional 16dp clearance buffer (`96dp` minimum bottom inset) to prevent floating action obscuration.

## Elevation & Depth

This system intentionally departs from diffuse drop shadows, opting instead for structural depth created by **tonal layering** and **crisp 1px hairlines**.

### Surface Hierarchies
- **Level 0 (Base Canvas)**: Flat `#FAF9F6` (Light) / `#121212` (Dark). The fundamental drawing background for all parent scroll views.
- **Level 1 (Grouped Cards & Sheets)**: `#FFFFFF` (Light) / `#1E1E1E` (Dark), bounded by a mandatory 1px border stroke (`#E6E3DC` / `#2E2E2E`). Zero shadow blur.
- **Level 2 (Active Floating Elements & FAB)**: `#C85A32` (Light) / `#D86B43` (Dark). Minimal functional elevation: `box-shadow: 0px 3px 8px rgba(0, 0, 0, 0.12)`.
- **Level 3 (Modal Bottom Sheets / Dialogs)**: `#F5F3EE` (Light) / `#242424` (Dark), combined with a 40% opacity neutral-black canvas backdrop dim.

### Border Strokes vs Shadows
Lists and cards rely on hard structural lines. Dividers within grouped lists run flush with a 56px left inset (matching the icon monogram margin) and do not pierce outer card boundaries.

## Shapes

The geometric vocabulary balances modern ergonomics with functional utility. Corner radii avoid exaggerated playful bubbly curvatures in favor of defined, durable forms.

### Curvature Tokens
- **Base Components (Inputs, List Items, Context Cards)**: `8px` (`0.5rem`). Provides structural discipline when stacked.
- **Cards & Modal Sheets (`rounded-lg`)**: `16px` (`1rem`). Softens high-volume content blocks.
- **Pills, Monograms, & Action Triggers (`rounded-full`)**: Fully circular/pill-shaped (`9999px`) for interactive category chips, selection pills, and the 56dp FAB.

## Components

### 1. Material 3 Bottom Navigation Bar
- **Dimensions & Structure**: 80dp height, 5 persistent destinations: *Ledger*, *Trends*, *Loans*, *Accounts*, *Settings*.
- **State Styling**: Active destination features a Terracotta pill indicator behind a filled vector icon with a medium-weight 12px label; inactive destinations display an unfilled outline icon with a muted 12px label.

### 2. Grouped Hairline List Cards
- **Structure**: Individual transaction entries are bundled inside an elevated white/charcoal card wrapped with a 1px border (`#E6E3DC` / `#2E2E2E`).
- **Interior Layout**:
  - **Left**: 40dp circular monogram badge tinted in surface-container with a centered category glyph.
  - **Center**: Primary payee/title (`body-md`, bold weight) with transaction timestamp and account source directly beneath (`body-sm`, text-muted).
  - **Right**: Amount formatted with tabular numerals (`label-currency-md`), strictly right-aligned. Inflow uses Credit tint (`+रु...`); Outflow uses Debit tint (`−रु...`).

### 3. Floating Action Button (FAB)
- **Visuals**: 56dp circular or rounded-square (`16px`) container pinned 16dp from the bottom navigation bar on the bottom-right axis.
- **Color**: Solid Terracotta (`#C85A32`) filled with a crisp white plus (`+`) icon.
- **Interaction**: Scrolls to minimize into a 40dp compact variant upon rapid downward scroll list gestures.

### 4. Selection Mode Action Bar (CAB)
- **Trigger**: Long-press on any transaction item initiates contextual multi-selection.
- **Appearance**: Replaces the default top app bar with a dark neutral container (`#191C1D` in Light mode, `#242424` in Dark mode).
- **Controls**: Displays selected item count (e.g., "3 selected"), accompanied by actions: *Batch Categorize*, *Delete Selected* (crimson icon), and *Select All*.

### 5. Filter Chips & Date Range Pills
- **Styling**: 32dp height, fully pill-shaped.
- **Unselected**: Surface-container background with a 1px hairline border, text-muted label.
- **Selected**: Solid Terracotta background with white high-contrast text and a left-aligned check icon.

### 6. Form Inputs & Currency Pad
- **Monetary Input**: Top-aligned, borderless display showing the entered sum in `display-hero` typography. Tabular figures active.
- **Form Fields**: Filled M3-style fields utilizing the `#F5F3EE` / `#242424` container fill, underlined by a 1px resting border that shifts to a 2px Terracotta stroke when focused.
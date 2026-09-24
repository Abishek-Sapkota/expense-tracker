---
name: Calm Terracotta Ledger
colors:
  surface: '#ffffff'
  surface-dim: '#dadada'
  surface-bright: '#ffffff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1d1b1a'
  on-surface-variant: '#55433e'
  inverse-surface: '#303030'
  inverse-on-surface: '#f1f1f1'
  outline: '#88726d'
  outline-variant: '#dbc1bb'
  surface-tint: '#984630'
  primary: '#823521'
  on-primary: '#ffffff'
  primary-container: '#a04c36'
  on-primary-container: '#ffdbd2'
  inverse-primary: '#ffb4a1'
  secondary: '#586249'
  on-secondary: '#ffffff'
  secondary-container: '#dce7c7'
  on-secondary-container: '#5e684f'
  tertiary: '#265456'
  on-tertiary: '#ffffff'
  tertiary-container: '#3f6c6e'
  on-tertiary-container: '#bcebed'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd2'
  primary-fixed-dim: '#ffb4a1'
  on-primary-fixed: '#3c0800'
  on-primary-fixed-variant: '#7a2f1c'
  secondary-fixed: '#dce7c7'
  secondary-fixed-dim: '#c0cbac'
  on-secondary-fixed: '#161e0b'
  on-secondary-fixed-variant: '#404a33'
  tertiary-fixed: '#bcebed'
  tertiary-fixed-dim: '#a0cfd1'
  on-tertiary-fixed: '#002021'
  on-tertiary-fixed-variant: '#1f4d50'
  background: '#ffffff'
  on-background: '#1d1b1a'
  surface-variant: '#e2e2e2'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 48px
    fontWeight: '400'
    lineHeight: 56px
    letterSpacing: -0.25px
  display-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 40px
    fontWeight: '400'
    lineHeight: 48px
    letterSpacing: 0px
  display-md:
    fontFamily: Roboto Flex
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 44px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  headline-sm:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '400'
    lineHeight: 32px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers a serene, distraction-free personal finance environment tailored for an offline-first mobile context in India. Instead of frantic stock tickers, gamified celebrations, or fluorescent fintech gradients, the experience feels like an impeccably kept clothbound ledger—durable, private, quiet, and deliberate.

The design philosophy combines Material Design 3 (Material You) architectural primitives with an earthy, warm tactile sensibility. It evokes an immediate emotional state of relief, control, and discretion. The interface handles automated SMS transaction parsing locally on the device with zero cloud latency, reflecting that reliability through solid, predictable shapes, tactile surface tonal shifts, and strict absence of speculative hype.

The visual style marries:
- **Earthen Minimalism:** Generous padding, organic clay-inspired tonal balance, and zero noisy micro-animations.
- **Material 3 Surface Elevation:** Natural tonal container shifts instead of dramatic, drop-shadow-heavy layering.
- **Utilitarian Clarity:** Information-dense Indian Rupee (Lakh/Crore) financial parsing rendered with unambiguous sign indicators and monospace tabular numerals.

## Colors

The palette is rooted in baked earth, clay terracotta, and dry river stone. It replaces saturated neo-banking primaries with organic minerals that remain gentle on tired eyes during evening budget reviews.

### Role Tokens & Semantic Intent

#### Light Theme
- **Surface Canvas (`#FAF8F5`)**: A soft, bleached khadi paper baseline, eliminating harsh OLED glare.
- **Surface Container Levels**:
  - Lowest: `#F4F2EF`
  - Low: `#EEECE8`
  - Container (Default Cards): `#E8E6E2`
  - High: `#E2E0DC`
  - Highest: `#DCDAD6`
- **Primary (`#A04C36` - Terracotta)**: Reserved for deliberate focal triggers—Primary FAB, active category filters, critical action confirmations.
- **On-Primary (`#FFFFFF`)**: Crisp, accessible text on deep terracotta.
- **Primary Container (`#FFDAD2`) / On-Primary Container (`#3E0A01`)**: Tinted hero backgrounds and active navigational indicators.
- **Secondary (`#586249` - Dried Sage)**: Used for secondary metadata, balance reconciliation badges, and passive tags.
- **Tertiary (`#386567` - Deep Slate Teal)**: Serves bank account/source identifiers (e.g., SBI, HDFC, Cash, UPI).

#### Financial Sentiment Tokens (Accessible & Muted)
Crucially, balance changes do not rely solely on color. Outflows and inflows employ low-chroma earthen tints backed by mandatory typographic symbols:
- **Debit / Expense Accent (`#8C382A`)**: Muted clay red. Always preceded by a minus sign (`− ₹`).
- **Credit / Income Accent (`#386843`)**: Muted forest moss. Always preceded by an explicit plus sign (`+ ₹`).
- **Neutral / Transfer Accent (`#515F6A`)**: River slate. Always paired with bi-directional arrow markers (`⇄ ₹`).

#### Dark Theme
- **Surface Canvas (`#131312`)**: Deep graphite stone.
- **Surface Containers**:
  - Lowest: `#0E0E0E`
  - Low: `#1B1B1A`
  - Container (Cards): `#222220`
  - High: `#2B2B29`
  - Highest: `#363633`
- **Primary (`#FFB4A2`) / On-Primary (`#5F1B0B`)**: Softened warm blush terracotta for low eye-strain contrast.
- **Debit Dark Accent (`#FFB3A4`)**: Desaturated clay.
- **Credit Dark Accent (`#98D7A5`)**: Desaturated sage.

## Typography

The type system prioritizes financial scanning, numerical precision, and multi-lingual Indian currency conventions. Roboto Flex is used uniformly across display, body, and labels to maintain structural consistency, with OpenType tabular figures (`tnum`) enabled on all numerical outputs.

### Indian Numbering & Currency Formatting Rules
- **Lakh and Crore Formatting**: Display numbers using the Indian numbering system separator standard: `₹X,XX,XXX.XX` (e.g., `₹1,23,456.78`).
- **Tabular Numerals**: All transaction items, balances, and period metrics must activate OpenType feature `'tnum' 1, 'lnum' 1`. This aligns decimals and commas vertically across feed rows.
- **Signage Placement**: A strict minus `−` or plus `+` must always precede the currency sign:
  - Valid: `− ₹1,420.00` | `+ ₹50,000.00`
  - Prohibited: `-₹1,420.00` | `(₹1,420.00)`
- **Hero Period Totals**: Use `display-lg-mobile` (40px) or `display-lg` (48px) with a lighter font weight (`400`) to let the sheer mass of the glyphs establish priority without feeling aggressive. The Rupee symbol `₹` at large display sizes scales to `85%` of the cap height with an 8px right tracking offset to eliminate glyph collision.

## Layout & Spacing

Layout adheres to a strict 8dp (0.5rem) baseline grid with a 4dp baseline sub-grid for badges and chip contents.

### Mobile Grid Philosophy
- **Standard Handset (<600dp)**: 4-column fluid layout with `margin: 1rem` (16px) and `gutter: 1rem` (16px).
- **Expanded Foldable / Tablet (600dp - 840dp)**: 8-column layout with `margin-tablet: 1.5rem` (24px). Single hand ledger transforms into an analytical master-detail split panel.
- **Rhythm & Safe Areas**: Top app bars enforce a 64px structural block. Transaction list items adopt a fixed minimum touch target of 56px with a vertical gap of `space-xs` (4px) inside contained cards, or zero margins with an inset divider (left-aligned to label start).

## Elevation & Depth

True to modern Material Design 3, this system deprecates heavy, dark drop-shadows in favor of **Tonal Surface Tinting** and crisp hairline low-contrast outlines.

### Elevation Levels

1. **Level 0 (Base Canvas)**: Completely flat background (`surface`). Zero shadow, zero tint.
2. **Level 1 (Card & Feed Surfaces)**: Surface Container Low. Elevation achieved by color delta (1.5% tint of Primary `#A04C36` blended over the canvas in light mode; 5% in dark mode). Border: `1px solid rgba(0, 0, 0, 0.04)` in light mode, `1px solid rgba(255, 255, 255, 0.06)` in dark mode.
3. **Level 2 (Hero Analytics Card & Active Selection)**: Surface Container. Tonal step increase. Ambient shadow only: `0px 2px 8px -2px rgba(96, 93, 91, 0.08)`.
4. **Level 3 (Sticky Bottom App Bars & Bottom Sheets)**: Surface Container High. Semi-diffused ambient shadow: `0px -4px 16px 0px rgba(0, 0, 0, 0.04)`.
5. **Level 4 (SMS Parsing Confirmation Modals & FAB)**: Surface Container Highest. A soft, warm tinted elevation: `0px 6px 16px -4px rgba(160, 76, 54, 0.16)`.

## Shapes

The design system uses deliberate, rounded geometry conforming to Material 3 shape tiers, balancing utility with warm softness.

### Shape Tiers
- **Hero Period Overview Card**: Extra-Large boundary (`28px`). Creates an organic framing device for monthly aggregate totals at the top of the viewport.
- **Standard Transaction Cards & Panels**: Medium boundary (`16px`). Ensures high internal content density without corner pinching.
- **Filter Chips & Account Pills**: Small boundary (`20px`). Perfectly centered height-to-curve ratio for thumb-friendly scrolling lists.
- **Buttons & Interactive Action Triggers**: Full pill (`9999px`). Unambiguous affordance that signals clickability or touch confirmation.
- **Input Fields & Textboxes**: Small rounded corner (`8px` inside, `12px` container).
- **Bottom Sheets**: Asymmetric rounding with `28px` on top-left and top-right, flat (`0px`) on bottom.

## Components

### 1. Hero Card (Period Total)
- **Geometry**: 28px border radius, Surface Container elevation tint with 16px internal padding.
- **Content**:
  - Period label (`label-md`, secondary text color).
  - Main expense total: `display-lg-mobile` (40px tabular numbers) with light weight. Right-hand indicator denotes cycle trend (`↑ 4% vs last mo`).
  - Dual sub-ledger strip at the card's base: two small internal rounded containers (12px) separating total "Spent" (Debits) and "Received" (Credits) with distinct `−` and `+` symbols.

### 2. Transaction List Tile
- **Touch Target**: 64px height.
- **Structure**:
  - Left: 40px circular container carrying the merchant/category glyph (e.g., Grocery, Swiggy/Zomato, Fuel, Metro).
  - Center: Title (`title-sm`) displaying verified SMS payee; Subtitle (`body-sm`) containing account anchor (e.g., `SBI ••4091`) and humanized time (`Today, 2:15 PM`).
  - Right: Typographic balance indicator (`title-md`, right-aligned, `tnum`). Color tone matched to debit/credit tokens, followed by a secondary small muted chip showing parsed tag status.

### 3. Chips (Category & Account Filtering)
- **Geometry**: Height 32px, corner radius 20px, horizontal padding 12px.
- **Inactive**: Surface Container Low background, 1px ghost outline (`outline-variant`), text in `label-md`.
- **Active**: Primary Container tint, zero border, checkmark icon (18px) prefixed to label text.

### 4. Buttons
- **Primary Pill Action Button**: 48px height, 9999px border radius, filled terracotta (`primary`), text `label-lg` in on-primary.
- **Tonal Button**: 40px height, Surface Container Highest fill, text `label-md` in on-surface.
- **Floating Action Button (Add Manual / Sync SMS)**: Material 3 squircle-pill hybrid (56px × 56px, 16px radius), primary container fill with on-primary-container icon.

### 5. Input Fields
- **Container**: Outlined style, 12px radius, 56px height. Active outline shifts to 2px primary terracotta.
- **Rupee Prefix Input**: Currency symbol permanently fixed on the left as an invariant typographic adornment (`title-lg`, neutral text), separating the user’s numerical input stream.

### 6. SMS Local Parse Card (Utility Component)
- A specialized card appearing when a new financial SMS is detected on the handset.
- **Visual Style**: Surface Container High background, dashed inner highlight border (tertiary teal).
- **Layout**: Displays raw snippet with an instant "Confirm & Categorize" pill button and a subtle dismiss icon. Offline guarantee badge pinned to the card corner ("Processed on-device").
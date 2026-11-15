---
name: Literary Resonance
colors:
  surface: '#fff9e7'
  surface-dim: '#e0dac3'
  surface-bright: '#fff9e7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#faf4db'
  surface-container: '#f4eed6'
  surface-container-high: '#eee8d0'
  surface-container-highest: '#e9e3cb'
  on-surface: '#1e1c0d'
  on-surface-variant: '#444748'
  inverse-surface: '#333121'
  inverse-on-surface: '#f7f1d8'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#171818'
  on-primary: '#ffffff'
  primary-container: '#2c2c2c'
  on-primary-container: '#949393'
  inverse-primary: '#c8c6c5'
  secondary: '#605f50'
  on-secondary: '#ffffff'
  secondary-container: '#e6e3d0'
  on-secondary-container: '#666556'
  tertiary: '#390002'
  on-tertiary: '#ffffff'
  tertiary-container: '#600006'
  on-tertiary-container: '#ff5a52'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e4e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1b1c1c'
  on-primary-fixed-variant: '#474747'
  secondary-fixed: '#e6e3d0'
  secondary-fixed-dim: '#c9c7b5'
  on-secondary-fixed: '#1c1c11'
  on-secondary-fixed-variant: '#48473a'
  tertiary-fixed: '#ffdad6'
  tertiary-fixed-dim: '#ffb4ac'
  on-tertiary-fixed: '#410003'
  on-tertiary-fixed-variant: '#92030f'
  background: '#fff9e7'
  on-background: '#1e1c0d'
  surface-variant: '#e9e3cb'
typography:
  display-lg:
    fontFamily: Noto Serif
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-xl:
    fontFamily: Noto Serif
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: 0.05em
  headline-md:
    fontFamily: Noto Serif
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.4'
    letterSpacing: 0.02em
  body-lg:
    fontFamily: Newsreader
    fontSize: 20px
    fontWeight: '400'
    lineHeight: '1.8'
    letterSpacing: 0.01em
  body-md:
    fontFamily: Newsreader
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Work Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1.0'
    letterSpacing: 0.1em
  quote-display:
    fontFamily: Noto Serif
    fontSize: 22px
    lineHeight: '1.8'
    letterSpacing: 0.02em
spacing:
  unit: 4px
  container-margin: 32px
  gutter: 24px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
  section-gap: 64px
---

## Brand & Style

The design system is rooted in the "Maiwang" concept—a reference to the legendary book-eating insect that transforms into a celestial being, symbolizing the profound consumption of knowledge. The brand personality is scholarly, contemplative, and timeless. It aims to evoke the emotional response of entering a quiet, sunlit private library or a traditional scholar’s studio (*shufang*).

The design style is **Minimalism** blended with **Tactile / Skeuomorphic** elements. It avoids the coldness of modern tech by using organic textures and traditional compositions while maintaining a clean, spacious layout. The aesthetic focuses on "meaningful emptiness" (*liubai*), ensuring that the book excerpts remain the focal point.

## Colors

The palette is derived from the four treasures of the study and classical Chinese art. 
- **Ink Black (#2C2C2C):** Used for primary text and structural lines, mimicking high-quality carbon ink.
- **Paper White (#F4F1DE):** The foundation of the UI, providing a warm, low-fatigue reading surface resembling aged Xuan paper.
- **Cinnabar Red (#B22222):** Reserved for primary actions, critical labels, and seal-inspired iconography.
- **Celadon Green (#83A598):** A secondary accent used for calm states, progress indicators, or categorizing poetry and nature-related excerpts.
- **Paper Shadow (#EAE4CC):** A darker variant of the paper white used for subtle depth and borders.

## Typography

Typography prioritizes vertical rhythm and legibility. For Traditional Chinese characters, Noto Serif provides the requisite "woodblock print" weight and calligraphic terminals. 

- **Titles:** Use generous letter spacing to evoke the feeling of classical book covers. 
- **Vertical Orientation:** Where possible (e.g., section headers or decorative quotes), text should flow vertically from right to left.
- **Body Text:** Use `Newsreader` for English excerpts and `Noto Serif` for Chinese. The line height is intentionally high (1.6 - 1.8) to allow the "breath" of the page to be felt, mimicking the generous margins of Ming Dynasty publications.

## Layout & Spacing

This design system utilizes a **Fixed Grid** model within a flexible container. On mobile and tablet, the margins are exceptionally wide (32px+) to reinforce the sense of a physical page rather than a digital screen.

The spacing rhythm is based on a 4px baseline, but emphasizes large "breathing rooms" (*liubai*) between disparate content blocks. Layouts should be asymmetrical where possible, echoing the organic composition of Chinese ink paintings. Use white space as a structural element rather than a void.

## Elevation & Depth

Elevation is achieved through **Tonal Layers** and **Subtle Textures** rather than shadows.
- **Surface 0:** The base "Paper White" with a nearly invisible Xuan paper grain texture overlay.
- **Surface 1:** A slightly darker parchment tone (#EAE4CC) used for cards or inset areas.
- **Borders:** Instead of drop shadows, depth is indicated by "Silk Borders"—ultra-fine 0.5px lines in Ink Black or Cinnabar Red.
- **Z-axis:** When an element must float (like a modal), use a subtle backdrop blur combined with a delicate frame, suggesting a sheet of translucent rice paper placed over the content.

## Shapes

The shape language is strictly **Sharp (0)**. Modern rounded corners feel too "industrial" for this aesthetic. 
- **Rectilinearity:** All containers, buttons, and image frames must have 90-degree corners, reflecting the geometry of traditional book blocks and wooden printing frames.
- **Seals:** The only exception is the "Seal" component (iconography/branding), which may have slight organic imperfections to mimic a hand-carved stone stamp.
- **Dividers:** Use traditional "Cloud" or "Thunder" patterns sparingly as horizontal rules to separate sections.

## Components

- **Buttons:** Sharp-edged boxes with a 1px solid Ink Black border. Primary buttons use a Cinnabar Red background with white text, styled like a chop/seal. Hover states involve a slight shift in background texture rather than color brightness.
- **Chips/Labels:** Small, vertical rectangular tags attached to the side of cards, mimicking "book tabs" (*yuer*). 
- **Lists:** Unstructured by heavy lines; use generous vertical spacing and small Cinnabar dots or square "seal" bullets.
- **Cards:** Defined by a change in texture or a subtle 0.5px border. Cards should feel like loose-leaf pages stacked on a desk.
- **Input Fields:** Underlined only, using a weighted line that mimics a brush stroke (slightly thicker at the start, tapering at the end).
- **Iconography:** Hand-drawn, single-weight strokes. Use square frames for icons to suggest the footprint of a traditional stamp.
- **The "Pulse" (Scroll Indicator):** A subtle, celadon-green vertical line that grows as the user reads, representing the "Mai" (pulse/vein) of the text.
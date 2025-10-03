# Bean Documentation Guidelines

This package hosts the `.properties` descriptors used to document Convertigo beans. Each file provides both the short summary and the long documentation displayed in Studio, the web IDE, and the public documentation site. To keep all targets aligned, authors (human or AI) must follow the rules below.

## General Principles
- Write documentation in English. Use complete sentences and avoid abbreviations.
- Keep the tone factual and technical. The short summary is a single sentence that ends with a period.
- The short summary (`short_description` before the `|` separator) **must not contain HTML** markup or manual line breaks.
- Everything after the first `|` is HTML. Structure the content instead of stacking `<br/>` tags.

## Allowed HTML
| Purpose | Recommended tag | Notes |
| --- | --- | --- |
| Paragraphs | `<p>…</p>` | Prefer multiple paragraphs over `<br/>` sequences. |
| Lists | `<ul><li>…</li></ul>` (or `<ol>`) | Each `<li>` should be a full phrase ending with a period. |
| Inline emphasis | `<strong>`, `<em>` | Do not use `<b>` or `<i>` unless you really need legacy styling. |
| Inline code / names | `<code>` | Use for identifiers, file names, HTTP verbs, etc. |
| Notes / warnings | `<strong>Note:</strong>` | See dedicated section below. |

## Blocks for Notes
- Begin a note with `<p><strong>Note:</strong> …</p>`.
- If the note contains more paragraphs or a list, keep them immediately after the first paragraph. Example:
  ```properties
  <p><strong>Note:</strong> The following behaviour applies only at runtime.</p>\
  <ul>\
   <li>First impact…</li>\
   <li>Second impact…</li>\
  </ul>
  ```
- Do not wrap notes in additional `<blockquote>` or custom containers; the helper converts them for each target automatically.

## Line Continuations
- Use `\` at the end of each line except the last one to keep valid Java properties.
- Indentation with two spaces after the continuation `\` improves readability; keep it consistent across the file.

## Linking and References
- Prefer absolute URLs with the `https://` scheme. Use `<a href="…">` only if the link text differs from the URL; otherwise list the URL within `<code>`.
- When referencing other beans or properties, use `<i>Bean name</i>` or `<b>Property name</b>` rather than quotes.

## Checklist before saving
1. Short summary is a single plain-text sentence ending with a period.
2. No HTML classes, inline styles, or deprecated tags.
3. Notes start with `<strong>Note:</strong>` and group their paragraphs/lists together.
4. Lists are inside `<ul>` / `<ol>`, not simulated with `-` characters.
5. Line continuations (`\`) are present on every wrapped line but the last.
6. HTML is valid and balanced (matching `<p>`, `<li>`, etc.).

Keeping these rules ensures `DocumentationHelper` can reliably produce HTML for the IDE and Markdown for doc.convertigo.com. If in doubt, follow existing well-formed entries such as:
- `engine/src/com/twinsoft/convertigo/beans/steps/res/CallStep.properties`
- `engine/src/com/twinsoft/convertigo/beans/mobile/components/res/UIEventSubscriber.properties`

Happy documenting!

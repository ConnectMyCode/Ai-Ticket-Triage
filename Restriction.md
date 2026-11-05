Added as a strict project restriction:

### Preserve all existing JSX/HTML tag names and section identifiers

- Do not rename, replace, or modify existing tag names such as `div`, `section`, `main`, `header`, `form`, `button`, etc.
- Do not rename existing IDs, classes, or other identifiers assigned to sections and elements.
- Keep the existing DOM/JSX structure and naming consistent so the code remains easy to search, locate, understand, and maintain.
- When adding a feature, reuse the existing tags and identifiers wherever possible.
- If a new element is genuinely required, add it without renaming or changing existing elements.
- Make only the minimum necessary changes to implement the requested feature.

This restriction applies to all future frontend changes. No structural or naming changes for convenience, styling preferences, or refactoring.



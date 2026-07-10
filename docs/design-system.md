# Web-First Shared UI Rules

This project keeps its UI in shared Compose Multiplatform code, but the design target is the web app first. Shared screens should feel like a clean admin-style web app, not like a mobile form stretched onto desktop.

## Approved primitives

Use these wrappers before reaching for raw Material 3 components in feature screens:

- `AppTheme`
- `PageScaffold`
- `PageHeader`
- `SectionCard`
- `FormSection`
- `InlineActionRow`
- `AppButton`
- `AppTextField`
- `AppNumberField`
- `AppSelectField`
- `StatusBanner`
- `EmptyState`

Raw Material components are allowed in the design-system layer and in feature screens only when a wrapper does not exist yet.

## Layout rules

- Default to constrained desktop width instead of edge-to-edge layouts.
- Prefer grouped sections over one long undifferentiated column.
- Use two columns on wider screens when the content is data-dense or form-heavy.
- Keep destructive actions visually separated from primary actions.
- Keep primary actions at stable section or page endpoints.

## Visual rules

- Use `AppThemeTokens.spacing` and `AppThemeTokens.radii`; do not introduce ad hoc spacing values without a concrete reason.
- Use semantic colors from `AppThemeTokens.colors`, not direct hard-coded page colors in feature screens.
- Prefer clear hierarchy and dense but readable spacing over large empty card stacks.
- Keep body copy and labels compact. This is an operational UI, not a marketing page.
- Prefer icon-led actions over text-only actions when the meaning is standard and repeated often.
- Buttons should usually include a leading icon for navigation, add, save, refresh, purchase, delete, sign-in, and sign-out actions.
- Use icon-only actions only when the control remains unambiguous from context and still has an accessible content description.
- Avoid long button copy when a short label plus icon communicates the action just as clearly.

## Form and validation rules

- Field validation should appear inline when possible.
- Page-level success and error messages should use `StatusBanner`.
- Numeric inputs must use `AppNumberField`.
- Numeric values must reject non-numeric characters at input time and in the state/update handler so invalid characters never persist in state.

## Composition pattern

Default screen structure:

1. `PageScaffold`
2. `PageHeader`
3. Optional `StatusBanner`
4. One or more `FormSection` or `SectionCard` blocks

For large editor screens, split "create/edit" and "existing data" into separate visual zones on wide layouts.

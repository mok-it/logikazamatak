# Project Agent Context

- Numeric input fields must reject non-numeric characters at input time. Prefer filtering in the state/update handler so invalid characters never remain in the field state.
- Follow the shared web-first UI rules in `docs/design-system.md` when editing feature screens.
- Prefer the approved wrappers from `docs/design-system.md` over raw Material 3 components in feature screens. Raw Material components are allowed only in the design-system layer or when a wrapper does not exist yet.

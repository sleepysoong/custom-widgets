# Liquid Glass implementation map

## Rendering invariants

`CustomWidgetsTheme` installs `GlassTheme` and `GlassHost`, so both `MainActivity` and `WidgetConfigureActivity` own their own composition-scoped backdrop. Host capture is on a background-only sibling, never on NavHost, a Card, or a foreground control. `GlassDialog` creates a separate host inside the Dialog window. No captured source includes its consumers. No cross-window layer is shared.

The first version follows the plan's shared-original-source option. Nested controls read the host background, not their parent's content. `exportedBackdrop` is therefore unnecessary in this version. No per-list-item `rememberLayerBackdrop` is created.

`glassMaterial` centralizes corner validation, Dp-to-pixel conversion, color/blur/lens order, tint, outline and opaque fallback. Generic unsupported shapes skip lens. API31–32 use blur; API33+ may use lens; Off creates no recording layer. Material draws labels/icons after the backdrop, so text stays sharp.

The product always selects the strongest effect supported by the running Android version: API31–32 use blur and API33+ use Full blur/lens. There is no user setting, saved preference, or emergency UI toggle for reducing effects. `Off` is only a safe fallback below the app's minSdk or in Compose inspection previews.

## UI coverage

| Area | Implementation |
|---|---|
| App navigation | Floating capsule GlassNavigationBar with shadow and bottom system inset; Material items retain selection semantics and routing |
| App top bars | GlassTopAppBar / GlassMediumTopAppBar; transparent Material container |
| Buttons and icons | GlassButton / GlassSecondaryButton / GlassIconButton; native Material interaction |
| FABs | GlassFab / GlassExtendedFab |
| Cards and statuses | GlassCard / GlassSurface, including empty/error/success states |
| Selections | GlassFilterChip with check icon; size cards with selectable/radio semantics and check label |
| Forms | GlassTextField retains transformations, keyboard options, labels, errors and multiline editing; one shared outline responds to focus/error state |
| Values | GlassSlider with glass thumb/track; GlassSwitch with glass thumb and tinted native track |
| Existing modals | Gallery deletion and MCP registration use GlassDialog; each host background is clipped to the panel's exact shape |
| Create workflow | Compact steps and FoldableDualPaneWizard both migrated |
| Effect policy | No user control; strongest effect supported by Android API is automatic |
| Home widget | Glance and user DSL unchanged; only preview frame/chrome is glass |

There are currently no Popup/DropdownMenu/BottomSheet product flows; none were invented. They remain future component contracts in the plan.

## Source locations

All paths below are relative to `app/src/main/kotlin/com/customwidgets/app`.

- `ui/glass/GlassTheme.kt`: API capability policy, host and material modifier.
- `ui/glass/GlassControls.kt`: buttons, chip, field, switch and slider.
- `ui/glass/GlassSurfaces.kt`: cards, bars, FAB and modal.
- `ui/theme/Theme.kt`: both Activity entry points get the same design system.
- `MainActivity.kt`: transparent app scaffold and explicit inset ownership.
- `ui/{gallery,create,settings,mcp}/*Screen.kt`: all current screen call sites.

The unused gradient-only `ui/theme/LiquidGlassComponents.kt` was removed. `ui/preview/ComposeWidgetPreview.kt`, `widget/renderer/DslRenderer.kt` and the persisted DSL models are untouched.

## Diagnostics and rollback

Debug-only catalog: `com.customwidgets.app.qa.GlassCatalogActivity`. It provides dark/light, recreation, disabled controls, form input, switch, slider and a separate-window dialog. Glass intensity is automatically selected from the Android API level; the catalog has no effect-mode selector. It does not need an API key or network response.

There is no remote effect configuration or persisted intensity setting. API31–32 always use blur, API33+ use lens, and inspection previews use a safe opaque material. A rendering regression must be fixed in code or reverted in a corrective build.

No background animation or custom press gesture was added. Existing Material indications and motion behavior remain. No database schema change, generated-widget restyling or API protocol change was introduced.

## Known verification limits

Source migration and successful compilation do not establish frame-time, visual contrast or all GPU compatibility. Use `verification.md` and the plan's device matrix before treating this as fully validated for release. In particular, test API31/32, API33+, large fonts, TalkBack, fold transitions, IME and modal dismissal on actual devices.

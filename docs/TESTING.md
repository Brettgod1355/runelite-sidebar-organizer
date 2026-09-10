# Testing status

## Windows live-client check — September 10, 2026

Tester: repository owner (Brettgod1355). Results below were reported during an
interactive test session; they are not automated assertions or independent
maintainer verification.

Environment: Windows development client launched from IntelliJ IDEA, Java 17,
RuneLite 1.12.38, Sidebar Organizer 0.1.0. The launch screenshot identifies Java 17;
the supplied startup log identifies RuneLite 1.12.38 and confirms the plugin
started. Tested source: initial implementation merged in PR #1
(`bb5bc6ed4546dff127884fa320a35d1dfbfe8b36`).
No demonstration recording is attached yet.

### Reported passed

- [x] Drag sidebar icons into a custom order.
- [x] Click moved icons and confirm they open the correct panels.
- [x] Reorder using the organizer's Up / Down controls.
- [x] Restart the development client and retain the saved arrangement.
- [x] Disable and re-enable a panel plugin; its icon returns to its saved position.
- [x] Enable Lock dragging; dragging stops while Up / Down still work.
- [x] Disable Sidebar Organizer; the native default order returns.
- [x] Re-enable Sidebar Organizer; the saved custom order returns.
- [x] Resize between one and two sidebar columns; order, dragging, and panel
      selection continue to work.
- [x] Reset order; default order returns and remains reset after restarting.

The checklist records the behavior actually confirmed. It does not establish
coverage of every plugin, every drag surface, or all window sizes. The tester did
not identify an icon with a context menu, so that case was skipped, not passed.

## Remaining live-client checks

- [ ] Explicit first-to-last and last-to-first moves.
- [ ] Identify equal-priority tabs and reorder them.
- [ ] Confirm an inactive tab can move while the organizer stays selected.
- [ ] Exercise organizer-list dragging in both directions.
- [ ] Drop outside tabs, click without dragging, and cancel a drag.
- [ ] Verify existing context menus on plugins that actually provide them.
- [ ] Verify a plugin's programmatic panel-open action after reordering.
- [ ] Close all panels, reorder, and verify none opens unexpectedly.
- [ ] Enable a previously unseen panel plugin under both new-tab placement settings.
- [ ] Disable a panel plugin while its panel is selected and while another is selected.
- [ ] Switch RuneLite configuration profiles with different saved arrangements.
- [ ] Explicitly verify the drag lock on both the sidebar and organizer list.
- [ ] Exercise additional window sizes, display scaling, and themes.
- [ ] Test macOS and Linux input, cancellation, and wrapped columns.
- [ ] Check keyboard traversal; logical traversal order may differ from visual order.

## Automated coverage

The initial implementation passed 15 automated tests locally on Java 17 against
RuneLite 1.12.38. The [initial CI run](https://github.com/Brettgod1355/runelite-sidebar-organizer/actions/runs/34505258736)
also passed on Java 11 and 17.

Tests use RuneLite dependency classes directly, including ClientUI navigation
methods in a headless fixture. They cover ordering policies, serialization,
removed-plugin slots, identity handling, selection retention, drag cancellation,
and compatibility guards. Tests install the real RuneLite look and feel, paint
reordered wrapped tabs, and dispatch mouse presses against the new positions.
Test-only reflection initializes ClientUI; production code does not use it.

The fixture does not initialize the entire live game client. These automated and
reported manual results do not establish Plugin Hub approval.

## Suggested demonstration

Record a short client-only video showing:

1. The initial layout and a direct icon drag.
2. Clicking a moved icon to open its panel.
3. The organizer's Up / Down controls and drag lock.
4. Resizing between one and two columns.
5. A client restart with the custom arrangement retained.

An optional second clip can show disable/re-enable and reset. Label any edit that
skips the restart wait. A still image of the organizer panel is useful for a README,
but cannot demonstrate interaction or persistence. Record at readable resolution
and keep credentials, account settings, private chat, and unrelated desktop
windows out of frame.

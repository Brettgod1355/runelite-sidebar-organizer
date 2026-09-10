# Manual testing checklist

Run the development client with the intended RuneLite release. These live-client
checks remain necessary even when the headless tests pass.

- [ ] Drag the first icon to the last icon, and move it back.
- [ ] Move two same-priority icons and verify each still opens the right panel.
- [ ] Reorder an inactive tab using the organizer's Up/Down controls; verify the
      organizer panel stays selected and its controls remain usable.
- [ ] Test the list drag and direct sidebar drag in both directions.
- [ ] Drop outside the tabs, click without dragging, and right-click an icon.
- [ ] Verify each plugin's existing context menu and programmatic open action.
- [ ] Close all panels, reorder, and verify none opens unexpectedly.
- [ ] Enable a new plugin with a panel under both new-tab placement settings.
- [ ] Disable and re-enable a panel plugin; verify it returns to its saved slot.
- [ ] Disable a plugin while its panel is selected, and while another panel is selected.
- [ ] Restart the development client; verify the saved order returns.
- [ ] Switch RuneLite configuration profiles with different saved arrangements.
- [ ] Lock dragging; verify both drag surfaces stop moving tabs and Up/Down still work.
- [ ] Reset order; verify default priority/tooltip order and persistence after restart.
- [ ] Disable Sidebar Organizer; verify original order and normal panels are restored.
- [ ] Re-enable Sidebar Organizer; verify its saved order returns.
- [ ] Resize to a small client window and test overflow/wrapped sidebar tabs.
- [ ] Test Windows with the normal client theme and display scaling.
- [ ] Test macOS and Linux, including sidebar drag cancellation and wrapped columns.
- [ ] Check keyboard tab traversal; logical traversal order may differ from visual order.

## Automated coverage

Tests use RuneLite dependency classes directly, including ClientUI navigation
methods in a headless fixture. They also cover ordering policies, serialization,
removed-plugin slots, duplicate priorities/identities, metadata/selection retention,
and compatibility guards. The tests install the real RuneLite look and feel, paint reordered wrapped tabs,
and dispatch mouse presses against the new positions. Test-only reflection
initializes ClientUI; production code does not use it. The fixture does not
initialize the entire live game client, so real profile switching, platform input
behavior, and game-driven panel behavior must also be checked above.

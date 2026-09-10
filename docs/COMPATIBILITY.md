# Compatibility and design

## Why the bridge exists

In RuneLite 1.12.38, `ClientUI.sidebarEntries` is a final `TreeSet<NavigationButton>`
and `ClientUI.sidebar` is a `JTabbedPane`. RuneLite uses the set's iteration index
for selection, programmatic panel opening, history, and context menus. Reordering
only Swing tabs would break that relationship.

`SidebarBridge` finds the sidebar through the organizer panel's public Swing
ancestor chain and installs an `OrderedSidebarUI` using `JTabbedPane.setUI`.
The delegate subclasses RuneLite's existing UI and uses protected Swing layout
extension points to assign tab rectangles to the saved visual positions.

The actual Swing tab indexes, NavigationButton objects, native navigation set,
plugin priorities, selected component, and client listeners remain intact.
Painting, hit testing, tooltips, and context menus resolve through the rectangles
for each original logical tab index. Wrapped columns also translate the tab-run
lookup from the logical index to its visual position.

On disable, the bridge restores the original UI delegate. It does not reconstruct
or remove any other plugin's tabs. No runtime reflection, private-field access,
game tick loop, network calls, global input hook, or background polling is used.
Container changes are coalesced onto the EDT after RuneLite finishes each update.
The test fixture alone uses reflection to initialize ClientUI without starting
the game; test classes are not included in the plugin JAR.

## Configuration and identity

`sidebarorganizer.savedOrderV1` stores a JSON array of identities through RuneLite's
ConfigManager. Decoding has no UI side effects. ConfigChanged notifications reload
it, including changes generated while switching RuneLite configuration profiles.
A malformed value is left intact and ordering is suspended until reset.

A tab identity consists of its panel's fully qualified class name, a newline, and
its tooltip. The tooltip distinguishes plugins using the same generic panel
class. Renaming a panel class or tooltip makes it a new tab. Exact duplicate
identities are ambiguous, so the bridge refuses to customize that sidebar.

Disabled plugins remain in saved order but are not shown. New tabs default to the
end; they keep RuneLite's normal relative priority/tooltip order there. The optional
neighbor policy inserts new tabs after the closest preceding tab in the natural
order, or before the closest following tab if no predecessor exists. This rule is
deterministic and preserves relative order of all previously saved active tabs.
It is an explicit policy, not a claim to implement a minimal-diff algorithm.

## Upstream work and limits

See [research findings](RESEARCH.md) for primary-source review objections and the
ongoing native implementation. Reflection is explicitly disallowed by Plugin Hub
reviewers; avoiding it removes that specific objection, but is not Hub approval
for replacing the sidebar UI delegate.

The bridge currently accepts only the released RuneLiteTabbedPaneUI, right-side
tab placement, and wrapped layout. An unfamiliar layout causes it to stop with
a visible status and a diagnostic in the client log. If another component
replaces its UI delegate, it will not overwrite that replacement on disable.
Native support should supersede this plugin when available. The upstream native
Sidebar class is not a supported layout for this prototype.

Right-click menus and panel-opening callbacks remain attached to their original
buttons. A normal drag may select the source tab on mouse press. There is a lock
setting; this prototype does not integrate RuneLite's configurable UI drag hotkey.
Keyboard tab traversal follows the client's logical ordering and requires further
live-client evaluation. Persistent identity is a best-effort fallback until
RuneLite provides a stable identifier; renames do not retain the previous slot.

Headless tests cover RuneLite 1.12.38. The repository owner reported successful
basic Windows live-client checks on September 10, 2026; see [testing status](TESTING.md).
Profile switching, additional platform/input cases, and Plugin Hub acceptability
remain unverified. Public/protected Swing access does not make the client UI structure
a guaranteed RuneLite plugin API.

# Why sidebar reordering has not shipped, and what this prototype addresses

Research checked September 10, 2026. Sources include the supplied PRs, the supplied
December 15, 2024 Discord screenshots, related native work, released client source,
and Plugin Hub reviews. The Discord URL itself was not independently readable;
claims about that discussion are based on the supplied screenshots.

## It is not a blanket rejection of the feature

[PR 18406](https://github.com/runelite/runelite/pull/18406) closed unmerged, but
[its author withdrew it](https://github.com/runelite/runelite/pull/18406#issuecomment-2441504153)
after describing the implementation as premature and lacking time to continue.
Review requested [ConfigManager persistence](https://github.com/runelite/runelite/pull/18406#issuecomment-2401107404),
less unrelated code churn, and a policy for how new plugins interact with their
default priorities and an existing user arrangement.

[PR 18720](https://github.com/runelite/runelite/pull/18720) and
[PR 19910](https://github.com/runelite/runelite/pull/19910) remained open and unmerged
at the research check. An open PR is not proof that maintainers rejected its idea.
Adam's [later comment on 19910](https://github.com/runelite/runelite/pull/19910#issuecomment-4994879052)
points to continued work on [abextm/sidebar-reorder4](https://github.com/abextm/runelite/tree/sidebar-reorder4).
That branch extracts a separate Sidebar component and includes work on dragging,
popups, overflow, global mouse release handling, and Wayland. This is evidence of
ongoing implementation and cross-platform UI work, not a release commitment.

## Concrete review objections

Adam's [review on 19910](https://github.com/runelite/runelite/pull/19910#issuecomment-4106673302)
identifies redundant order state, vague configuration naming, parsing that mutates
the UI, tooltip-based identity, incorrect fallback sorting, and using plugin/profile
events instead of configuration changes. Other discussion covers selection,
flicker, resetting, and the drag gesture. These are observable behavior and
maintenance concerns that a working drag demo alone does not settle.

The supplied Discord screenshots explain two particularly difficult cases:

- Integer priorities cannot reliably express placement between equal-priority tabs.
- Persistent identity must handle shared panel classes, disabled plugins, and
  newly installed plugins without imposing changes on every existing Hub plugin.

The discussion considers IDs, a saved ordered list, and insertion relative to the
natural order. It does not establish one agreed or approved final algorithm.

## A specific Plugin Hub blocker

Reviewers explicitly say reflection is disallowed in
[Hub PR 15950](https://github.com/runelite/plugin-hub/pull/15950#issuecomment-5518711163),
[14851](https://github.com/runelite/plugin-hub/pull/14851#issuecomment-5225053988), and
[15492](https://github.com/runelite/plugin-hub/pull/15492#issuecomment-5429763061).
The [15492 review](https://github.com/runelite/plugin-hub/pull/15492#issuecomment-5438482750)
also disallows KeyboardFocusManager. These examples are not rejections of this
repository; they establish concrete objections that our implementation must avoid.

An early local prototype used private-field reflection and was discarded.
The committed runtime implementation uses public/protected Swing extension points
and no global keyboard hook. This removes that known implementation blocker.
It does not establish that Plugin Hub reviewers will approve UI-delegate replacement.

## Decisions in this prototype

| Concern | Implementation | Remaining limitation |
| --- | --- | --- |
| Private sidebar access | Public ancestor lookup and Swing UI delegate | Depends on the released sidebar structure; Hub review still needed |
| Native click/index mismatch | Change rectangle positions, retain logical tabs and navigation set | Live-client programmatic actions and platform behavior need testing |
| Priority collisions | Store an ordered identity list, never change plugin priorities | New-tab placement is an explicit policy, not a minimal-diff algorithm |
| Native default order | Preserve RuneLite's existing logical order | Assumes the supported client still maintains that order |
| Persistence and profiles | Versioned ConfigManager key, pure decode, ConfigChanged subscription | Real profile switching remains on the manual checklist |
| Shared panel classes | Class name plus tooltip, reject exact duplicate IDs | Renames become new identities; a native stable ID would be better |
| Disabled/new plugins | Retain disabled IDs; append or place near natural neighbor | Neither policy can infer every user's preferred position |
| Lifecycle and cleanup | EDT updates; restore original delegate; preserve foreign replacements | Native sidebar redesign is intentionally unsupported |
| Accidental dragging | Movement threshold and lock control | Does not integrate the native configurable drag hotkey |

## Path forward

1. Use the development launcher to complete the live-client checklist, especially
   context menus, programmatic panel opening, profile switching, and platform input.
2. Address reproducible defects and agree on the new-plugin insertion behavior.
3. Obtain a maintainer assessment of the public Swing integration before describing
   this as Plugin Hub-ready. No maintainer contact or Hub submission has been made.
4. If the integration is unsuitable for the Hub, carry the ordering logic and tests
   into a small contribution to the active native effort. Do not revive reflection
   or use a policy exception as the basis of the design.

Automated tests are evidence about behavior, not evidence of maintainer approval.
The native work may eventually make this plugin unnecessary.

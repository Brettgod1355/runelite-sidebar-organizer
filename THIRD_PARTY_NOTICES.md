# Third-party notices

Sidebar Organizer is independently implemented against RuneLite's client API.
It is not an official RuneLite product and has not been approved for the Plugin Hub.

## RuneLite

Development setup follows the official [example plugin](https://github.com/runelite/example-plugin).
RuneLite is a build/runtime dependency, not bundled in the plugin JAR. RuneLite
source files carry their respective BSD 2-Clause copyright notices. The project
uses RuneLite's original navigation buttons and panels without copying their source.

## Gradle wrapper

`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and the wrapper
properties originate from the RuneLite example plugin (template commit
`5370caa0f5f6a5bba4fbb42931722ca535ad3fd5`). Gradle is licensed under Apache 2.0;
see `licenses/Apache-2.0.txt` and the notices retained in the scripts and wrapper.
The wrapper is development tooling and is not included in the plugin JAR.

## Research references

These discussions informed design decisions; their patches were not copied:

- https://github.com/runelite/runelite/pull/18406
- https://github.com/runelite/runelite/pull/19910
- https://github.com/abextm/runelite/tree/sidebar-reorder4

The contributors identified useful concerns about default priorities, button
identity, settings persistence, and client lifecycle. There is no implied
endorsement of this standalone plugin.

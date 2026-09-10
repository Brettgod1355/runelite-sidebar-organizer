# Sidebar Organizer

Arrange RuneLite's sidebar panel icons in your own order.

**Experimental development build.** This plugin arranges tab positions through a
Swing UI delegate. Its runtime code uses no reflection or private-field access.
It is not currently in the Plugin Hub. Native sidebar reordering is also being
developed upstream; see the [research findings](docs/RESEARCH.md) and
[compatibility notes](docs/COMPATIBILITY.md).

## Features

- Drag a sidebar icon onto another icon to move it to that position.
- Open **Sidebar Organizer** for a draggable list and **Up / Down** controls.
- Automatically save the order through RuneLite's ConfigManager.
- Remember positions for disabled plugins when they return.
- Choose whether new tabs appear at the end or near a default-order neighbor.
- Lock dragging to prevent accidental moves; the explicit Up / Down controls remain available.
- Reset to RuneLite's default priority/tooltip order.
- Restore default order on disable, retaining your saved arrangement for re-enable.

The initial version arranges tabs that open plugin panels. Separate utility
buttons, title-bar controls, and in-game tabs are outside its scope. Dragging an
icon initially selects it through RuneLite's normal click behavior; the reorder
itself preserves the selected panel. Drop onto a tab; dropping outside the tabs
cancels the move. No account, server, or data upload is needed by this plugin.

## Run in IntelliJ (Windows, macOS, Linux)

1. Clone this repository and use the `main` branch.
2. Open the repository as a Gradle project in IntelliJ IDEA.
3. Set the project and Gradle JVM to a **JDK 11 or 17**, not a JRE.
4. Let Gradle finish importing, then run the Gradle **run** task.
5. Find **Sidebar Organizer** in the development client's plugin settings and enable it if needed.
6. Drag sidebar icons, or click the gold list icon for the organizer panel.

Command line:

```powershell
# Windows
.\gradlew.bat test build
.\gradlew.bat run
```

```sh
# macOS / Linux
./gradlew test build
./gradlew run
```

The build uses RuneLite's `latest.release` by default. A reproducible compatibility
check can use `./gradlew test -PruneLiteVersion=1.12.38`.

For a Jagex account, use RuneLite's [development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
A game login is not needed to test arranging many of the built-in panels.

## Testing

Run `./gradlew test`. Tests exercise saved ordering, original navigation identity,
selection, click targets, wrapped columns, painting with RuneLite's look and feel,
and actual `ClientUI.addNavigation` / `removeNavigation` lifecycle methods in a
headless fixture.
They do not constitute a complete live-client or Plugin Hub approval test.

The repository owner reported successful Windows live-client checks on September 10,
2026: dragging, correct panel selection, Up / Down, saved order after restart,
plugin disable/re-enable, drag locking, one/two-column resizing, and reset persistence.
See [testing status and remaining checks](docs/TESTING.md) for the tested environment,
automated results, and the limits of that verification.

## License

[BSD 2-Clause](LICENSE). Redistribution must retain the required notices.
See [third-party notices](THIRD_PARTY_NOTICES.md) for development tooling and references.

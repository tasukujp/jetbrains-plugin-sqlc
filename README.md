# jetbrains-plugin-sqlc

Removes false SQL inspection errors for [sqlc](https://sqlc.dev) parameters in JetBrains IDEs.

- `@name`
- `sqlc.arg()` / `sqlc.narg()` / `sqlc.slice()` / `sqlc.embed()`

Errors are suppressed only at parameter positions in files listed under `queries` in `sqlc.yaml`.
Other SQL errors are still reported.

## Requirements

An IDE with the Database Tools and SQL plugin, version 2026.2 or later. Tested on GoLand 2026.2.3.

## Install

Run `./gradlew buildPlugin`, then choose `build/distributions/jetbrains-plugin-sqlc-*.zip` in
Settings > Plugins > ⚙️ > Install Plugin from Disk...

## Limitations

- Only the nearest `sqlc.yaml` / `sqlc.yml` / `sqlc.json` in a parent directory is read.
- `queries` must be file or directory paths. Globs are not supported.
- Only `SqlResolve` and `SqlAmbiguousColumn` are suppressed.

## License

MIT

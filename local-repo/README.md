# Local TC4R dependency

TC4R is a user-supplied local build and is not redistributed. Only `DEPENDENCIES.json` is tracked;
the jar and pom are git-ignored. Copy them from `../Technomancy-1.20.1/local-repo` (or
`../_tc4_1201`, renamed to the Maven layout below), or point the build elsewhere with
`-Ptc4Repository=<maven root>`.

```text
dev/tc4port/thaumcraft-forge/0.1.0-20732/
  thaumcraft-forge-0.1.0-20732.jar
  thaumcraft-forge-0.1.0-20732.pom
dev/tc4port/thaumcraft-neoforge/0.1.0-20736/
  thaumcraft-neoforge-0.1.0-20736.jar
  thaumcraft-neoforge-0.1.0-20736.pom
```

The Maven version (`0.1.0-20732`) differs from the in-game mod version
(`4.2.3.5-1.20.1-port.0.1.0-20732`); `mods.toml` pins the latter exactly.
`gradlew verifyLocalDependencies` checks the SHA-256 of both files.

Each node reads its own manifest (`DEPENDENCIES.json` for 1.20.1, `DEPENDENCIES.1.21.1-neoforge.json`
for 1.21.1). CI receives both trees as one `.tar.gz` whose root holds this directory's contents,
served through the `TC4R_ARCHIVE_URL` repository variable and `TC4R_ARCHIVE_TOKEN` secret; see
`.github/actions/forge-setup`.

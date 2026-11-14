# Argosy Local PC Runner Support

## Current phase

Core persistence and launcher baseline implemented; management UI and end-to-end verification remain.

## Completed

- Created GitHub fork: `https://github.com/JustAQuickName/argosy-launcher`.
- Cloned upstream at `d8408a9197de0725f5a2d18a605b17b107a5e705`.
- Created branch `feature/local-pc-runners`.
- Initialized all pinned submodules.
- Read repository constitution, contributor rules, and required feature/UI/dual-screen skills.
- Cloned current GameNative, liaralabs ReVanced patches, and GameHub Lite references under `research/`.
- Confirmed upstream already has Steam-oriented GameNative/GameHub integration that must remain intact.
- Re-read the repository constitution, contributor rules, coupling map, and required code/UI/device/validation skills on 2026-11-14.
- Verified GameNative public launch and `.pcgame` export contracts from checked-out source; no private database access is required.
- Confirmed Argosy database version 223 and shared game projections for favorites, search, collections, and history.
- Added the version-224 Room schema, migration registry entry, exported schema, local-PC entry/target entities, DAO, and migration instrumentation test.
- Added stable entry and target identities, typed runner targets, default/one-shot resolution, versioned JSON backup codec, and idempotent GameNative marker import primitives.
- Added explicit GameNative, patched full GameHub, Lite, and stock-full capability profiles; stock full is blocked with an unverified-contract diagnostic rather than guessed.
- Integrated local-PC launch dispatch without changing RomM, Steam, emulator resolver, save-sync paths, or private runner databases.
- Committed and pushed the persistence/typed-runner milestone as `3646a8eef` on `origin/feature/local-pc-runners`.
- Added a SAF document service for single marker import, recursive folder preview, explicit unreadable/rejected reporting, confirmed import, versioned backup read, and user-selected backup write. No persisted tree dependency or private app storage is introduced.
- Verified the SAF layer with `:app:compileDebugKotlin` using the complete JDK (PASS, 2m19s cold KSP compile).
- Focused tests were attempted; the first invocation failed before project compilation because the host `JAVA_HOME` selected `/usr/lib/jvm/java-21-openjdk-amd64`, whose installation lacks `bin/jlink`. Resume with the complete toolchain path shown below.
- Verified GitHub credential `JustAQuickName`. Upstream's release skill reserves official releases/version bumps/release builds for `tmgast`; this project will produce only the required side-by-side debug artifact unless maintainer authority is later provided.

## Decisions

- Side-by-side delivery uses Argosy's existing debug application ID `com.nendo.argosy.debug` and debug signing; no release-key assumptions.
- Local library identity, metadata identity, optional file location, and runner targets will remain separate.
- A local game owns zero or more typed launch targets with stable IDs and one optional default target.
- Stock full GameHub, patched full GameHub, Lite, and GameNative are distinct capability profiles.
- No claim of stock GameHub or physical Retroid compatibility will be made without artifact/device evidence.
- Research checkouts are evidence inputs and will not be committed into the fork.
- Import uses user-selected public mapping files and Argosy-owned backups only.
- Local-entry identity and typed target rows remain separate from Steam fields and game file paths.
- This schema addition does not change save-sync entities or paths; migration and schema proof remain mandatory.

## Remaining

- Record the final evidence ledger and compatibility limits.
- Wire import preview/conflict review, SAF launchers, backup file I/O, and artwork selection into the management UI.
- Replace internal launcher diagnostic text with localized actionable UI messages.
- Integrate library/settings/detail workflows with touch, controller, keyboard, and presentation behavior.
- Add unit/migration/UI tests.
- Build, lint, test, install and exercise in emulator where feasible.
- Package APK, checksum, docs, evidence matrix, commit, push, and verify remote HEAD.

## Exact resume commands

```sh
cd /opt/data/argosy-local-pc
sed -n '1,240p' PROJECT_PROGRESS.md
git status --short --branch
/opt/data/bin/project-runtime status /opt/data/argosy-local-pc
JAVA_HOME=/opt/data/.android-toolchain/jdk ./gradlew testDebugUnitTest --tests 'com.nendo.argosy.data.localpc.*'
```

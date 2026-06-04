# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Universal Media Server (UMS) is a cross-platform DLNA/UPnP/HTTP(S) media server written in Java (17), with a React-based web GUI. It streams or transcodes media to renderers (TVs, consoles, phones) using external tools like FFmpeg, MediaInfo, MEncoder, tsMuxeR, and VLC.

## Build & Test Commands

Java 17 and Maven are required. The React client needs Node with Corepack (Yarn 4, see `packageManager` in `react-client/package.json`); the Maven build installs Node/Yarn itself via frontend-maven-plugin.

```bash
mvn clean package                  # full build incl. React client (binary lands in target/)
mvn clean package -DskipTests      # skip tests
mvn verify -P testing              # run tests with platform binaries downloaded (Linux)
mvn verify -P testing-macos        # same, macOS
mvn verify -P testing-windows     # same, Windows
mvn test -Dtest=FormatTest         # single test class
mvn test -Dtest=FormatTest#method  # single test method
mvn verify -P linter -DskipTests   # CheckStyle (CheckStyle.xml, build fails on violations)
```

Platform profiles (`linux-x86_64`, `macos`, `macos-arm`, `windows`, `docker-ubuntu`, etc.) download the matching external binaries (FFmpeg etc.) into `target/bin`. Tests are JUnit 5.

React client (in `react-client/`):

```bash
yarn install
yarn dev                # Vite dev server (port 3000), proxies API to backend on port 9001
yarn build              # tsc + eslint + vite build
yarn lint               # eslint, --max-warnings 0
yarn test:playwright    # E2E tests (see test:prepareserver:* / test:runserver:* scripts and runtestserver.sh)
```

## Architecture

Main entry point: `net.pms.PMS` (`src/main/java/net/pms/`). It boots configuration, the H2 databases, the media scanner, the UPnP media server, and the web GUI server.

Media flow: filesystem → `store/` (`MediaScanner` watches folders, `MediaStore` holds the browse tree, `MediaInfoStore` orchestrates metadata) → `parsers/` (MediaInfoParser, FFmpegParser, JaudiotaggerParser…) extract metadata → persisted in H2 via `database/` (`MediaDatabase` for the library with versioned `MediaTable*` schemas, `UserDatabase` for accounts; Lucene full-text search) → exposed over DLNA/UPnP via `network/mediaserver/` (JUpnp-based) → transcoded if needed by `encoders/` (FFMpegVideo, TsMuxeRVideo, VLCVideo… selected through `EngineFactory`, launched via `io/` process wrappers).

Key packages:

- `configuration/` — `UmsConfiguration` (global settings, UMS.conf), `RendererConfiguration` (per-device profiles)
- `renderers/` — renderer detection and capabilities; ~220 device profile `.conf` files live in `src/main/external-resources/renderers/` (`DefaultRenderer.conf` is the fallback). Adding renderer support usually means adding/editing a `.conf` there.
- `network/webguiserver/` — Jetty servlets forming the REST API under `/v1/` (Auth, Settings, Renderers, Player, WebSocket at `/v1/websocket`) consumed by the React client
- `iam/` — accounts, permissions, JWT auth for the web GUI
- `platform/` — OS-specific code (JNA bindings for Windows/macOS)
- `dlna/` — DLNA protocol details, image profiles, thumbnails

Frontend integration: the React app (React 19, Mantine UI, Vite, TypeScript) is built into `src/main/external-resources/web/react-client/` during the Maven build and served by the backend's GUI servlets. For frontend work, run the backend on port 9001 and use `yarn dev`.

Configuration templates and other runtime resources (UMS.conf, logback.xml, renderer confs) are in `src/main/external-resources/`.

## Code Style

- Tabs for indentation (4-char width), K&R braces — enforced by CheckStyle (`CheckStyle.xml`)
- No star imports, no trailing whitespace; standard Java naming conventions
- See STYLEGUIDE.md (based on Google Java Style Guide with the above deviations)
- When changing files in `/bin`, the `binary-revision` property in pom.xml must be bumped or the Windows installer won't replace them

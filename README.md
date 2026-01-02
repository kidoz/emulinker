# Kaillux

![Java](https://img.shields.io/badge/Java-25-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A?logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/License-GPL--2.0-blue)

Kaillux is a Kaillera network server that enables online multiplayer for
emulators by routing player input over the network.

## Highlights

- Modernized Java 25 + Spring Boot build
- Admin API and health/metrics endpoints for ops visibility
- File-based access control with temporary bans and silencing
- Arch Linux packaging with systemd service

## Quick start

Build the server:

```bash
./gradlew build
```

Run the JAR:

```bash
java -jar build/kaillux.jar
```

## Installation

### Arch Linux

Build and install using the provided PKGBUILD:

```bash
cd packaging/archlinux
makepkg -si
```

Start the service:

```bash
sudo systemctl enable --now kaillux
```

Configuration files are installed to `/etc/kaillux/`.

### Docker

```bash
docker build -t kaillux .
docker run -p 8080:8080 -p 27888:27888/udp -p 27889-27899:27889-27899/udp kaillux
```

### Manual

Run directly with Java 25+:

```bash
java -jar build/kaillux.jar
```

## Configuration

- App config: `src/main/resources/application.properties`
- Access control rules: `src/main/resources/access.cfg`
- Logging: `src/main/resources/logback.xml`

For packaged installations (Arch Linux), config files are in `/etc/kaillux/`.

## Architecture

### Package Structure

The codebase has two package hierarchies reflecting its evolution:

- **`org.emulinker.*`** - Original EmuLinker codebase (maintained for stability)
- **`su.kidoz.*`** - New and refactored components

**Guidelines for contributors:**

| Scenario | Package to Use |
|----------|----------------|
| New classes | `su.kidoz.*` |
| Bug fixes in existing code | Keep original package |
| Refactoring existing code | Move to `su.kidoz.*` if significant changes |
| Tests for new code | Mirror the source package |

**Migrated components:**

- Access control: `su.kidoz.kaillera.access.*` (pattern matching, timed rules)
- Model helpers: `su.kidoz.kaillera.model.impl.*` (UserManager, GameManager)
- Services: `su.kidoz.kaillera.service.*` (ChatModerationService, AnnouncementService)
- Validation: `su.kidoz.kaillera.model.validation.*` (LoginValidator)
- Controller: `su.kidoz.kaillera.controller.v086.*` (V086ClientHandler)

New packages mirror the legacy structure (e.g., `org.emulinker.kaillera.model` → `su.kidoz.kaillera.model`).

## Admin, health, and metrics

- Admin API: `/api/admin/**` (HTTP Basic; set `admin.username`/`admin.password`)
- Health probes: `/healthz`, `/livez`, `/readyz`
- Prometheus metrics: `/metrics`

These endpoints are intended for cluster-internal access only.

## License

GPL-2.0. See `LICENSE`.

## Credits

- **Current Maintainer:** Aleksandr Pavlov
- **Original Author:** Paul Cowan
- **Contributors:** See [GitHub contributors](../../graphs/contributors)

# Changelog

All notable changes to HIAM (Helix Identity and Access Management) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.4] - 2026-04-05

### Fixed

- Refactored class names to remove redundancy and improve readability:
  - Plugin entry points: `HelixIAMVelocity` → `VelocityPlugin`, etc.
  - Command handlers: `HiamCommand` → `AuthCommand`
  - Listeners: `ServerPreConnectEventListener` → `ProxyConnectListener`

### Changed

- Cleaned up documentation throughout for clarity

## [2.3.3] - 2026-04-05

### Added

- Support for Java 17, 21, and 25 with separate optimised builds for each version
- Virtual thread executors to make database queries faster on Java 21:
  - `VirtualThreadExecutor` in hiam-velocity
  - `ConcurrentCommandExecutor` in hiam-paper
  - `GateAuthenticationExecutor` in hiam-paper-gate
- Pattern matching helpers for Java 25:
  - `AuthStatePatternMatcher`
  - `CommandSenderMatcher`
  - `GateEventMatcher`
- Maven profiles for building against different Java versions:
  - Default builds against Java 17
  - `-P java21` for Java 21 features
  - `-P java25` for Java 25 features

### Changed

- Kept groupId as `uk.co.keirahopkins` (original domain)
- Artifact names now include the Java version: `hiam-velocity-2.3.3-java17.jar`, etc.
- Added proper licensing, source control, and issue tracking info to the POM
- Updated README with Java version info

### Fixed

- Maven configuration for consistent builds across Java versions
- Build-helper plugin paths for version-specific source directories

### Technical Details

- Java 21 builds use virtual threads for better I/O handling
- Java 25 builds use pattern matching for cleaner code
- Full backward compatibility with Java 17

## [2.3.2] - 2026-02-09

### Added

- Set up the HIAM codebase and build system from scratch
- Three-module architecture:
  - **hiam-velocity**: Core authentication proxy running on Velocity
  - **hiam-paper**: Authentication server plugin for Paper instances
  - **hiam-paper-gate**: Optional lightweight gate plugin for perimeter auth checks
- Full authentication system for offline Velocity networks
  - Register and login with `/register` and `/login` commands
  - Premium UUID authentication with `/premium` command
  - Session management with configurable TTLs and IP-based locking
  - Rate limiting and brute-force protection
  - 30-second single-use confirmation tokens for security-critical operations
- Database layer
  - PostgreSQL with HikariCP connection pooling
  - Argon2id for password hashing
  - Account and session persistence
- Plugin messaging via `hiam:auth` channel
  - Bidirectional communication between Velocity and Paper servers
  - Paper sends auth requests (`register`, `login`, `premium`, etc.)
  - Velocity sends responses (success/fail, freeze/unfreeze, teleport commands)
  - Synchronizes auth state across the entire network
  - Uses binary serialization for efficiency
- Command system
  - Full set of commands for players and admins
  - Command completion and permission checks
  - Uses Adventure API for rich chat components
- Configuration
  - YAML config files for easy customisation
  - Spawn point management
  - Customisable auth prompts
- Build and releases
  - GitHub Actions for automated builds
  - Automated release pipeline

### Technical Details

- Java 21 for modern language features
- Maven 3.9+ to manage dependencies and builds
- Paper API 1.21.4+ for Minecraft server integration
- Velocity API 3.3.0+ for proxy-level authentication
- SLF4J with Spring Cloud Config support for logging

## [Unreleased]

### Planned

- Additional authentication methods (OAuth 2.0, SAML integration)
- Enhanced session analytics and activity tracking
- WebSocket-based real-time authentication status updates
- Multi-factor authentication support
- Customisable authentication flows

---

For information about specific features, implementation details, and usage instructions, please refer to the [README.md](README.md).

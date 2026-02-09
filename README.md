# Helix IAM

A Java 21 Maven authentication system for Velocity + Paper networks.

## Overview

Helix IAM provides secure authentication for offline-mode Velocity networks with support for:
- Password-based authentication for cracked/Eaglercraft users (`/register`, `/login`)
- Java-only no-password premium mode via `/premium`
- Session management with TTLs and IP locking
- Rate limiting and brute-force protection
- 30-second single-use confirmation tokens for security-critical operations
- Clickable chat prompts using Adventure API

## Architecture

### Modules

- **hiam-velocity**: Core authentication logic running on Velocity proxy
  - Source of truth for accounts and sessions
  - PostgreSQL storage with HikariCP connection pooling
  - Argon2id password hashing
  - Session management and validation
  - Client detection (Java vs Eaglercraft)
  
- **hiam-paper**: Required auth server plugin
  - Spawn management and player freeze mechanics
  - Command handlers for `/register`, `/login`, `/changepass`, `/premium`, `/offline`
  - Admin commands for account management
  - Plugin messaging with Velocity
  
- **hiam-paper-gate**: Optional lightweight gate plugin
  - Checks authentication state via plugin messaging
  - Kicks unauthenticated players on join
  - Permission-based bypass support

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 12+
- Velocity 3.3.0+
- Paper 1.21.4+

## Database Setup

Create a PostgreSQL database and user:

```sql
CREATE DATABASE helixiam;
CREATE USER hxnt_srv WITH ENCRYPTED PASSWORD 'QvnPkJUszIJH4LVyIBhjjlo+BUBB2gBk7V+b5DeIN+4iRGbOoRkFw7+XY9t/38v0fJluFBJksSZ1qytxWMnFDw';
GRANT ALL PRIVILEGES ON DATABASE helixiam TO hxnt_srv;
```

The schema will be automatically created on first startup from `001-init.sql`.

## Building

```bash
export JAVA_HOME=/path/to/java21
mvn clean package
```

Built JARs will be in:
- `hiam-velocity/target/Helix IAM - Velocity-1.0.0-SNAPSHOT.jar`
- `hiam-paper/target/Helix IAM - Paper Auth Server-1.0.0-SNAPSHOT.jar`
- `hiam-paper-gate/target/Helix IAM - Paper Gate-1.0.0-SNAPSHOT.jar`

## Installation

### Velocity Setup

1. Place the Velocity plugin in `velocity/plugins/`
2. Start the server to generate `plugins/hiam-velocity/config.yml`
3. Configure database connection:
```yaml
database:
  host: "localhost"
  port: 5432
  name: "helixiam"
  user: "hxnt_srv"
  password: "your_password"
```
4. Configure server routing:
```yaml
routing:
  authServer: "auth"
  postLoginTarget: "lobby"
  fallbackServer: "survival"
```
5. Restart Velocity

### Paper Auth Server Setup

1. Place the Paper plugin on your auth server in `plugins/`
2. Configure spawn locations in `plugins/HelixIAM-Paper/config.yml`
3. Set spawn points in-game:
   - `/setloginspawn` - Where players spawn before auth
   - `/setmainspawn` - Where players spawn after auth (optional)

### Paper Gate Setup (Optional)

1. Place the gate plugin on lobby/survival servers in `plugins/`
2. No configuration needed - it communicates with Velocity automatically

## Commands

### Player Commands

- `/register <password> <confirm>` - Register a new account
- `/login <password>` - Login to your account
- `/changepass <old> <new> <confirm>` - Change your password
- `/premium` - Enable premium (no-password) mode (Java only, requires confirmation)
- `/offline` - Switch to offline (password) mode (requires confirmation)

### Admin Commands

- `/authinfo <player>` - View authentication info
- `/setloginspawn` - Set the login spawn point
- `/setmainspawn` - Set the post-auth spawn point
- `/hiam info` - View plugin information
- `/hiam admin forcereset <player>` - Reset account completely
- `/hiam admin resetpassword <player>` - Set a new password for a player
- `/hiam admin clearpassword <player>` - Clear a player's password
- `/hiam admin setpremium <player>` - Set a player to premium mode
- `/hiam admin setoffline <player>` - Set a player to offline mode
- `/hiam admin confirm <token>` - Confirm a pending action
- `/hiam admin cancel <token>` - Cancel a pending action

## Permissions

### Player Permissions
- `hiam.player.register` - Use /register (default: true)
- `hiam.player.login` - Use /login (default: true)
- `hiam.player.changepass` - Use /changepass (default: true)
- `hiam.player.premium` - Use /premium (default: true)
- `hiam.player.offline` - Use /offline (default: true)

### Admin Permissions
- `hiam.admin.authinfo` - Use /authinfo (default: op)
- `hiam.admin.setspawn` - Set spawn points (default: op)
- `hiam.admin.forcereset` - Force reset accounts (default: op)
- `hiam.admin.resetpassword` - Reset passwords (default: op)
- `hiam.admin.clearpassword` - Clear passwords (default: op)
- `hiam.admin.setpremium` - Set premium mode (default: op)
- `hiam.admin.setoffline` - Set offline mode (default: op)
- `hiam.info` - View plugin info (default: op)

### Gate Permissions
- `hiam.gate.bypass` - Bypass auth gate checks (default: op)

## Features

### Account Modes

**OFFLINE Mode**: Password-based authentication
- Used for cracked/Eaglercraft clients
- Required password login
- Password hashed with Argon2id

**PREMIUM Mode**: Java-only, no password
- Requires verified Java Edition client
- UUID binding for security
- No password needed

### Security Features

- **Argon2id Password Hashing**: Industry-standard password hashing with full parameter storage
- **Rate Limiting**: Configurable max attempts and lockout duration
- **Session Management**: TTL-based sessions with automatic expiration
- **IP Locking**: Optional IP-based session validation
- **Confirmation Tokens**: 30-second single-use tokens for security-critical operations
- **SQL Injection Protection**: All queries use prepared statements

### Client Detection

Automatically detects Java Edition vs Eaglercraft clients:
- Java users are offered premium mode after registration
- Eaglercraft users never see premium prompts
- Premium mode enforced only for Java Edition

### Player Freeze System

Unauthenticated players are frozen and cannot:
- Move (position locked, not head rotation)
- Execute commands (except whitelisted auth commands)
- Access inventory
- Take or deal damage
- Interact with blocks, entities, or items
- Send chat messages

### Authentication Flow

1. Player connects to Velocity
2. Velocity routes to auth server
3. Player is frozen and teleported to login spawn
4. Player registers or logs in
5. On success, Velocity creates session and routes to lobby/survival
6. Player is unfrozen and can play normally

### Admin Features

- View detailed account information
- Force reset accounts
- Reset or clear passwords
- Change account modes (premium/offline)
- All dangerous operations require confirmation with clickable buttons

## Configuration

### Velocity Config (`plugins/hiam-velocity/config.yml`)

Key settings:
- `database.*` - PostgreSQL connection
- `session.ttlMinutes` - Session expiration (default: 720 = 12 hours)
- `session.ipLock` - Enable IP-based session validation
- `login.timeoutSeconds` - Auto-kick timeout (default: 60)
- `login.maxAttempts` - Max login attempts (default: 5)
- `login.lockoutMinutes` - Lockout duration (default: 15)
- `routing.*` - Server routing configuration
- `premium.enableAutoPrompt` - Show premium prompt after registration
- `commands.allowedPreAuth` - Commands allowed before authentication
- `security.minPasswordLength` - Minimum password length (default: 8)

### Paper Config (`plugins/HelixIAM-Paper/config.yml`)

Key settings:
- `auth.spawn.login` - Login spawn location
- `auth.spawn.post` - Post-auth spawn location
- `auth.freeze.*` - Freeze restrictions
- `messaging.channel` - Plugin messaging channel (must match Velocity)
- `messages.*` - Customizable player messages

## Database Schema

### Tables

**accounts**
- `id` - UUID primary key
- `username` - VARCHAR(16) unique (lowercase)
- `uuid` - UUID (for premium users)
- `mode` - ENUM('OFFLINE','PREMIUM')
- `password_hash` - VARCHAR(255) (Argon2id hash)
- `created_at`, `updated_at`, `last_ip`, `last_login_at` - Tracking fields

**sessions**
- `session_id` - UUID primary key
- `account_id` - Foreign key to accounts
- `issued_at`, `expires_at` - Session lifetime
- `ip` - INET (for IP locking)
- `user_agent` - TEXT
- `is_premium` - BOOLEAN

**login_attempts**
- Rate limiting and attempt tracking
- Automatic cleanup after 1 hour

## State Machine

Player authentication states:
- `NEW` - New player, needs to register
- `OFFLINE_REGISTERED` - Has account with password
- `PREMIUM_PENDING` - Transitioning to premium
- `PREMIUM_ACTIVE` - Premium mode enabled
- `AUTHENTICATED` - Currently logged in
- `LOCKED` - Account locked by admin

## Development

Built with:
- Java 21
- Maven 3.9+
- HikariCP for connection pooling
- PostgreSQL JDBC driver
- Argon2-JVM for password hashing
- Adventure API for chat components
- SLF4J for logging
- Velocity API 3.3.0
- Paper API 1.21.4

## License

Copyright (c) 2024 Keira Hopkins. All rights reserved.

## Support

For issues and feature requests, please contact the project maintainer.

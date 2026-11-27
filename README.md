# Superpower Mod

Two cinematic abilities for **Minecraft Java Edition 1.21.11 (Fabric)**: **Sky Slam** and **Titan Throw**.

> **Status: this project has not been compiled against Minecraft or tested in game.**
> The machine it was written on could not download Minecraft or Fabric. Your first build is the first real
> compile. If it fails, copy the whole error output and send it back. See
> [What has been checked](#what-has-been-checked).

## The abilities

| Key | Ability | What happens |
|-----|---------|--------------|
| **R** | Sky Slam | Crouch, launch about 22 blocks, front flip, dive, three-point landing. A shockwave ring spreads out, nearby players are launched away and a shallow crater breaks open. |
| **G** | Titan Throw | Aim at a player within 4.5 blocks. You grab and lift them, swing them over your shoulder and throw them about 30 blocks on level ground. They tumble through the air, crash on their back and get up. |

* Every player on the server can use both. Keys can be changed under **Options > Controls > Key Binds > Superpowers**.
* **Aiming the slam:** look at a spot while you are at the top of the jump to dive towards it (up to 40 blocks
  away). Look at the sky to drop straight down.
* **Aiming the throw:** you throw in the direction you face while holding. Look up for a higher, shorter throw.
* Neither ability causes fall damage, and neither deals damage unless you enable it in the config.
* Cooldowns are 8 s and 10 s. Pressing a key too early shows the time left above the hotbar.

## 1. Build the JAR

You need:

* **JDK 25** (for example [Eclipse Temurin](https://adoptium.net/) or the Microsoft Build of OpenJDK). The mod
  itself targets Java 21.
* An internet connection. The first build downloads Gradle, Minecraft, Fabric and the animation library, and can
  take several minutes.

Extract the ZIP, open a terminal in the `SuperpowerMod` folder (the one containing `build.gradle`), then run:

```bat
:: Windows (Command Prompt)
gradlew.bat build
```

```bash
# macOS / Linux (PowerShell users: .\gradlew.bat build)
chmod +x gradlew
./gradlew build
```

Your mod is `build/libs/superpowermod-1.0.0.jar`.

If Gradle says it is running on the wrong Java version, point `JAVA_HOME` at your JDK 25 and run the command again.

**No local setup?** Push the folder to a GitHub repository. The included workflow
(`.github/workflows/build.yml`) builds it and attaches the JAR to the run under **Actions**.

## 2. Install

Put these in the `mods` folder of **the server and every player**:

| Mod | Version | Required |
|-----|---------|----------|
| `superpowermod-1.0.0.jar` | the file you built | yes |
| [Fabric API](https://modrinth.com/mod/fabric-api) | for 1.21.11 | yes |
| [Player Animation Library](https://modrinth.com/mod/player-animation-library) | 1.1.12 for 1.21.11, Fabric | yes |
| Bendable Cuboids | for 1.21.11, Fabric | players only, recommended |

Also install [Fabric Loader](https://fabricmc.net/use/) for 1.21.11 (0.18.3 or newer).

**Bendable Cuboids** adds elbow and knee bends. The animations include bend keyframes for it. Without it the arms
and legs stay straight and everything else plays the same. For filming, install it.

Singleplayer works for Sky Slam. **Titan Throw needs a second player** (a server, or Open to LAN).

### Testing from the project folder

```bash
./gradlew runClient                              # singleplayer test of Sky Slam
./gradlew runServer                              # first run stops: set eula=true in run/eula.txt
./gradlew runClient --args="--username Hero"     # in two more terminals, then join "localhost"
./gradlew runClient --args="--username Victim"
```

The development accounts are offline accounts, so set `online-mode=false` in `run/server.properties` for this
local test only. These dev clients do not include Bendable Cuboids.

## 3. Configure

### Server: `config/superpowermod.json`

Created on first start. A copy with the defaults is in `config-example/`. Save your edit and the next ability use
picks it up; no restart is needed. A file with a JSON mistake is reported in the log and ignored.

| `skySlam` setting | Default | Meaning |
|---|---|---|
| `cooldownSeconds` | 8 | |
| `jumpHeight` | 22 | Blocks risen before the dive (4 to 60) |
| `diveSpeed` | 2.4 | Blocks per tick |
| `aimRange` | 40 | Dive aiming distance; 0 = always straight down |
| `shockwaveRadius` | 9 | Reach of knockback and ring |
| `knockbackStrength` / `knockbackUpward` | 1.8 / 0.9 | Sideways and upward launch speed |
| `shockwaveDamage` | 0 | Health points (2 = one heart) |
| `breakBlocks` | true | Set false to protect your set completely |
| `blockRadius` / `blockDepth` | 4 / 2 | Crater size |
| `maxBlocks` | 250 | Most blocks one slam can break |
| `maxBlockHardness` | 25 | Harder blocks survive (obsidian is 50) |
| `dropBrokenBlocks` | false | Drop items from broken blocks |

| `titanThrow` setting | Default | Meaning |
|---|---|---|
| `cooldownSeconds` | 10 | |
| `grabRange` | 4.5 | Blocks |
| `holdTicks` | 30 | Hold time before the throw, 10 to 50 (20 = 1 second) |
| `throwStrength` / `throwUpward` | 2.8 / 0.9 | Forward and upward launch speed |
| `landingDamage` | 0 | Health points |

| `protection` setting | Default | Meaning |
|---|---|---|
| `protectBlockEntities` | true | Chests, furnaces, signs, beds, spawners and similar never break |
| `protectedBlocks` | `[]` | Extra ids, e.g. `["minecraft:diamond_block"]` |
| `protectNeighbours` | true | Also spares blocks touching a protected block |

The crater never removes unbreakable blocks such as bedrock, water or lava source blocks, anything in the
`superpowermod:protected` block tag (doors, rails, anvils and a few more; extend it with a data pack), or blocks
the player may not break (Adventure mode, spawn protection, and claim mods that use Fabric's block break events).
Above ground level only plants and other walk-through blocks are removed, so walls stay standing.

**Block destruction is permanent.** Back up your world before filming.

### Player: `config/superpowermod-client.json`

Read when the game starts.

| Setting | Default | Meaning |
|---|---|---|
| `cameraShake` | true | Shake on impacts |
| `cameraShakeStrength` | 1 | 0 to 4 |
| `firstPersonArms` | true | Show your animated arms in first person while grabbing and throwing |
| `particleDensity` | 1 | 0.25 to 2 |

### Filming tips

* Film from a third camera account or in third person (**F5**). The poses are designed to be seen from outside.
* A spectator or distant camera sees the particles up to 128 blocks from the impact.
* For retakes, set both `cooldownSeconds` to 0.

## Animations

Eight keyframe files in `src/main/resources/assets/superpowermod/player_animations/`:

| File | Used for | Plays |
|---|---|---|
| `slam_jump.json` | crouch and launch | once, holds last pose |
| `slam_fall.json` | front flip, then dive | loops the dive |
| `slam_land.json` | three-point landing and rise | once |
| `titan_grab.json` | lunge, grab, lift | loops the strain |
| `titan_throw.json` | wind-up, release at 0.5 s, follow-through | once |
| `target_held.json` | victim kicking in the grip | loops |
| `target_flying.json` | victim tumbling | loops |
| `target_land.json` | victim crashing and getting up | once |

The abilities animate each player's own model and skin, so the mod ships **no separate 3D model files**.

If you change an animation's timing, update `common/Timing.java` to match. If you move the throwing arm, also
update `common/GripPath.java`, which places the victim relative to the thrower.

## Project layout

```
src/main/java/com/superpowermod/
  ability/   server logic: sessions, shockwave, block protection, targeting
  client/    animations, particles, camera shake, keys
  common/    cues, timings, grip path, jump physics (shared by both sides)
  config/    server config
  network/   three packets: activate, animation cue, impact effect
  mixin/     one client mixin for camera shake
src/main/resources/
  fabric.mod.json, superpowermod.mixins.json
  assets/superpowermod/  animations, sounds (synthesised for this mod), language, icon
  data/superpowermod/    protected block tag
```

The server decides everything (targets, cooldowns, movement, blocks) and tells nearby clients which animation each
player is in. Player movement in Minecraft is run by the player's own game, so the server sends velocities and the
clients position a grabbed player each tick.

## Known limitations

* Not compiled or played yet (see below). Timing and positioning may need tuning after you see it in game.
* With high ping, a held or thrown player may jitter, and the camera account sees them slightly delayed.
* A grabbed player cannot break free.
* After an ability, you may need to press your movement key again.
* First person shows arms only during grab and throw; the other animations are visible in third person.
* Fabric servers only. Paper and Spigot are not supported. Anti-cheat plugins or mods may object to the movement.
* Other mods that animate or move players may conflict.

## What has been checked

Done:

* All Java sources compile with `javac` against **stand-in classes** written for this check. This catches mistakes
  inside the mod's own code. It does **not** prove that Minecraft and Fabric method signatures are right.
* All eight animations load and play in the Player Animation Library's own loader and playback code, compiled
  from its 1.21.11 source. Poses were previewed with a custom renderer, not in Minecraft.
* Config loading, jump height maths and one-shot animation lengths against the Java timings pass small tests.
* Many Minecraft and Fabric names were compared with publicly available source and documentation.

Not done:

* A real Gradle build, a game launch, a mixin check, multiplayer testing, or listening to the sounds in game.

If the build fails, likely places are `ability/Sync.java` (velocity packet), `mixin/CameraMixin.java` and the
`client/` package. Send the full error text.

## Rights

The code, animations, sounds and icon were created for your project; pick the licence you want in
`fabric.mod.json`. The Gradle wrapper (Apache-2.0) was copied from Fabric's example mod. Dependencies keep their own
licences.

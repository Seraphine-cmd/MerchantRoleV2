# MerchantRole

A task-driven rank progression plugin for **Paper/Spigot 26.3 (Java 25)**, built for servers running **QuickShop-Hikari**, **WorldGuard** and **WorldEdit**.

Players start as **Peasants** and work through:

```text
Peasants
  -> Miner I -> Miner II
  -> Fighter I -> Fighter II
  -> Explorer I -> Explorer II
  -> Farmer I -> Farmer II
```

Each `/roles progress` view shows the task list for the **next** rank. For example, a Peasant sees the Miner I tasks, and a Miner I sees the Miner II tasks.

## Build

```sh
cd ~/Desktop/MerchantRole
./gradlew build
```

The ready-to-install jar is:

```text
build/libs/MerchantRole-1.0.0.jar
```

Requires JDK 25. A Gradle wrapper is included.

## Install

1. Put the jar in your server's `plugins/` folder.
2. Install **QuickShop-Hikari**, **WorldGuard** and **WorldEdit**.
3. Start the server. `plugins/MerchantRole/config.yml` is created on first run.
4. Optionally install **LuckPerms** (rank groups), **GriefPrevention** (claim-block rewards) and a Vault-compatible economy plugin for money costs/rewards.

The plugin depends on `WorldGuard`, `WorldEdit` and `QuickShop-Hikari` at startup. LuckPerms, GriefPrevention and Vault are optional.

## Commands

| Command | Description |
| --- | --- |
| `/roles progress` | Show the next rank's unfinished/finished tasks. White = unfinished, green = finished. |
| `/roles upgrade` | Advance from rank I to rank II (and Peasants -> Miner I). |
| `/roles rankup` | Advance to the next profession (Miner II -> Fighter I, etc.). |
| `/roles start` | Show the current rank and task list. |
| `/roles reload` | Reload `config.yml` without restarting the server. |
| `/roles help` | Command help. |

`/MerchantRole` is an alias for `/roles`. Commands are case-insensitive.

### Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `merchantrole.use` | `true` | Use the player commands. |
| `merchantrole.admin` | `op` | Use `/roles reload`. |

## Costs and rewards

Costs are configured in `config.yml`:

- First upgrade (Peasants -> Miner I): free by default.
- Each I -> II upgrade: **$5,000**.
- Rankups start at **$10,000** and increase by **$5,000** each time:
  - Miner II -> Fighter I: $10,000
  - Fighter II -> Explorer I: $15,000
  - Explorer II -> Farmer I: $20,000
- Rewards are separate from costs and default to the requested values:
  - Fighter I: $10,000
  - Explorer I: $15,000
  - Farmer I: 200 claim blocks (via GriefPrevention)
- Every reward and cost can be changed in `config.yml`.

Money is handled through QuickShop-Hikari's economy provider, with a Vault fallback. If no economy is available, paid transitions are blocked with a clear message; free transitions still work.

Claim-block rewards require GriefPrevention. If it is missing, the reward is saved as pending and granted automatically once GriefPrevention is available.

## Task tracking notes

- **Smelting:** taking iron ingots out of a furnace/blast furnace counts toward the raw-iron task.
- **Diamonds:** breaking diamond ore/deepslate diamond ore counts as one ore; holding 64 diamonds also completes the task.
- **Raw ores:** the plugin remembers the highest amount of raw iron, raw gold, raw copper and lapis lazuli you have held at once.
- **Obtain/collect tasks** use the highest amount ever seen in the player's inventory, so spending the item later does not remove progress.
- **Armor trims:** distinct trim templates in the inventory and distinct trim patterns on armor both count.
- **Pottery sherds:** distinct sherd items in the inventory count.
- **Saplings:** both planting and growing are tracked. A sapling counts as grown when its tree grows.
- **Trial Chamber:** right-clicking a vault marks the Trial Chamber as looted.
- **Ominous Vaults:** counted when right-clicking a vault while holding an Ominous Trial Key.
- **Sniffer eggs:** hatching a Sniffer credits the player who placed the egg (or the nearest player if the egg was placed another way).
- **Mobs for Fighter II:** hostile mobs (zombies, skeletons, creepers, spiders, slimes, etc.). The killing blow must be made with a Diamond Sword carrying Sharpness V, Fire Aspect II, Looting III, Mending, Unbreaking III and Sweeping Edge III.

## WorldGuard region gate (optional)

Set this in `config.yml` to require players to stand inside a WorldGuard region when using `/roles upgrade` or `/roles rankup`:

```yaml
settings:
  rankup-region: "rankup"
```

Leave it blank to allow rank changes anywhere.

## Configuration

- `requirements:` changes task goals (for example `miner-i.smelt-raw-iron: 32`).
- `economy:` changes upgrade/rankup prices.
- `rewards:` changes money, claim-block and console-command rewards.
- `luckperms.groups:` maps each rank to a LuckPerms group. Set `luckperms.enabled: false` to use internal ranks only.
- `settings.sync-groups-on-join:` keeps LuckPerms in sync when players join.
- `settings.inventory-scan-interval-ticks:` controls how often inventory-based tasks refresh.

Player progress is stored in `plugins/MerchantRole/players.yml`. Deleting a player's section resets that player.

## Project layout

```text
src/main/java/com/bap80/merchantrole/
├── MerchantRolePlugin.java       # plugin lifecycle, rewards, tasks
├── RolesCommand.java             # /roles command and tab completion
├── Rank.java                     # rank chain
├── TaskRegistry.java             # all task definitions
├── ProgressService.java          # task evaluation and inventory scanning
├── PlayerProfile.java            # per-player counters/flags/sets
├── PlayerDataStore.java          # players.yml persistence
├── EconomyService.java           # QuickShop-Hikari/Vault bridge
├── ClaimRewardService.java       # optional GriefPrevention bridge
├── WorldGuardHook.java           # optional region gate
├── LuckPermsHook.java            # optional group sync
└── *Listener.java                # Bukkit event trackers
src/main/resources/
├── plugin.yml
└── config.yml
```

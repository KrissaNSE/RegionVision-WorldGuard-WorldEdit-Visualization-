# 👁️ RegionVision
### The Ultimate WorldGuard & WorldEdit Visualization Tool

![Version](https://img.shields.io/badge/Minecraft-1.20%20--%201.21-brightgreen) ![License](https://img.shields.io/badge/License-MIT-blue) ![Performance](https://img.shields.io/badge/Performance-Async-orange)

![RegionVision Banner](https://proxy.spigotmc.org/c7c23298cf46273eb154f6c8f9359189f2ae6ece/68747470733a2f2f692e696d6775722e636f6d2f646448397a48472e6a706567)

**RegionVision** revolutionizes how players and admins interact with server regions. Stop guessing where a PvP zone ends or where your WorldEdit selection begins. Built specifically for **Minecraft 1.21** (with 1.20 support) using modern asynchronous architecture, it provides immediate, high-performance visual feedback without dragging down your server's TPS.

---

## ✨ Key Features

### 🛡️ Dynamic Permission Visualization
Automatically visualize the WorldGuard region you are currently standing in.
* **Green Particles:** You have build rights.
* **Red Particles:** You are denied build rights.

### 🪓 Real-Time WorldEdit Feedback
Forget typing `//pos1` blindly.
* Hold your **Wooden Axe** to see your selection instantly.
* Displays a crisp **Purple Wireframe**.
* Updates in **real-time** as you modify your selection.
* *Note: Particles vanish when you switch items to keep your view clean.*

### 📍 Permanent Admin Regions
Mark critical areas (Spawns, Hubs, Arenas) to be permanently visible to players.
* **Customizable Colors:** Set unique RGB colors for every region.
* **Performance Mode:** Smart view distances ensure particles only render when necessary.
* **Entry Notifications:** Inform players via **Boss Bar**, **Action Bar**, or **Title** text when entering zones.

### ⚡ Performance First
RegionVision is built with an **Async-First architecture**.
* All geometric calculations handled off the main thread.
* Smart caching prevents recalculating static regions.
* Race-condition protection ensures no "ghost" particles remain.

---

## 📥 Installation

1.  Download `RegionVision.jar` from the [SpigotMC Page](https://www.spigotmc.org/resources/regionvision-worldguard-worldedit-visualization.130649/).
2.  Ensure you have **WorldGuard** and **WorldEdit** installed.
3.  Place the jar in your server's `plugins/` folder.
4.  Restart your server.

---

## 💻 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/rv show` | Visualize the region you are standing in. | `regionvision.use` |
| `/rv clear` | Clear all active particles immediately. | `regionvision.use` |
| `/rv info` | View Owners, Members, and Flags. | `regionvision.info` |
| `/rv toggle selection` | Toggle auto-WorldEdit visualization. | `regionvision.toggle` |
| `/rv view <region>` | Force view a remote region. | `regionvision.view.remote` |
| `/rv near [radius]` | Visualize all nearby regions. | `regionvision.near` |
| `/rv perm ...` | Admin commands for permanent regions. | `regionvision.admin` |
| `/rv reload` | Reload configuration. | `regionvision.admin` |

### Admin Sub-commands
* `/rv perm add <name>` - Make a region permanently visible.
* `/rv perm color <name> <RGB>` - Set custom particle color.
* `/rv perm density <name> <0.1-1.0>` - Adjust particle density.
* `/rv perm message <name> <TYPE> <msg>` - Set entry notification.

---

## ⚙️ Configuration

Fully customizable `config.yml` allowing you to tweak performance and visuals.

```yaml
visualizer:
  # Duration (seconds) static particles remain visible
  duration: 15
  # Particle density (0.25 = High Quality, 1.0 = Performance)
  particle-density: 0.25
  # Safety cap for /rv near
  max-near-radius: 100

colors:
  allowed: {r: 0, g: 255, b: 0}
  denied: {r: 255, g: 0, b: 0}
  selection: {r: 200, g: 0, b: 255}
```

---

## 🤝 Contributing & Support

Found a bug or have a feature request?

* Join my [Discord](https://discord.gg/wqtJ9dWCnZ) for support.

**Created by Krissan**

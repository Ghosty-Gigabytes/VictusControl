# VictusControl

A Linux utility for controlling keyboard backlight and fan speeds on HP Victus laptops. Runs as a **systemd daemon** and is controlled via a lightweight **CLI tool** over Unix Domain Sockets.

> Tested on **HP Victus 16-s0095ax**. Should work on most HP Victus 16 models.

---

## Features

### Keyboard Backlight
| Command | Description |
|---|---|
| `rainbow <brightness> <delay>` | Smooth cycling rainbow effect |
| `static <r> <g> <b> <brightness>` | Fixed RGB color at specified brightness |
| `off` | Turn off keyboard backlight |
| `brightness <value>` | Adjust brightness without changing mode |

### Fan Control
| Command | Description |
|---|---|
| `auto` | Hand control back to BIOS |
| `max` | Run fans at maximum speed |
| `manual <fan1_rpm> <fan2_rpm>` | Set independent target RPM for each fan |

---

## Requirements

- **Java 21+**
- **Maven 3.8+**
- **HP Victus 16 laptop**
- **[hp-wmi-fan-and-backlight-control](https://github.com/TUXOV/hp-wmi-fan-and-backlight-control)** kernel module — required to expose HP WMI controls under `/sys/class` and `/sys/devices/platform/hp-wmi/`

### Install the kernel module first

Follow the instructions in [hp-wmi-fan-and-backlight-control](https://github.com/TUXOV/hp-wmi-fan-and-backlight-control) before proceeding. VictusControl will not work without it.

After installing the module, verify these paths exist:

```bash
ls /sys/class/leds/hp::kbd_backlight/
ls /sys/devices/platform/hp-wmi/hwmon/
```

---

## Architecture

```
CLI (victus-ctl)
      │
      │  Unix Domain Socket
      │  /run/victus-control/victus.sock
      ▼
Daemon (rgbd) ── systemd service
      │
      ├── SocketListener thread   listens for incoming commands
      │         │
      │         └── updates DaemonState (shared volatile fields)
      │
      ├── Keyboard thread         polls DaemonState every 100ms
      │         │
      │         ├── Rainbow task (ExecutorService) → writes to hp::kbd_backlight/
      │         └── Static / Off                  → writes to hp::kbd_backlight/
      │
      └── Fan thread              polls DaemonState every 100ms
                │
                ├── MAX  → pwm1_enable = 0
                ├── AUTO → pwm1_enable = 2
                └── ManualMode task (ExecutorService) → writes fan1_target, fan2_target
                          └── enforcement loop: verifies RPM and retries if BIOS fights back
```

### Shared State Architecture

All threads communicate through a single `DaemonState` object with `volatile` fields. No locks needed — the Keyboard and Fan threads poll for changes every 100ms, and the SocketListener writes new values directly. The `volatile` keyword guarantees visibility across threads without expensive synchronization.

```
DaemonState (volatile fields)
├── rgbMode, r, g, b, brightness, rgbSpeed
└── fanMode, fan1_target, fan2_target, fan1_input, fan2_input
```

### IPC via Unix Domain Sockets

The CLI and daemon communicate over a Unix Domain Socket at `/run/victus-control/victus.sock`. Each command is a short-lived connection — the CLI connects, sends one line, and disconnects. The daemon's `SocketListener` loops on `accept()`, handling one command per connection.

```
CLI                              Daemon
 open connection  ────────────►  accept()
 send "fan manual 3000 3500" ►  readLine() → updates DaemonState
 shutdownOutput() ────────────►  readLine() returns (EOF)
 close connection ────────────►  loops back to accept()
```

### Project Structure

```
VictusControl/
├── core/                              # Shared between daemon and CLI
│   └── src/main/java/core/
│       ├── DaemonState.java           # Shared volatile state
│       ├── RGBMode.java               # Enum: STATIC, RAINBOW, OFF
│       └── FANMode.java               # Enum: MAX, MANUAL, AUTO
│
├── daemon/                            # Systemd service
│   └── src/main/java/daemon/
│       ├── Main.java                  # Starts all threads
│       ├── SocketListener.java        # IPC — listens on Unix socket
│       ├── keyboardEffects/
│       │   ├── Keyboard.java          # Mode dispatcher + change detection
│       │   ├── Rainbow.java           # Rainbow effect runnable
│       │   └── Static.java            # Static color + off
│       └── fanControl/
│           ├── Fan.java               # Fan mode dispatcher
│           └── ManualMode.java        # Fan enforcement loop
│
└── cli/                               # CLI client
    └── src/main/java/cli/
        ├── Main.java                  # Argument parsing + command routing
        └── SocketClient.java          # Sends commands over Unix socket
```

---

## Building

```bash
git clone https://github.com/ghosty-gigabytes/VictusControl
cd VictusControl
mvn package
```

Produces:
- `daemon/target/daemon-1.0.0.jar` — self-contained fat jar for the daemon
- `cli/target/cli-1.0.0.jar` — CLI client jar

---

## Installation

### 1. Copy the daemon jar

```bash
sudo mkdir -p /usr/lib/victus-control
sudo cp daemon/target/daemon-1.0.0.jar /usr/lib/victus-control/daemon.jar
```

### 2. Create the systemd service

```bash
sudo nano /etc/systemd/system/rgbd.service
```

```ini
[Unit]
Description=VictusControl Daemon
After=multi-user.target

[Service]
ExecStart=/usr/bin/java -jar /usr/lib/victus-control/daemon.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

### 3. Enable and start

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now rgbd.service
```

Check it's running:
```bash
sudo systemctl status rgbd.service
sudo journalctl -u rgbd.service -f
```

### 4. Fix sysfs permissions (run commands without sudo)

```bash
sudo nano /etc/udev/rules.d/99-victus-control.rules
```

Add:
```
SUBSYSTEM=="leds", KERNEL=="hp::kbd_backlight", RUN+="/bin/chmod a+w /sys/class/leds/hp::kbd_backlight/brightness /sys/class/leds/hp::kbd_backlight/multi_intensity"
```

```bash
sudo udevadm control --reload-rules && sudo udevadm trigger
```

---

## Usage

> **Note:** The CLI currently requires running via the jar file directly. A native binary is planned.

```bash
java -jar cli/target/cli-1.0.0.jar <category> <command> [args]
```

### Keyboard Commands

```bash
# Rainbow effect with default brightness (255) and default delay (20ms)
java -jar cli.jar setkeyboard rainbow

# Rainbow with custom brightness (0–255)
java -jar cli.jar setkeyboard rainbow <brightness>

# Rainbow with custom brightness and delay
java -jar cli.jar setkeyboard rainbow <brightness> <delay_ms>

# Static color
java -jar cli.jar setkeyboard static <r> <g> <b> <brightness>

# Static color with default brightness
java -jar cli.jar setkeyboard static <r> <g> <b>

# Turn off
java -jar cli.jar setkeyboard off
```

### Fan Commands

```bash
# BIOS controls fans
java -jar cli.jar setfan auto

# Maximum speed
java -jar cli.jar setfan max

# Manual — both fans at the same RPM
java -jar cli.jar setfan manual <rpm>

# Manual — independent RPM per fan
java -jar cli.jar setfan manual <fan1_rpm> <fan2_rpm>
```

### Examples

```bash
java -jar cli.jar setkeyboard rainbow 200 15        # bright, fast rainbow
java -jar cli.jar setkeyboard static 255 0 128 255  # purple at full brightness
java -jar cli.jar setkeyboard static 0 255 0 100    # dim green
java -jar cli.jar setkeyboard off

java -jar cli.jar setfan manual 3000                # both fans at 3000 RPM
java -jar cli.jar setfan manual 2500 4000           # fan1 slower, fan2 faster
java -jar cli.jar setfan auto
```

### Help

```bash
java -jar cli.jar help
```

---

## How Fan Enforcement Works

When in `manual` mode, the daemon does not just set the fan speed once and stop. The `ManualMode` thread continuously verifies the actual fan RPM against the target and rewrites the target if the BIOS overrides it. This ensures the fan speed stays at the user-set value even under thermal events:

```
write fan1_target, fan2_target
        │
        └── read fan1_input, fan2_input
                │
                ├── matches target → sleep 100s, check again
                └── doesn't match → retry write
```

---

## Roadmap

### In Progress
- [ ] Fan curve mode — automatically adjust speed based on CPU temperature
- [ ] Temperature polling
- [ ] Config file persistence — restore last used settings on daemon start

### Planned
- [ ] PyQt GUI with fan curve editor and temperature dashboard
- [ ] System tray / widget for quick settings
- [ ] Multiple RGB effects (breathing, pulse, static gradient)
- [ ] `getKeyboard` / `getFan` commands — query current state from daemon
- [ ] Native CLI binary (no Java required to run the CLI)
- [ ] Linux packages (`.rpm`, `.deb`)
- [ ] Install script (`install.sh`)

---

## Troubleshooting

**Daemon won't start / sysfs paths not found**
Verify the kernel module is loaded:
```bash
lsmod | grep hp_wmi
ls /sys/devices/platform/hp-wmi/hwmon/
```

**CLI can't connect to daemon**
```bash
sudo systemctl status rgbd.service
sudo journalctl -u rgbd.service -n 50
```

**Fan speed not changing in manual mode**
The `hp-wmi` module must expose `fan1_target` and `fan2_target` in the hwmon directory. Verify:
```bash
ls /sys/devices/platform/hp-wmi/hwmon/hwmon*/
```

---

## Contributing

Contributions are welcome, especially:
- Testing on other HP Victus models and reporting compatibility
- New RGB effects
- Packaging and install script improvements

Please open an issue before submitting a PR for large changes.

---

## License

GPL-3.0 — see [LICENSE](LICENSE) for details.

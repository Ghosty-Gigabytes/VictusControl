# VictusControl

![License](https://img.shields.io/badge/license-GPL--3.0-blue)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Python](https://img.shields.io/badge/Python-3.10%2B-blue)
![Platform](https://img.shields.io/badge/platform-Linux-lightgrey)
![Status](https://img.shields.io/badge/status-active%20development-green)

A Linux utility for controlling keyboard backlight and fan speeds on HP Victus laptops. Runs as a **systemd daemon**, controlled via a **CLI tool** and a **PyQt6 GUI** over Unix Domain Sockets.

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

### GUI
- Mode buttons for keyboard and fan with live state loaded from daemon on startup
- Brightness and speed sliders
- Color picker for static mode
- Live RPM gauges updated every 2 seconds
- System tray icon with quick-access menu
- Daemon connection status indicator

---

## Defaults

When optional arguments are omitted, the following default values are used:

| Setting | Default Value |
|---|---|
| Brightness | 255 |
| Rainbow delay | 20ms |
| Fan 1 target | 2500 RPM |
| Fan 2 target | 2500 RPM |

---

## Requirements

- **Java 21+**
- **Maven 3.8+**
- **Python 3.10+** with **PyQt6** (for GUI only)
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
CLI (victus-ctl)  ──┐
                    │  Unix Domain Socket
GUI (PyQt6)  ───────┤  /run/victus-control/victus.sock
                    │
                    ▼
Daemon (rgbd) ── systemd service
      │
      ├── SocketListener thread   listens for incoming commands
      │         │
      │         └── updates DaemonState (shared volatile fields)
      │             returns JSON for getKeyboard / getFan queries
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

The CLI, GUI, and daemon all communicate over a Unix Domain Socket at `/run/victus-control/victus.sock`. Each command is a short-lived connection. For query commands (`getKeyboard`, `getFan`) the daemon writes back a JSON response before closing.

```
Client                           Daemon
 open connection  ────────────►  accept()
 send "setFan manual 3000 3500"► readLine() → updates DaemonState
 shutdownOutput() ────────────►  readLine() returns (EOF)
 close connection ────────────►  loops back to accept()

 open connection  ────────────►  accept()
 send "getFan"    ────────────►  readLine() → builds JSON response
                  ◄────────────  println(json)
 read response    ◄────────────  flush + close
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
│       ├── SocketListener.java        # IPC — listens on Unix socket, returns JSON
│       ├── keyboardEffects/
│       │   ├── Keyboard.java          # Mode dispatcher + change detection
│       │   ├── Rainbow.java           # Rainbow effect runnable
│       │   └── Static.java            # Static color + off
│       └── fanControl/
│           ├── Fan.java               # Fan mode dispatcher
│           └── ManualMode.java        # Fan enforcement loop
│
├── cli/                               # CLI client
│   └── src/main/java/cli/
│       ├── Main.java                  # Argument parsing + command routing
│       └── SocketClient.java          # Sends commands, reads JSON responses
│
├── gui/                               # PyQt6 GUI
│   ├── main.py                        # Entry point
│   ├── requirements.txt
│   ├── ipc/
│   │   └── client.py                  # Unix socket client (send_command, send_query)
│   ├── ui/
│   │   ├── main_window.py             # QMainWindow + tabs + header
│   │   ├── keyboard_tab.py            # Keyboard controls tab
│   │   ├── fan_tab.py                 # Fan controls tab + RPM gauges
│   │   └── tray.py                    # System tray icon + quick menu
│   └── workers/
│       └── fan_poller.py              # QThread — polls daemon for live RPM
│
└── packaging/
    ├── install.sh
    └── uninstall.sh
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

### 4. Fix sysfs permissions (run without sudo)

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

### 5. Set up socket permissions (run CLI/GUI without sudo)

```bash
sudo groupadd victus-control
sudo usermod -aG victus-control $USER
# log out and back in for the group change to take effect
groups $USER  # verify — should show victus-control
```

### 6. Install GUI dependencies

```bash
pip install -r gui/requirements.txt
```

---

## Usage

### GUI

```bash
python3 gui/main.py
```

The GUI loads the current daemon state on startup — sliders, mode buttons, and color picker all reflect what the daemon is currently running. Closing the window hides it to the system tray.

### CLI

> **Note:** The CLI currently requires running via the jar file directly. A native binary is planned.

```bash
java -jar cli/target/cli-1.0.0.jar <command> [args]
```

#### Keyboard Commands

```bash
# Rainbow with defaults
java -jar cli.jar setKeyboard rainbow

# Rainbow with custom brightness (0–255)
java -jar cli.jar setKeyboard rainbow <brightness>

# Rainbow with custom brightness and delay
java -jar cli.jar setKeyboard rainbow <brightness> <delay_ms>

# Static color with default brightness
java -jar cli.jar setKeyboard static <r> <g> <b>

# Static color with custom brightness
java -jar cli.jar setKeyboard static <r> <g> <b> <brightness>

# Turn off
java -jar cli.jar setKeyboard off

# Query current keyboard state
java -jar cli.jar getKeyboard
```

#### Fan Commands

```bash
# BIOS controls fans
java -jar cli.jar setFan auto

# Maximum speed
java -jar cli.jar setFan max

# Manual — both fans at the same RPM
java -jar cli.jar setFan manual <rpm>

# Manual — independent RPM per fan
java -jar cli.jar setFan manual <fan1_rpm> <fan2_rpm>

# Query current fan state
java -jar cli.jar getFan
```

#### Examples

```bash
java -jar cli.jar setKeyboard rainbow 200 15        # bright, fast rainbow
java -jar cli.jar setKeyboard static 255 0 128 255  # purple at full brightness
java -jar cli.jar setKeyboard static 0 255 0 100    # dim green
java -jar cli.jar setKeyboard off

java -jar cli.jar setFan manual 3000                # both fans at 3000 RPM
java -jar cli.jar setFan manual 2500 4000           # fan1 slower, fan2 faster
java -jar cli.jar setFan auto

java -jar cli.jar getKeyboard                       # print current keyboard state
java -jar cli.jar getFan                            # print current fan state
java -jar cli.jar about                             # show version and project info
```

#### Help

```bash
java -jar cli.jar help
```

---

## Socket Protocol Reference

Commands are plain text strings sent over `/run/victus-control/victus.sock`. Useful for writing custom clients (scripts, other GUI frameworks, etc.) without going through the CLI.

### Set Commands (no response)

| Command | Description |
|---|---|
| `setKeyboard rainbow <brightness> <delay_ms>` | Start rainbow effect |
| `setKeyboard static <r> <g> <b> <brightness>` | Set static color |
| `setKeyboard off` | Turn off backlight |
| `setKeyboard brightness <value>` | Set brightness only |
| `setFan auto` | BIOS fan control |
| `setFan max` | Maximum fan speed |
| `setFan manual <fan1_rpm> <fan2_rpm>` | Manual fan speed |

### Query Commands (returns JSON)

| Command | Response fields |
|---|---|
| `getKeyboard` | `rgbMode`, `brightness`, `speed`, `r`, `g`, `b`, `maxBrightness` |
| `getFan` | `pwmMode`, `fan1Target`, `fan2Target`, `fan1Input`, `fan2Input`, `fan1Max`, `fan2Max` |

`pwmMode` values: `0` = MAX, `1` = MANUAL, `2` = AUTO

Test manually with `socat`:

```bash
# install socat
sudo dnf install socat   # Fedora
sudo apt install socat   # Debian/Ubuntu

# set commands
echo "setKeyboard rainbow 255 20" | socat - UNIX-CONNECT:/run/victus-control/victus.sock
echo "setKeyboard static 255 0 128 255" | socat - UNIX-CONNECT:/run/victus-control/victus.sock
echo "setFan manual 3000 3500" | socat - UNIX-CONNECT:/run/victus-control/victus.sock

# query commands
echo "getKeyboard" | socat - UNIX-CONNECT:/run/victus-control/victus.sock
echo "getFan" | socat - UNIX-CONNECT:/run/victus-control/victus.sock
```

---

## How Fan Enforcement Works

When in `manual` mode, the daemon does not just set the fan speed once and stop. The `ManualMode` thread continuously verifies the actual fan RPM against the target and rewrites it if the BIOS overrides it:

```
write fan1_target, fan2_target
        │
        └── read fan1_input, fan2_input
                │
                ├── matches target → sleep, check again
                └── doesn't match → retry write
```

This ensures the fan speed stays at the user-set value even under thermal events where the BIOS tries to take back control.

---

## Viewing Daemon Logs

```bash
# follow live logs
sudo journalctl -u rgbd.service -f

# last 50 lines
sudo journalctl -u rgbd.service -n 50

# all logs since last boot
sudo journalctl -u rgbd.service -b

# with precise timestamps
sudo journalctl -u rgbd.service -b --output=short-precise
```

---

## Known Limitations

- Fan speed changes may take a few seconds to apply — the BIOS enforces its own ramp rate
- Brightness control affects the overall backlight level only, not per-key
- Settings are not persisted across daemon restarts — resets to defaults on reboot (planned fix)
- CLI requires Java to be installed — native binary is planned
- Only one RGB effect runs at a time — no layering or transitions yet

---

## Troubleshooting

**Daemon won't start / sysfs paths not found**
```bash
lsmod | grep hp_wmi
ls /sys/devices/platform/hp-wmi/hwmon/
```

**CLI or GUI can't connect to daemon**
```bash
sudo systemctl status rgbd.service
sudo journalctl -u rgbd.service -n 50
```

**Permission denied on socket**
```bash
groups $USER   # verify victus-control is listed
# if not: sudo usermod -aG victus-control $USER, then log out and back in
```

**Fan speed not changing in manual mode**
```bash
ls /sys/devices/platform/hp-wmi/hwmon/hwmon*/
# verify fan1_target and fan2_target exist
```

**GUI shows "daemon offline"**
```bash
sudo systemctl start rgbd.service
```

---

## Roadmap

### In Progress
- [ ] Fan curve mode — automatically adjust speed based on CPU temperature
- [ ] Temperature polling
- [ ] Config file persistence — restore last used settings on daemon start

### Planned
- [ ] Fan curve editor in GUI
- [ ] Temperature dashboard in GUI
- [ ] Multiple RGB effects (breathing, pulse, static gradient)
- [ ] Native CLI binary (no Java required)
- [ ] Linux packages (`.rpm`, `.deb`)
- [ ] Install script (`install.sh`)

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
import json
from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QFrame
)
from PyQt6.QtCore import Qt
from ui.keyboard_tab import ModeButton, SliderRow
from ipc.client import send_command, send_query
from workers.fan_poller import FanPoller


class RPMGauge(QWidget):
    """Card showing live fan RPM for one fan."""

    def __init__(self, label: str, parent=None):
        super().__init__(parent)
        self.setFixedSize(160, 100)
        self.setStyleSheet("""
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.07);
            border-radius: 12px;
        """)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 20, 20, 20)
        layout.setSpacing(4)
        layout.setAlignment(Qt.AlignmentFlag.AlignCenter)

        lbl = QLabel(label)
        lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        lbl.setStyleSheet(
            "color: #444; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 2px;"
        )

        self.rpm_label = QLabel("—")
        self.rpm_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.rpm_label.setStyleSheet(
            "color: #e05c2a; font-size: 22px; font-weight: 700; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        layout.addWidget(lbl)
        layout.addWidget(self.rpm_label)

    def set_rpm(self, rpm: int):
        self.rpm_label.setText(f"{rpm:,}")


class TempGauge(QWidget):
    """Card showing live CPU temperature with one decimal place."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setFixedSize(160, 100)
        self.setStyleSheet("""
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.07);
            border-radius: 12px;
        """)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 20, 20, 20)
        layout.setSpacing(4)
        layout.setAlignment(Qt.AlignmentFlag.AlignCenter)

        lbl = QLabel("CPU TEMP")
        lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        lbl.setStyleSheet(
            "color: #444; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 2px;"
        )

        self.temp_label = QLabel("—")
        self.temp_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.temp_label.setStyleSheet(
            "color: #e05c2a; font-size: 22px; font-weight: 700; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        layout.addWidget(lbl)
        layout.addWidget(self.temp_label)

    def set_temp(self, temp: float):
        self.temp_label.setText(f"{temp:.1f} °C")

        if temp < 60.0:
            color = "#2ecc71"
        elif temp < 80.0:
            color = "#e0a12a"
        else:
            color = "#e05c2a"

        self.temp_label.setStyleSheet(
            f"color: {color}; font-size: 22px; font-weight: 700; "
            "font-family: 'JetBrains Mono', monospace;"
        )


class FanTab(QWidget):

    _DEFAULT_MAX_RPM = 5500

    def __init__(self, parent=None):
        super().__init__(parent)
        self._current_mode = "auto"
        self._poller = FanPoller()
        self._poller.rpm_updated.connect(self._on_rpm_update)
        self._poller.error.connect(self._on_poller_error)
        self._poller.start()
        self._build_ui()
        self._load_state()

    def _build_ui(self):
        root = QVBoxLayout(self)
        root.setContentsMargins(32, 32, 32, 32)
        root.setSpacing(28)

        # ── mode ──────────────────────────────────────────────
        root.addWidget(self._section_label("MODE"))

        mode_row = QHBoxLayout()
        mode_row.setSpacing(10)

        self._btn_auto   = ModeButton("AUTO")
        self._btn_max    = ModeButton("MAX")
        self._btn_manual = ModeButton("MANUAL")

        self._btn_auto.clicked.connect(lambda:   self._set_mode("auto"))
        self._btn_max.clicked.connect(lambda:    self._set_mode("max"))
        self._btn_manual.clicked.connect(lambda: self._set_mode("manual"))

        mode_row.addWidget(self._btn_auto)
        mode_row.addWidget(self._btn_max)
        mode_row.addWidget(self._btn_manual)
        mode_row.addStretch()
        root.addLayout(mode_row)

        root.addWidget(self._divider())

        # ── target RPM sliders ────────────────────────────────
        root.addWidget(self._section_label("TARGET RPM  —  manual mode only"))

        # step=100 so each tick moves 100 RPM
        # max updated from daemon in _load_state()
        self._fan1_slider = SliderRow(
            "Fan 1", 0, self._DEFAULT_MAX_RPM, 2500, step=100
        )
        self._fan2_slider = SliderRow(
            "Fan 2", 0, self._DEFAULT_MAX_RPM, 2500, step=100
        )

        # connect AFTER both sliders exist to avoid cross-firing
        self._fan1_slider.slider.valueChanged.connect(self._on_slider_change)
        self._fan2_slider.slider.valueChanged.connect(self._on_slider_change)

        root.addWidget(self._fan1_slider)
        root.addWidget(self._fan2_slider)

        root.addWidget(self._divider())

        # ── live gauges ───────────────────────────────────────
        root.addWidget(self._section_label("LIVE RPM & TEMPERATURE"))

        gauge_row = QHBoxLayout()
        gauge_row.setSpacing(16)

        self._gauge1     = RPMGauge("FAN 1")
        self._gauge2     = RPMGauge("FAN 2")
        self._temp_gauge = TempGauge()

        gauge_row.addWidget(self._gauge1)
        gauge_row.addWidget(self._gauge2)
        gauge_row.addWidget(self._temp_gauge)
        gauge_row.addStretch()
        root.addLayout(gauge_row)

        self._status_lbl = QLabel("Polling...")
        self._status_lbl.setStyleSheet(
            "color: #444; font-size: 11px; "
            "font-family: 'JetBrains Mono', monospace;"
        )
        root.addWidget(self._status_lbl)

        root.addStretch()
        self._update_mode_buttons()
        self._update_controls()

    # ── helpers ───────────────────────────────────────────────

    def _section_label(self, text: str) -> QLabel:
        lbl = QLabel(text)
        lbl.setStyleSheet(
            "color: #444; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; "
            "letter-spacing: 3px; font-weight: 700;"
        )
        return lbl

    def _divider(self) -> QFrame:
        line = QFrame()
        line.setFrameShape(QFrame.Shape.HLine)
        line.setStyleSheet(
            "background: rgba(255,255,255,0.06); border: none; max-height: 1px;"
        )
        return line

    # ── state ─────────────────────────────────────────────────

    def _load_state(self):
        """Query daemon for current fan state and populate UI."""
        response = send_query("getFan")
        if not response:
            return
        try:
            data     = json.loads(response)
            pwm_mode = int(data.get("pwmMode", 2))

            # update slider ranges from real hardware max values
            fan1_max = int(data.get("fan1Max", self._DEFAULT_MAX_RPM))
            fan2_max = int(data.get("fan2Max", self._DEFAULT_MAX_RPM))
            if fan1_max > 0:
                self._fan1_slider.set_range(0, fan1_max)
            if fan2_max > 0:
                self._fan2_slider.set_range(0, fan2_max)

            mode_map = {0: "max", 1: "manual", 2: "auto"}
            self._current_mode = mode_map.get(pwm_mode, "auto")
            self._update_mode_buttons()
            self._update_controls()

            # populate slider values only when in manual mode
            if pwm_mode == 1:
                self._fan1_slider.slider.setValue(
                    int(data.get("fan1Target", 2500))   # fan1 slider ← fan1Target
                )
                self._fan2_slider.slider.setValue(
                    int(data.get("fan2Target", 2500))   # fan2 slider ← fan2Target
                )

            cpu_temp = float(data.get("cpuTemp", 0.0))
            if cpu_temp:
                self._temp_gauge.set_temp(cpu_temp)

        except Exception as e:
            print(f"[FanTab] Failed to load state: {e}")

    def _set_mode(self, mode: str):
        self._current_mode = mode
        self._update_mode_buttons()
        self._update_controls()
        self._send_current()

    def _update_mode_buttons(self):
        self._btn_auto.setActive(self._current_mode   == "auto")
        self._btn_max.setActive(self._current_mode    == "max")
        self._btn_manual.setActive(self._current_mode == "manual")

    def _update_controls(self):
        """Sliders only active in manual mode."""
        manual = (self._current_mode == "manual")
        self._fan1_slider.setEnabled(manual)
        self._fan2_slider.setEnabled(manual)

    def _on_slider_change(self):
        if self._current_mode == "manual":
            self._send_current()

    def _send_current(self):
        if self._current_mode == "auto":
            send_command("setFan auto")
        elif self._current_mode == "max":
            send_command("setFan max")
        elif self._current_mode == "manual":
            f1 = self._fan1_slider.value()  # fan1 slider → fan1 target
            f2 = self._fan2_slider.value()  # fan2 slider → fan2 target
            send_command(f"setFan manual {f1} {f2}")

    def _on_rpm_update(self, fan1: int, fan2: int, cpu_temp: float):
        self._gauge1.set_rpm(fan1)   # fan1 gauge ← fan1Input
        self._gauge2.set_rpm(fan2)   # fan2 gauge ← fan2Input
        self._temp_gauge.set_temp(cpu_temp)
        self._status_lbl.setText("Updated just now")

    def _on_poller_error(self, msg: str):
        self._status_lbl.setText(f"⚠  {msg}")

    def closeEvent(self, event):
        self._poller.stop()
        super().closeEvent(event)
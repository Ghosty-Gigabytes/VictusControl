import json
from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QSlider, QFrame, QColorDialog
)
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QColor, QPainter, QBrush, QPen
from ipc.client import send_command, send_query


class ColorPreview(QFrame):
    """Small rounded square showing the currently selected static color."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setFixedSize(48, 48)
        self._color = QColor(255, 0, 0)

    def set_color(self, color: QColor):
        self._color = color
        self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        painter.setBrush(QBrush(self._color))
        painter.setPen(QPen(QColor(255, 255, 255, 30), 1))
        painter.drawRoundedRect(self.rect().adjusted(1, 1, -1, -1), 8, 8)


class ModeButton(QPushButton):
    """Styled toggle mode button."""

    ACTIVE_STYLE = """
        QPushButton {
            background: qlineargradient(x1:0, y1:0, x2:1, y2:1,
                stop:0 #e05c2a, stop:1 #c93f15);
            color: #ffffff;
            border: none;
            border-radius: 10px;
            padding: 10px 24px;
            font-size: 13px;
            font-weight: 600;
            font-family: 'JetBrains Mono', 'Fira Code', monospace;
            letter-spacing: 1px;
        }
    """
    INACTIVE_STYLE = """
        QPushButton {
            background: rgba(255,255,255,0.05);
            color: #888;
            border: 1px solid rgba(255,255,255,0.08);
            border-radius: 10px;
            padding: 10px 24px;
            font-size: 13px;
            font-family: 'JetBrains Mono', 'Fira Code', monospace;
            letter-spacing: 1px;
        }
        QPushButton:hover {
            background: rgba(255,255,255,0.1);
            color: #ccc;
        }
    """

    def __init__(self, text, parent=None):
        super().__init__(text, parent)
        self.setActive(False)

    def setActive(self, active: bool):
        self.setStyleSheet(self.ACTIVE_STYLE if active else self.INACTIVE_STYLE)


class SliderRow(QWidget):
    """Label + slider + value display in a single row."""

    def __init__(self, label: str, min_val: int, max_val: int, default: int, parent=None):
        super().__init__(parent)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(16)

        lbl = QLabel(label)
        lbl.setFixedWidth(90)
        lbl.setStyleSheet(
            "color: #888; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        self.slider = QSlider(Qt.Orientation.Horizontal)
        self.slider.setRange(min_val, max_val)
        self.slider.setValue(default)
        self.slider.setStyleSheet("""
            QSlider::groove:horizontal {
                height: 4px;
                background: rgba(255,255,255,0.1);
                border-radius: 2px;
            }
            QSlider::handle:horizontal {
                width: 16px; height: 16px;
                background: #e05c2a;
                border-radius: 8px;
                margin: -6px 0;
            }
            QSlider::sub-page:horizontal {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #c93f15, stop:1 #e05c2a);
                border-radius: 2px;
            }
        """)

        self.value_lbl = QLabel(str(default))
        self.value_lbl.setFixedWidth(36)
        self.value_lbl.setAlignment(Qt.AlignmentFlag.AlignRight)
        self.value_lbl.setStyleSheet(
            "color: #e05c2a; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace; font-weight: 600;"
        )

        self.slider.valueChanged.connect(lambda v: self.value_lbl.setText(str(v)))

        layout.addWidget(lbl)
        layout.addWidget(self.slider)
        layout.addWidget(self.value_lbl)

    def value(self) -> int:
        return self.slider.value()


class KeyboardTab(QWidget):

    def __init__(self, parent=None):
        super().__init__(parent)
        self._static_color = QColor(255, 0, 0)
        self._current_mode = "rainbow"
        self._build_ui()
        self._load_state()

    def _build_ui(self):
        root = QVBoxLayout(self)
        root.setContentsMargins(32, 32, 32, 32)
        root.setSpacing(28)

        root.addWidget(self._section_label("MODE"))

        mode_row = QHBoxLayout()
        mode_row.setSpacing(10)

        self._btn_rainbow = ModeButton("RAINBOW")
        self._btn_static  = ModeButton("STATIC")
        self._btn_off     = ModeButton("OFF")

        self._btn_rainbow.clicked.connect(lambda: self._set_mode("rainbow"))
        self._btn_static.clicked.connect(lambda:  self._set_mode("static"))
        self._btn_off.clicked.connect(lambda:     self._set_mode("off"))

        mode_row.addWidget(self._btn_rainbow)
        mode_row.addWidget(self._btn_static)
        mode_row.addWidget(self._btn_off)
        mode_row.addStretch()
        root.addLayout(mode_row)

        root.addWidget(self._divider())

        root.addWidget(self._section_label("BRIGHTNESS & SPEED"))

        self._brightness = SliderRow("Brightness", 0, 255, 255)
        self._speed      = SliderRow("Delay (ms)",  1, 200,  20)

        self._brightness.slider.valueChanged.connect(self._on_brightness_change)
        self._speed.slider.valueChanged.connect(self._on_delay_change)

        root.addWidget(self._brightness)
        root.addWidget(self._speed)

        root.addWidget(self._divider())

        root.addWidget(self._section_label("COLOR  —  static mode only"))

        color_row = QHBoxLayout()
        color_row.setSpacing(16)

        self._color_preview = ColorPreview()
        self._color_preview.set_color(self._static_color)

        pick_btn = QPushButton("Pick Color")
        pick_btn.setStyleSheet("""
            QPushButton {
                background: rgba(255,255,255,0.05);
                color: #ccc;
                border: 1px solid rgba(255,255,255,0.1);
                border-radius: 8px;
                padding: 8px 20px;
                font-size: 12px;
                font-family: 'JetBrains Mono', monospace;
            }
            QPushButton:hover {
                background: rgba(255,255,255,0.1);
                color: white;
            }
        """)
        pick_btn.clicked.connect(self._pick_color)

        self._color_hex = QLabel(self._static_color.name().upper())
        self._color_hex.setStyleSheet(
            "color: #555; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        color_row.addWidget(self._color_preview)
        color_row.addWidget(pick_btn)
        color_row.addWidget(self._color_hex)
        color_row.addStretch()
        root.addLayout(color_row)

        root.addStretch()

        self._update_mode_buttons()


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

    def _load_state(self):
        """Query daemon for current keyboard state and populate UI."""
        response = send_query("getKeyboard")
        if not response:
            return
        try:
            data = json.loads(response)

            # sliders
            self._brightness.slider.setValue(int(data.get("brightness", 255)))
            self._speed.slider.setValue(int(data.get("speed", 20)))

            # color
            r  = int(data.get("r", 255))
            g  = int(data.get("g", 0))
            b  = int(data.get("b", 0))
            self._static_color = QColor(r, g, b)
            self._color_preview.set_color(self._static_color)
            self._color_hex.setText(self._static_color.name().upper())

            # mode — set last so sliders are ready before any send
            mode = data.get("rgbMode", "RAINBOW").upper()
            self._current_mode = mode.lower()
            self._update_mode_buttons()

        except Exception as e:
            print(f"[KeyboardTab] Failed to load state: {e}")

    def _set_mode(self, mode: str):
        self._current_mode = mode
        self._update_mode_buttons()
        self._send_current()

    def _update_mode_buttons(self):
        self._btn_rainbow.setActive(self._current_mode == "rainbow")
        self._btn_static.setActive(self._current_mode  == "static")
        self._btn_off.setActive(self._current_mode     == "off")

    def _on_brightness_change(self):
        b=self._brightness.value();
        send_command(f"setKeyboard brightness {b}")

    def _pick_color(self):
        color = QColorDialog.getColor(self._static_color, self, "Pick Keyboard Color")
        if color.isValid():
            self._static_color = color
            self._color_preview.set_color(color)
            self._color_hex.setText(color.name().upper())
            if self._current_mode == "static":
                self._send_current()

    def _send_current(self):
        b = self._brightness.value()
        s = self._speed.value()

        if self._current_mode == "rainbow":
            send_command(f"setKeyboard rainbow {b} {s}")
        elif self._current_mode == "static":
            r  = self._static_color.red()
            g  = self._static_color.green()
            bl = self._static_color.blue()
            send_command(f"setKeyboard static {r} {g} {bl} {b}")
        elif self._current_mode == "off":
            send_command("setKeyboard off")

    def _on_delay_change(self):
        d = self._speed.value();
        send_command(f"setKeyboard delay {d}")

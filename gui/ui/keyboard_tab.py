import json
from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QSlider, QFrame, QColorDialog,
    QStyleOptionSlider, QStyle
)
from PyQt6.QtCore import Qt, QRect
from PyQt6.QtGui import QColor, QPainter, QBrush, QPen, QMouseEvent
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


class SnapSlider(QSlider):
    """
    QSlider subclass that jumps directly to the clicked position
    instead of moving one step at a time.
    Also snaps values to multiples of step.
    """

    def __init__(self, step: int = 1, parent=None):
        super().__init__(Qt.Orientation.Horizontal, parent)
        self._step = step

    def mousePressEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.LeftButton:
            value = self._value_from_position(event.pos().x())
            self.setValue(value)
            event.accept()
        else:
            super().mousePressEvent(event)

    def mouseMoveEvent(self, event: QMouseEvent):
        if event.buttons() & Qt.MouseButton.LeftButton:
            value = self._value_from_position(event.pos().x())
            self.setValue(value)
            event.accept()
        else:
            super().mouseMoveEvent(event)

    def _value_from_position(self, x: int) -> int:
        """Convert a pixel x position to a snapped slider value."""
        opt = QStyleOptionSlider()
        self.initStyleOption(opt)

        groove: QRect = self.style().subControlRect(
            QStyle.ComplexControl.CC_Slider,
            opt,
            QStyle.SubControl.SC_SliderGroove,
            self
        )
        handle: QRect = self.style().subControlRect(
            QStyle.ComplexControl.CC_Slider,
            opt,
            QStyle.SubControl.SC_SliderHandle,
            self
        )

        # usable groove width excluding handle half-widths
        groove_start = groove.left() + handle.width() // 2
        groove_end   = groove.right() - handle.width() // 2
        groove_width = max(groove_end - groove_start, 1)

        # clamp x to groove bounds
        x_clamped = max(groove_start, min(groove_end, x))

        ratio = (x_clamped - groove_start) / groove_width
        span  = self.maximum() - self.minimum()
        raw   = self.minimum() + ratio * span

        # snap to multiple of step
        if self._step > 1:
            snapped = round(raw / self._step) * self._step
        else:
            snapped = round(raw)

        return max(self.minimum(), min(self.maximum(), snapped))


class SliderRow(QWidget):
    """
    Label + SnapSlider + value display in a single row.
    Shows min/max values below the slider in subtle grey.
    Click anywhere on the slider to jump to that value instantly.
    """

    SLIDER_STYLE_ENABLED = """
        QSlider {
            min-height: 32px;
        }
        QSlider::groove:horizontal {
            height: 4px;
            background: rgba(255,255,255,0.1);
            border-radius: 2px;
        }
        QSlider::handle:horizontal {
            width: 24px;
            height: 24px;
            background: #e05c2a;
            border-radius: 12px;
            margin: -10px 0;
        }
        QSlider::sub-page:horizontal {
            background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                stop:0 #c93f15, stop:1 #e05c2a);
            border-radius: 2px;
        }
    """

    SLIDER_STYLE_DISABLED = """
        QSlider {
            min-height: 32px;
        }
        QSlider::groove:horizontal {
            height: 4px;
            background: rgba(255,255,255,0.04);
            border-radius: 2px;
        }
        QSlider::handle:horizontal {
            width: 24px;
            height: 24px;
            background: #333;
            border-radius: 12px;
            margin: -10px 0;
        }
        QSlider::sub-page:horizontal {
            background: rgba(255,255,255,0.06);
            border-radius: 2px;
        }
    """

    def __init__(self, label: str, min_val: int, max_val: int,
                 default: int, step: int = 1, parent=None):
        super().__init__(parent)

        self._step    = step
        self._min_val = min_val
        self._max_val = max_val

        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.setSpacing(3)

        # ── main slider row ───────────────────────────────────
        row = QHBoxLayout()
        row.setContentsMargins(0, 0, 0, 0)
        row.setSpacing(16)

        self._lbl = QLabel(label)
        self._lbl.setFixedWidth(90)
        self._lbl.setStyleSheet(
            "color: #888; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        self.slider = SnapSlider(step=step)
        self.slider.setRange(min_val, max_val)
        self.slider.setValue(self._snap(default))
        self.slider.setSingleStep(step)
        self.slider.setPageStep(step)
        self.slider.setStyleSheet(self.SLIDER_STYLE_ENABLED)

        self.slider.valueChanged.connect(self._on_value_changed)

        self._value_lbl = QLabel(str(self._snap(default)))
        self._value_lbl.setFixedWidth(36)
        self._value_lbl.setAlignment(Qt.AlignmentFlag.AlignRight)
        self._value_lbl.setStyleSheet(
            "color: #e05c2a; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace; font-weight: 600;"
        )

        row.addWidget(self._lbl)
        row.addWidget(self.slider)
        row.addWidget(self._value_lbl)
        outer.addLayout(row)

        # ── min/max hint row ──────────────────────────────────
        hint_row = QHBoxLayout()
        hint_row.setContentsMargins(90 + 16, 0, 36 + 16, 0)
        hint_row.setSpacing(0)

        hint_style = (
            "color: #2a2a2a; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        self._min_lbl = QLabel(str(min_val))
        self._min_lbl.setStyleSheet(hint_style)

        self._max_lbl = QLabel(str(max_val))
        self._max_lbl.setStyleSheet(hint_style)
        self._max_lbl.setAlignment(Qt.AlignmentFlag.AlignRight)

        hint_row.addWidget(self._min_lbl)
        hint_row.addStretch()
        hint_row.addWidget(self._max_lbl)
        outer.addLayout(hint_row)

    # ── snapping ──────────────────────────────────────────────

    def _snap(self, value: int) -> int:
        if self._step <= 1:
            return value
        snapped = round(value / self._step) * self._step
        return max(self._min_val, min(self._max_val, snapped))

    def _on_value_changed(self, value: int):
        """Keep value label in sync. SnapSlider already snaps on click/drag."""
        self._value_lbl.setText(str(value))

    # ── public API ────────────────────────────────────────────

    def value(self) -> int:
        return self.slider.value()

    def set_range(self, min_val: int, max_val: int):
        self._min_val = min_val
        self._max_val = max_val
        self.slider.setRange(min_val, max_val)
        self._min_lbl.setText(str(min_val))
        self._max_lbl.setText(str(max_val))

    def setEnabled(self, enabled: bool):
        super().setEnabled(enabled)
        self.slider.setEnabled(enabled)
        self.slider.setStyleSheet(
            self.SLIDER_STYLE_ENABLED if enabled else self.SLIDER_STYLE_DISABLED
        )
        self._lbl.setStyleSheet(
            f"color: {'#888' if enabled else '#2a2a2a'}; "
            "font-size: 12px; font-family: 'JetBrains Mono', monospace;"
        )
        self._value_lbl.setStyleSheet(
            f"color: {'#e05c2a' if enabled else '#2a2a2a'}; "
            "font-size: 12px; font-family: 'JetBrains Mono', monospace; font-weight: 600;"
        )


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

        # ── mode ──────────────────────────────────────────────
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

        # ── brightness & delay ────────────────────────────────
        root.addWidget(self._section_label("BRIGHTNESS & SPEED"))

        self._brightness = SliderRow("Brightness", 0, 255, 255, step=10)
        self._speed      = SliderRow("Delay (ms)",  1, 200,  20, step=5)

        self._brightness.slider.valueChanged.connect(self._on_brightness_change)
        self._speed.slider.valueChanged.connect(self._on_delay_change)

        root.addWidget(self._brightness)
        root.addWidget(self._speed)

        root.addWidget(self._divider())

        # ── color picker ──────────────────────────────────────
        root.addWidget(self._section_label("COLOR  —  static mode only"))

        color_row = QHBoxLayout()
        color_row.setSpacing(16)

        self._color_preview = ColorPreview()
        self._color_preview.set_color(self._static_color)

        self._pick_btn = QPushButton("Pick Color")
        self._pick_btn.setStyleSheet("""
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
            QPushButton:disabled {
                background: rgba(255,255,255,0.02);
                color: #2a2a2a;
                border: 1px solid rgba(255,255,255,0.03);
            }
        """)
        self._pick_btn.clicked.connect(self._pick_color)

        self._color_hex = QLabel(self._static_color.name().upper())
        self._color_hex.setStyleSheet(
            "color: #555; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        color_row.addWidget(self._color_preview)
        color_row.addWidget(self._pick_btn)
        color_row.addWidget(self._color_hex)
        color_row.addStretch()
        root.addLayout(color_row)

        root.addStretch()
        self._update_mode_buttons()

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
        response = send_query("getKeyboard")
        if not response:
            return
        try:
            data = json.loads(response)

            self._brightness.slider.setValue(int(data.get("brightness", 255)))
            self._speed.slider.setValue(int(data.get("speed", 20)))

            r = int(data.get("r", 255))
            g = int(data.get("g", 0))
            b = int(data.get("b", 0))
            self._static_color = QColor(r, g, b)
            self._color_preview.set_color(self._static_color)
            self._color_hex.setText(self._static_color.name().upper())

            mode = data.get("rgbMode", "RAINBOW").upper()
            self._current_mode = mode.lower()
            self._update_mode_buttons()
            self._update_controls()

        except Exception as e:
            print(f"[KeyboardTab] Failed to load state: {e}")

    def _set_mode(self, mode: str):
        self._current_mode = mode
        self._update_mode_buttons()
        self._update_controls()
        self._send_current()

    def _update_mode_buttons(self):
        self._btn_rainbow.setActive(self._current_mode == "rainbow")
        self._btn_static.setActive(self._current_mode  == "static")
        self._btn_off.setActive(self._current_mode     == "off")

    def _update_controls(self):
        mode = self._current_mode
        self._brightness.setEnabled(mode != "off")
        self._speed.setEnabled(mode == "rainbow")

        color_on = (mode == "static")
        self._pick_btn.setEnabled(color_on)
        self._color_preview.setEnabled(color_on)
        self._color_hex.setStyleSheet(
            f"color: {'#555' if color_on else '#2a2a2a'}; "
            "font-size: 12px; font-family: 'JetBrains Mono', monospace;"
        )

    def _on_brightness_change(self):
        if self._current_mode != "off":
            send_command(f"setKeyboard brightness {self._brightness.value()}")

    def _on_delay_change(self):
        if self._current_mode == "rainbow":
            send_command(f"setKeyboard delay {self._speed.value()}")

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
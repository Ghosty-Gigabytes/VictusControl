from PyQt6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QLabel, QPushButton, QFrame
)
from PyQt6.QtCore import Qt, QUrl
from PyQt6.QtGui import QDesktopServices


class AboutDialog(QDialog):

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setWindowTitle("About VictusControl")
        self.setFixedSize(480, 360)
        self.setStyleSheet("""
            QDialog {
                background: #111111;
            }
            QLabel {
                background: transparent;
            }
        """)
        self._build_ui()

    def _build_ui(self):
        root = QVBoxLayout(self)
        root.setContentsMargins(40, 40, 40, 40)
        root.setSpacing(0)

        # ── title ─────────────────────────────────────────────
        title = QLabel("VICTUS<span style='color:#e05c2a'>CONTROL</span>")
        title.setTextFormat(Qt.TextFormat.RichText)
        title.setStyleSheet("""
            font-family: 'JetBrains Mono', 'Fira Code', monospace;
            font-size: 22px;
            font-weight: 800;
            letter-spacing: 5px;
            color: #ffffff;
        """)
        root.addWidget(title)
        root.addSpacing(6)

        # ── subtitle ──────────────────────────────────────────
        subtitle = QLabel(
            "Keyboard backlight and fan control\n"
            "for HP Victus laptops on Linux"
        )
        subtitle.setStyleSheet(
            "color: #555; font-size: 12px; "
            "font-family: 'JetBrains Mono', monospace; line-height: 1.6;"
        )
        root.addWidget(subtitle)
        root.addSpacing(32)

        root.addWidget(self._divider())
        root.addSpacing(24)

        # ── info rows ─────────────────────────────────────────
        info = [
            ("Version",  "1.0.0"),
            ("Author",   "ghosty-gigabytes"),
            ("License",  "GPL-3.0"),
            ("Tested on", "HP Victus 16-s0095ax"),
        ]
        for label, value in info:
            root.addLayout(self._info_row(label, value))
            root.addSpacing(10)

        root.addSpacing(24)
        root.addWidget(self._divider())
        root.addSpacing(24)

        # ── bottom row: github link + close ───────────────────
        bottom = QHBoxLayout()

        github_btn = QPushButton("GitHub ↗")
        github_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        github_btn.setStyleSheet("""
            QPushButton {
                background: rgba(255,255,255,0.05);
                color: #888;
                border: 1px solid rgba(255,255,255,0.08);
                border-radius: 8px;
                padding: 8px 20px;
                font-size: 12px;
                font-family: 'JetBrains Mono', monospace;
            }
            QPushButton:hover {
                background: rgba(255,255,255,0.1);
                color: #ccc;
            }
        """)
        github_btn.clicked.connect(
            lambda: QDesktopServices.openUrl(
                QUrl("https://github.com/ghosty-gigabytes/VictusControl")
            )
        )

        close_btn = QPushButton("Close")
        close_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        close_btn.setStyleSheet("""
            QPushButton {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:1,
                    stop:0 #e05c2a, stop:1 #c93f15);
                color: #ffffff;
                border: none;
                border-radius: 8px;
                padding: 8px 24px;
                font-size: 12px;
                font-weight: 600;
                font-family: 'JetBrains Mono', monospace;
            }
            QPushButton:hover {
                background: qlineargradient(x1:0, y1:0, x2:1, y2:1,
                    stop:0 #f06030, stop:1 #d44a1a);
            }
        """)
        close_btn.clicked.connect(self.accept)

        bottom.addWidget(github_btn)
        bottom.addStretch()
        bottom.addWidget(close_btn)
        root.addLayout(bottom)

    def _info_row(self, label: str, value: str) -> QHBoxLayout:
        row = QHBoxLayout()
        row.setSpacing(0)

        lbl = QLabel(label)
        lbl.setFixedWidth(100)
        lbl.setStyleSheet(
            "color: #444; font-size: 11px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 1px;"
        )

        val = QLabel(value)
        val.setStyleSheet(
            "color: #ccc; font-size: 11px; "
            "font-family: 'JetBrains Mono', monospace;"
        )

        row.addWidget(lbl)
        row.addWidget(val)
        row.addStretch()
        return row

    def _divider(self) -> QFrame:
        line = QFrame()
        line.setFrameShape(QFrame.Shape.HLine)
        line.setStyleSheet(
            "background: rgba(255,255,255,0.06); border: none; max-height: 1px;"
        )
        return line
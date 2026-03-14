import os
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout,
    QHBoxLayout, QLabel, QTabWidget, QFrame
)
from PyQt6.QtCore import Qt, QSize
from ui.keyboard_tab import KeyboardTab
from ui.fan_tab import FanTab


WINDOW_STYLE = """
QMainWindow, QWidget#root {
    background: #111111;
}
QTabWidget::pane {
    border: none;
    background: transparent;
}
QTabWidget::tab-bar {
    alignment: left;
}
QTabBar::tab {
    background: transparent;
    color: #444;
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 11px;
    letter-spacing: 3px;
    font-weight: 700;
    padding: 10px 24px;
    border: none;
    border-bottom: 2px solid transparent;
}
QTabBar::tab:selected {
    color: #e05c2a;
    border-bottom: 2px solid #e05c2a;
}
QTabBar::tab:hover:!selected {
    color: #888;
}
"""


class HeaderBar(QWidget):
    """Top bar with title and daemon connection status."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setFixedHeight(64)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(32, 0, 32, 0)

        title = QLabel("VICTUS<span style='color:#e05c2a'>CONTROL</span>")
        title.setTextFormat(Qt.TextFormat.RichText)
        title.setStyleSheet("""
            font-family: 'JetBrains Mono', 'Fira Code', monospace;
            font-size: 15px;
            font-weight: 800;
            letter-spacing: 4px;
            color: #ffffff;
        """)

        self._dot  = QLabel("●")
        self._text = QLabel("daemon connected")

        self._dot.setStyleSheet("color: #2ecc71; font-size: 10px;")
        self._text.setStyleSheet(
            "color: #2ecc71; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 1px;"
        )

        status_row = QHBoxLayout()
        status_row.setSpacing(6)
        status_row.addWidget(self._dot)
        status_row.addWidget(self._text)

        layout.addWidget(title)
        layout.addStretch()
        layout.addLayout(status_row)

    def set_connected(self, connected: bool):
        color = "#2ecc71" if connected else "#e74c3c"
        text  = "daemon connected" if connected else "daemon offline"
        style = (
            f"color: {color}; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 1px;"
        )
        self._dot.setStyleSheet(f"color: {color}; font-size: 10px;")
        self._text.setStyleSheet(style)
        self._text.setText(text)


class MainWindow(QMainWindow):

    def __init__(self):
        super().__init__()
        self.setWindowTitle("VictusControl")
        self.setMinimumSize(QSize(600, 520))
        self.setStyleSheet(WINDOW_STYLE)
        self._build_ui()
        self._check_daemon()

    def _build_ui(self):
        root = QWidget()
        root.setObjectName("root")
        self.setCentralWidget(root)

        layout = QVBoxLayout(root)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        self._header = HeaderBar()
        layout.addWidget(self._header)

        div = QFrame()
        div.setFrameShape(QFrame.Shape.HLine)
        div.setStyleSheet(
            "background: rgba(255,255,255,0.06); border: none; max-height: 1px;"
        )
        layout.addWidget(div)

        tabs = QTabWidget()
        tabs.addTab(KeyboardTab(), "KEYBOARD")
        tabs.addTab(FanTab(),      "FAN")
        layout.addWidget(tabs)

    def _check_daemon(self):
        connected = os.path.exists("/run/victus-control/victus.sock")
        self._header.set_connected(connected)

    def closeEvent(self, event):
        """Hide to tray instead of quitting."""
        event.ignore()
        self.hide()
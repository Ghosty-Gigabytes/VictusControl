from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout,
    QHBoxLayout, QLabel, QTabWidget, QFrame, QPushButton
)
from PyQt6.QtCore import Qt, QSize, QTimer
from ui.keyboard_tab import KeyboardTab
from ui.fan_tab import FanTab
from ui.about_dialog import AboutDialog


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
    """Top bar with title, daemon status and about button."""

    def __init__(self, on_about, parent=None):
        super().__init__(parent)
        self.setFixedHeight(64)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(32, 0, 32, 0)
        layout.setSpacing(16)

        # title
        title = QLabel("VICTUS<span style='color:#e05c2a'>CONTROL</span>")
        title.setTextFormat(Qt.TextFormat.RichText)
        title.setStyleSheet("""
            font-family: 'JetBrains Mono', 'Fira Code', monospace;
            font-size: 15px;
            font-weight: 800;
            letter-spacing: 4px;
            color: #ffffff;
        """)

        # daemon status
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

        # about button
        about_btn = QPushButton("?")
        about_btn.setFixedSize(28, 28)
        about_btn.setCursor(Qt.CursorShape.PointingHandCursor)
        about_btn.setToolTip("About VictusControl")
        about_btn.setStyleSheet("""
            QPushButton {
                background: rgba(255,255,255,0.05);
                color: #555;
                border: 1px solid rgba(255,255,255,0.08);
                border-radius: 14px;
                font-size: 13px;
                font-weight: 700;
                font-family: 'JetBrains Mono', monospace;
            }
            QPushButton:hover {
                background: rgba(255,255,255,0.1);
                color: #ccc;
            }
        """)
        about_btn.clicked.connect(on_about)

        layout.addWidget(title)
        layout.addStretch()
        layout.addLayout(status_row)
        layout.addWidget(about_btn)

    def set_connected(self, connected: bool):
        color = "#2ecc71" if connected else "#e74c3c"
        text  = "daemon connected" if connected else "daemon offline"
        self._dot.setStyleSheet(f"color: {color}; font-size: 10px;")
        self._text.setStyleSheet(
            f"color: {color}; font-size: 10px; "
            "font-family: 'JetBrains Mono', monospace; letter-spacing: 1px;"
        )
        self._text.setText(text)


class MainWindow(QMainWindow):

    # poll daemon socket every 5 seconds
    POLL_INTERVAL_MS = 5000

    def __init__(self):
        super().__init__()
        self.setWindowTitle("VictusControl")
        self.setMinimumSize(QSize(600, 520))
        self.setStyleSheet(WINDOW_STYLE)
        self._build_ui()
        self._start_daemon_polling()

    def _build_ui(self):
        root = QWidget()
        root.setObjectName("root")
        self.setCentralWidget(root)

        layout = QVBoxLayout(root)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        self._header = HeaderBar(on_about=self._show_about)
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

    # ── daemon polling ────────────────────────────────────────

    def _start_daemon_polling(self):
        """Check daemon connection on startup and every 5 seconds after."""
        self._poll_daemon()   # immediate first check

        self._poll_timer = QTimer(self)
        self._poll_timer.setInterval(self.POLL_INTERVAL_MS)
        self._poll_timer.timeout.connect(self._poll_daemon)
        self._poll_timer.start()

    def _poll_daemon(self):
        connected = self._is_daemon_alive()
        self._header.set_connected(connected)

    def _is_daemon_alive(self) -> bool:
        """Try to actually connect to the socket — file existing is not enough."""
        import socket as _socket
        try:
            sock = _socket.socket(_socket.AF_UNIX, _socket.SOCK_STREAM)
            sock.settimeout(1.0)
            sock.connect("/run/victus-control/victus.sock")
            sock.close()
            return True
        except Exception:
            return False

    # ── about dialog ──────────────────────────────────────────

    def _show_about(self):
        dialog = AboutDialog(self)
        dialog.exec()

    # ── window behaviour ──────────────────────────────────────

    def closeEvent(self, event):
        """Hide to tray instead of quitting."""
        event.ignore()
        self.hide()
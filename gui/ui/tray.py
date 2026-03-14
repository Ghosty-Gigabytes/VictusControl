from PyQt6.QtWidgets import QSystemTrayIcon, QMenu
from PyQt6.QtGui import QIcon, QPixmap, QPainter, QColor, QBrush
from PyQt6.QtCore import QSize
from ipc.client import send_command


def _make_tray_icon() -> QIcon:
    """Generate a simple orange circle icon for the tray."""
    px = QPixmap(QSize(22, 22))
    px.fill(QColor(0, 0, 0, 0))
    painter = QPainter(px)
    painter.setRenderHint(QPainter.RenderHint.Antialiasing)
    painter.setBrush(QBrush(QColor("#e05c2a")))
    painter.setPen(QColor(0, 0, 0, 0))
    painter.drawEllipse(2, 2, 18, 18)
    painter.end()
    return QIcon(px)


class TrayIcon(QSystemTrayIcon):

    def __init__(self, main_window, parent=None):
        super().__init__(_make_tray_icon(), parent)
        self._window = main_window
        self._build_menu()
        self.activated.connect(self._on_activated)
        self.setToolTip("VictusControl")

    def _build_menu(self):
        menu = QMenu()
        menu.setStyleSheet("""
            QMenu {
                background: #1a1a1a;
                border: 1px solid rgba(255,255,255,0.1);
                border-radius: 8px;
                padding: 4px;
                color: #ccc;
                font-family: 'JetBrains Mono', monospace;
                font-size: 12px;
            }
            QMenu::item { padding: 6px 20px; border-radius: 4px; }
            QMenu::item:selected { background: rgba(224,92,42,0.3); color: white; }
            QMenu::separator { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 0; }
        """)

        menu.addAction("Open VictusControl", self._show_window)
        menu.addSeparator()

        kb_menu = menu.addMenu("Keyboard")
        kb_menu.setStyleSheet(menu.styleSheet())
        kb_menu.addAction("Rainbow", lambda: send_command("setKeyboard rainbow 255 20"))
        kb_menu.addAction("Off",     lambda: send_command("setKeyboard off"))

        fan_menu = menu.addMenu("Fan")
        fan_menu.setStyleSheet(menu.styleSheet())
        fan_menu.addAction("Auto",   lambda: send_command("setFan auto"))
        fan_menu.addAction("Max",    lambda: send_command("setFan max"))

        menu.addSeparator()
        menu.addAction("Quit", self._quit)

        self.setContextMenu(menu)

    def _on_activated(self, reason):
        if reason == QSystemTrayIcon.ActivationReason.Trigger:
            self._show_window()

    def _show_window(self):
        self._window.show()
        self._window.raise_()
        self._window.activateWindow()

    def _quit(self):
        from PyQt6.QtWidgets import QApplication
        QApplication.quit()
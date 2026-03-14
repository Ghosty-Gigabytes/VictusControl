import sys
import os

# ensure all gui subpackages resolve regardless of working directory
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PyQt6.QtWidgets import QApplication
from PyQt6.QtCore import Qt
from ui.main_window import MainWindow
from ui.tray import TrayIcon


def main():
    QApplication.setHighDpiScaleFactorRoundingPolicy(
        Qt.HighDpiScaleFactorRoundingPolicy.PassThrough
    )

    app = QApplication(sys.argv)
    app.setApplicationName("VictusControl")
    app.setApplicationDisplayName("VictusControl")
    app.setQuitOnLastWindowClosed(False)  # keep alive in tray

    window = MainWindow()

    if app.platformName() != "offscreen":
        tray = TrayIcon(window)
        tray.show()

    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
import time
import json
from PyQt6.QtCore import QThread, pyqtSignal
from ipc.client import send_query


class FanPoller(QThread):
    """
    Background thread that queries the daemon for live fan RPM every 2 seconds
    and emits a signal with updated values.
    Never update UI directly from this thread — use the signal.
    """

    rpm_updated = pyqtSignal(int, int)   # fan1_rpm, fan2_rpm
    error = pyqtSignal(str)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._running = True

    def run(self):
        while self._running:
            response = send_query("getFan")
            if response:
                try:
                    data = json.loads(response)
                    fan1 = int(data.get("fan1Input", 0))
                    fan2 = int(data.get("fan2Input", 0))
                    self.rpm_updated.emit(fan1, fan2)
                except Exception as e:
                    self.error.emit(f"Parse error: {e}")
            else:
                self.error.emit("Daemon not responding")

            time.sleep(2.0)

    def stop(self):
        self._running = False
        self.wait()
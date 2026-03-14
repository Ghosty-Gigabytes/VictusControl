import socket

SOCKET_PATH = "/run/victus-control/victus.sock"


def send_command(command: str) -> bool:
    """
    Send a command to the daemon. No response expected.
    Returns True on success, False on failure.
    """
    try:
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.settimeout(2.0)
        sock.connect(SOCKET_PATH)
        sock.sendall((command + "\n").encode())
        sock.shutdown(socket.SHUT_WR)
        sock.close()
        return True
    except FileNotFoundError:
        print(f"[SocketClient] Socket not found at {SOCKET_PATH} — is the daemon running?")
        return False
    except ConnectionRefusedError:
        print("[SocketClient] Connection refused — daemon may have crashed.")
        return False
    except Exception as e:
        print(f"[SocketClient] Error: {e}")
        return False


def send_query(command: str) -> str | None:
    """
    Send a command and read a JSON response.
    Returns the response string or None on failure.
    """
    try:
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.settimeout(2.0)
        sock.connect(SOCKET_PATH)
        sock.sendall((command + "\n").encode())
        sock.shutdown(socket.SHUT_WR)

        response = b""
        while chunk := sock.recv(4096):
            response += chunk
        sock.close()
        return response.decode().strip()
    except FileNotFoundError:
        print(f"[SocketClient] Socket not found at {SOCKET_PATH} — is the daemon running?")
        return None
    except Exception as e:
        print(f"[SocketClient] Query error: {e}")
        return None
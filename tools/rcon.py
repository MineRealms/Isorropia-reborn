"""Minimal RCON client, enough to stop a dev server gracefully.

    python tools/rcon.py stop
"""
import socket
import struct
import sys


def send(sock, ident, kind, body):
    payload = struct.pack('<ii', ident, kind) + body.encode('utf-8') + b'\x00\x00'
    sock.sendall(struct.pack('<i', len(payload)) + payload)
    length = struct.unpack('<i', sock.recv(4))[0]
    data = b''
    while len(data) < length:
        data += sock.recv(length - len(data))
    return data[8:-2].decode('utf-8', 'replace')


def main():
    command = ' '.join(sys.argv[1:]) or 'list'
    with socket.create_connection(('127.0.0.1', 25575), timeout=10) as sock:
        send(sock, 1, 3, 'isoropia')
        print(send(sock, 2, 2, command))


if __name__ == '__main__':
    main()

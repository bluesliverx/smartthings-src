#!/usr/bin/env python2

import socket
import sys

TCP_IP = '192.168.1.104'
TCP_PORT = 8899
BUFFER_SIZE = 1024
messages = {
    'all-on':       bytearray([0x55,0xaa,0x15,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x03,0x00,0x00,0x00,0xea]),
    'all-off':      bytearray([0x55,0xaa,0x17,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x00,0x00,0x00,0x00,0xea]),
    'z1offz2on':    bytearray([0x55,0xaa,0x05,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x02,0x00,0x00,0x00,0xea]),
    'z1onz2off':    bytearray([0x55,0xaa,0x2f,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x01,0x00,0x00,0x00,0xea]),
#                                                                  zone           r    g    b                               
    'zone1-blue5r': bytearray([0x55,0xaa,0x0e,0x04,0x01,0x11,0x0e,0x59,0x01,0x00,0x05,0x05,0x00,0xff,0x00,0xff,0x00,0x00,0xea]),
    'zone1-r':      bytearray([0x55,0xaa,0x09,0x04,0x01,0x11,0x0e,0x59,0x01,0x00,0x05,0xff,0x02,0x02,0x00,0xff,0x00,0x00,0xea]),
    'zone2-r':      bytearray([0x55,0xaa,0x39,0x04,0x01,0x11,0x0e,0x59,0x02,0x00,0x05,0xff,0x02,0x02,0x00,0xff,0x00,0x00,0xea]),
    'zone1-b':      bytearray([0x55,0xaa,0x11,0x04,0x01,0x11,0x0e,0x59,0x01,0x00,0x05,0x00,0x00,0xff,0x00,0xff,0x00,0x00,0xea]),
    'zone1-g':      bytearray([0x55,0xaa,0x16,0x04,0x01,0x11,0x0e,0x59,0x01,0x00,0x05,0x00,0xff,0x00,0x00,0xff,0x00,0x00,0xea]),
    'zone1-rg':     bytearray([0x55,0xaa,0x27,0x04,0x01,0x11,0x0e,0x59,0x01,0x00,0x05,0xfb,0xff,0x00,0x00,0xff,0x00,0x00,0xea]),
}

def get_color_command(zone, r, g, b):
    return bytearray([0x55,0xaa,0x0e,0x04,0x01,0x11,0x0e,0x59,zone,0x00,0x05,r,g,b,0x00,0xff,0x00,0x00,0xea])

if sys.argv[1]=='set-color':
    message = get_color_command(int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4]), int(sys.argv[5]))
else:
    message = messages[sys.argv[1]]

print('Sending data:', message)
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((TCP_IP, TCP_PORT))
s.send(message)
data = s.recv(BUFFER_SIZE)
s.close()

print("received data:", data)
#!/usr/bin/env python2.7

from lib.lights_http_server import LightsHTTPServer
import logging
import platform

LIGHTS_ENDPOINT = '192.168.1.104'
LIGHTS_ENDPOINT_PORT = 8899
LIGHTS_INTERNAL_ADDRESS = [0x11, 0x0e, 0x59]

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
if platform.system()=='Darwin':
    logging.basicConfig()
else:
    logging.basicConfig(filename='/var/log/wifi-104-server.log')

lights_http_server = LightsHTTPServer(1050, LIGHTS_ENDPOINT, LIGHTS_ENDPOINT_PORT, LIGHTS_INTERNAL_ADDRESS)
lights_http_server.start()

#!/usr/bin/env python2

from lib.ssdp import SSDPServer
from lib.upnp_http_server import UPNPHTTPServer
from lib.lights_http_server import LightsHTTPServer
import uuid
import netifaces as ni
from time import sleep
import logging
import platform

DEVICE_UUID = 'f9d30a08-c55c-45bc-86ff-4905058d5cb2'
LIGHTS_ENDPOINT = '192.168.1.104'
LIGHTS_ENDPOINT_PORT = 8899
LIGHTS_INTERNAL_ADDRESS = [0x11, 0x0e, 0x59]

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
logging.basicConfig()


def setup_debugging():
    """
    Load PyCharm's egg and start a remote debug session.
    :return: None
    """
    import sys
    sys.path.append('/root/pycharm-debug-py3k.egg')
    import pydevd
    pydevd.settrace('192.168.4.47', port=5422, stdoutToServer=True, stderrToServer=True, suspend=False)


#setup_debugging()

def get_network_interface():
    if platform.system()=='Darwin':
        return 'en0'
    else:
        return 'eth0'

def get_network_interface_ip_address(interface):
    """
    Get the first IP address of a network interface.
    :param interface: The name of the interface.
    :return: The IP address.
    """
    while True:
        if interface not in ni.interfaces():
            logger.error('Could not find interface %s.' % (interface,))
            exit(1)
        interface = ni.ifaddresses(interface)
        if (2 not in interface) or (len(interface[2]) == 0):
            logger.warning('Could not find IP of interface %s. Sleeping.' % (interface,))
            sleep(60)
            continue
        return interface[2][0]['addr']


device_uuid = DEVICE_UUID if DEVICE_UUID else uuid.uuid4() 
local_ip_address = get_network_interface_ip_address(get_network_interface())

lights_http_server = LightsHTTPServer(1050, LIGHTS_ENDPOINT, LIGHTS_ENDPOINT_PORT, LIGHTS_INTERNAL_ADDRESS)
lights_http_server.start()

upnp_http_server = UPNPHTTPServer(1049,
                             friendly_name="LTech WiFi 104",
                             manufacturer="LTech Lighting Technology",
                             manufacturer_url='http://www.ltechonline.com/',
                             model_description='LED WiFi Controller WiFi-104',
                             model_name="WiFi",
                             model_number="104",
                             model_url="http://www.ltechonline.com/html/en/products/Wireless-LED-Controller/WiFi-Controller/WiFi-104.html",
                             serial_number="abc123",
                             uuid=device_uuid,
                             presentation_url="http://{}:1050/".format(local_ip_address))
upnp_http_server.start()

ssdp = SSDPServer()
ssdp.register(manifestation='local',
              usn='uuid:{}::upnp:rootdevice'.format(device_uuid),
              st='urn:schemas-upnp-org:device:LTechWiFi104:1',
              location='http://{}:1049/ltech-wifi-104.xml'.format(local_ip_address))
ssdp.run()

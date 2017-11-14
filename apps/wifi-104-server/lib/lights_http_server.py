from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from urlparse import urlparse
import threading
import logging
import json
import socket

PORT_NUMBER = 1050
BUFFER_SIZE = 1024

logger = logging.getLogger('lights')

class LightsHTTPServerHandler(BaseHTTPRequestHandler):
  """
  A HTTP handler that routes commands to lights.
  """

  @classmethod
  def get_frame_index(cls):
    if not hasattr(cls, 'frame_index'):
      cls.frame_index = 0
    ret = cls.frame_index
    cls.frame_index += 1
    if cls.frame_index > 255:
      cls.frame_index = 0
    return ret

  # Handler for the GET requests
  def do_PUT(self):
    response_code = 200
    params = self.get_query_params()

    if self.path == '/lights/zones/all/on':
      message = self.turn_lights_on()
    elif self.path == '/lights/zones/all/off':
      message = self.turn_lights_off()
    elif self.path.startswith('/lights/zones/1/on'):
      message = self.turn_lights_on(1)
    elif self.path.startswith('/lights/zones/1/off'):
      message = self.turn_lights_off(1)
    elif self.path.startswith('/lights/zones/1/color'):
      if 'red' not in params or 'green' not in params or 'blue' not in params or 'white' not in params:
        response_code = 400
        message = 'Please include "red", "green", "blue", and "white" params as integers'
      else:
        # Default to full brightness
        brightness = 0xff
        if 'brightness' in params:
          brightness = int(params['brightness'])
        message = self.set_color(1, int(params['red']), int(params['green']), int(params['blue']), int(params['white']), brightness)
    elif self.path.startswith('/lights/zones/2/on'):
      message = self.turn_lights_on(2)
    elif self.path.startswith('/lights/zones/2/off'):
      message = self.turn_lights_off(2)
    elif self.path.startswith('/lights/zones/2/color'):
      if 'red' not in params or 'green' not in params or 'blue' not in params or 'white' not in params:
        response_code = 400
        message = 'Please include "red", "green", "blue", and "white" params as integers'
      else:
        # Default to full brightness
        brightness = 0xff
        if 'brightness' in params:
          brightness = int(params['brightness'])
        message = self.set_color(2, int(params['red']), int(params['green']), int(params['blue']), int(params['white']), brightness)
    else:
      response_code = 404
      message = 'Not found'
    self.send_response(response_code)
    self.send_header('Content-type', 'application/json')
    self.end_headers()
    self.wfile.write(json.dumps({'message':message}).encode())

  def get_query_params(self):
    query = urlparse(self.path).query
    if not query:
      return {}
    return dict(qc.split("=") for qc in query.split("&"))

  def turn_lights_on(self, zone=None):
    logger.info('Turning lights on for zone %s', zone)
    all_zones = self.get_zone_mask(zone)
    zone_status = self.get_zone_status(True, True)
    # Send command twice since this is what the java app does - maybe something with frame index incrementing?
    command = self.create_command(0x0f, 0x08, all_zones, zone_status)
    self.send_command(command)
    self.send_command(command)
    #self.send_command(command, receive=True)
    return 'Successfully turned lights on'

  def turn_lights_off(self, zone=None):
    logger.info('Turning lights off for zone %s', zone)
    all_zones = self.get_zone_mask(zone)
    zone_status = self.get_zone_status(False, False)
    # Send command twice since this is what the java app does - maybe something with frame index incrementing?
    command = self.create_command(0x0f, 0x08, all_zones, zone_status)
    self.send_command(command)
    self.send_command(command)
    #self.send_command(command, receive=True)
    return 'Successfully turned lights off'

  def set_color(self, zone, red, green, blue, white, brightness):
    logger.info('Setting color for zone %d to red %s green %d blue %d white %d brightness %d', 
      zone, red, green, blue, white, brightness)
    command = self.create_command(0x04, 0x01,
      # Only target the one zone
      self.get_zone_mask(zone),
      # Arguments: RGBW + brightness
      [red, green, blue, white, brightness]
    )
    self.send_command(command)
    return 'Successfully set color for zone {}'.format(zone)

  def get_zone_mask(self, zone=None):
    if not zone:
      return [255, 255]
    return [zone, 0]

  def get_zone_status(self, zone1On, zone2On):
    if zone1On and zone2On:
      return [0x03, 0x00]
    elif zone1On:
      return [0x01, 0x00]
    elif zone2On:
      return [0x02, 0x00]
    return [0x00, 0x00]

  def create_command(self, command1, command2, zones, args=[]):
    # Begin command
    #ret = [85, -86]
    ret = [0x55, 0xaa]
    # Add frame index (auto incrementing)
    ret.append(LightsHTTPServerHandler.get_frame_index())
    # Add commands
    ret.extend([command1, command2])
    # Add "internal" address
    ret.extend(self.server.internal_address)
    # Add zones (255 for all zones)
    if len(zones) != 2:
      raise Exception('zones parameter should be an array of 2 ints')
    ret.extend(zones)
    
    # Add number of args
    num_args = 0
    if command2==8:
      # Control zones
      num_args = 2
    elif command2==1:
      # RGBW + brightness
      num_args = 5
    ret.append(num_args)
    # Add args and make sure they match
    if len(args)!=num_args:
      raise Exception('The number of arguments passed ({}) does not match the number expected for command {} ({})'.format(len(args), command2, num_args))
    ret.extend(args)

    # End command
    #ret.extend([0, 0, -22])
    ret.extend([0x00, 0x00, 0xea])
    return ret

  def send_command(self, command, receive=False):
    command_bytes = bytearray(command)
    logger.debug('Sending command to endpoint %s:%d - %s', self.server.endpoint, self.server.endpoint_port, command)
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((self.server.endpoint, self.server.endpoint_port))
    s.send(command_bytes)
    if receive:
      logger.debug('Waiting for response from endpoint')
      data = s.recv(BUFFER_SIZE)
      logger.debug('Received %s from endpoint', data)
    s.close()
    

class LightsHTTPServerBase(HTTPServer):
  """
  A simple HTTP server that handles LTech WiFi 104 light controller commands.
  """
  def __init__(self, server_address, request_handler_class):
    HTTPServer.__init__(self, server_address, request_handler_class)
    self.port = None
    self.endpoint = None
    self.endpoint_port = None
    self.internal_address = None


class LightsHTTPServer(threading.Thread):
  """
  A thread that runs LightsHTTPServerBase.
  """

  def __init__(self, port, endpoint, endpoint_port, internal_address):
    threading.Thread.__init__(self)
    self.daemon = True
    self.server = LightsHTTPServerBase(('', port), LightsHTTPServerHandler)
    self.server.port = port
    self.server.endpoint = endpoint
    self.server.endpoint_port = endpoint_port
    self.server.internal_address = internal_address

  def run(self):
    self.server.serve_forever()

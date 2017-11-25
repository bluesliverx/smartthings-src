#!/usr/bin/env python2.7

from flask import Flask, request, abort, jsonify
from timeout_decorator import timeout
import logging
import socket
import platform
import os

# App for uwsgi
app = Flask(__name__)

ENDPOINT = os.getenv('ENDPOINT', '192.168.1.104')
ENDPOINT_PORT = 8899
INTERNAL_ADDRESS = [0x11, 0x0e, 0x59]
BUFFER_SIZE = 1024

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
if platform.system()=='Darwin':
    logging.basicConfig()
else:
    logging.basicConfig(filename='/var/log/uwsgi/wifi-104-server.log')

frame_index = 0
def get_frame_index():
    """
    Retrieve and increment the frame index, wrapping around if needed.
    """
    global frame_index
    ret = frame_index
    frame_index += 1
    if frame_index > 255:
        frame_index = 0
    return ret

def message_response(message):
    return jsonify({'message':message})

@app.route('/zones/<zone>/on', methods=['PUT'])
def zone_on(zone):
    return message_response(switch_lights(zone, True))

@app.route('/zones/<zone>/off', methods=['PUT'])
def zone_off(zone):
    return message_response(switch_lights(zone, False))

@app.route('/zones/<zone>/color', methods=['PUT'])
def zone_color(zone):
    if 'red' not in request.args or 'green' not in request.args or 'blue' not in request.args or 'white' not in request.args:
        return app.response_class(
            response=json.dumps({'message':'Please include "red", "green", "blue", and "white" query params as integers'}),
            status=400,
            mimetype='application/json'
        )
    # Default to full brightness
    message = set_color(
        zone,
        convert_color(request.args.get('red')),
        convert_color(request.args.get('green')),
        convert_color(request.args.get('blue')),
        convert_color(request.args.get('white')),
        convert_color(request.args.get('brightness', 0xff)),
    )
    return message_response(message)

@app.route('/zones/all/status')
def status():
    logger.info('Retrieving status')
    # While 1 arg is expected, it is never set in the app, so set it explicitly to 0
    response = send_command(create_command(0x0f, 0x09, get_zone_mask('all'), [0x00]), receive=True)
    zone_statuses1 = int(response[11])
    zone_statuses2 = int(response[12]) >> 4
    logger.info('Zone statuses are {} and {}'.format(zone_statuses1, zone_statuses2))
    statuses = {}
    for zone in range(0, 8):
        statuses[zone+1] = bool((1 << zone) & zone_statuses1)
    for zone in range(8, 12):
        statuses[zone+1] = bool((1 << (zone - 8)) & zone_statuses2)
    
    return jsonify({'zones':statuses})
    
def convert_color(value):
    if type(value) is int:
        return value
    if not value or value=='null':
        return 0
    return int(value)

def switch_lights(zone=None, on=True):
    logger.info('Turning lights on for zone %s', zone)
    all_zones = get_zone_mask(zone)
    zone_status = get_zone_status(on, on)
    # Send command twice since this is what the java app does - maybe something with frame index incrementing?
    send_command(create_command(0x0f, 0x08, all_zones, zone_status))
    send_command(create_command(0x0f, 0x08, all_zones, zone_status), receive=True)
    return 'Successfully turned lights {}'.format('on' if on else 'off')

def set_color(zone, red, green, blue, white, brightness):
    logger.info('Setting color for zone %d to red %s green %d blue %d white %d brightness %d', 
        zone, red, green, blue, white, brightness)
    command = create_command(0x04, 0x01,
        # Only target the one zone
        get_zone_mask(zone),
        # Arguments: RGBW + brightness
        [red, green, blue, white, brightness]
    )
    send_command(command, receive=True)
    return 'Successfully set color for zone {}'.format(zone)

def get_zone_mask(zone=None):
    if zone=='all':
        return [255, 255]
    return [int(zone), 0]

def get_zone_status(zone1On, zone2On):
    if zone1On and zone2On:
        return [0x03, 0x00]
    elif zone1On:
        return [0x01, 0x00]
    elif zone2On:
        return [0x02, 0x00]
    return [0x00, 0x00]

def create_command(command1, command2, zones, args=[]):
    # Begin command
    #ret = [85, -86]
    ret = [0x55, 0xaa]
    # Add frame index (auto incrementing)
    ret.append(get_frame_index())
    # Add commands
    ret.extend([command1, command2])
    # Add "internal" address
    ret.extend(INTERNAL_ADDRESS)
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
    elif command2==9:
        # Query zone status
        num_args = 1
    ret.append(num_args)
    # Add args and make sure they match
    if len(args)!=num_args:
        raise Exception('The number of arguments passed ({}) does not match the number expected for command {} ({})'.format(len(args), command2, num_args))
    ret.extend(args)

    # End command
    #ret.extend([0, 0, -22])
    ret.extend([0x00, 0x00, 0xea])
    return ret

def send_command(command, receive=False):
    global ENDPOINT
    global ENDPOINT_PORT
    command_bytes = bytearray(command)
    frame_index = command[2]
    logger.debug('Sending command %d to endpoint %s:%d - %s', frame_index, ENDPOINT, ENDPOINT_PORT, command)

    response = None
    tries = 3
    while not response and tries > 0:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            s.connect((ENDPOINT, ENDPOINT_PORT))
            s.send(command_bytes)
            if not receive:
                return None
            try:
                response = receive_response(s, frame_index)
                # Do not return if there is no response (the frame index did not match)
                if response:
                    return response
            except Exception as e:
                logger.warn(str(e))
                logger.warn('Timed out while waiting for a response, resending command {} more times'.format(tries))
            tries -= 1
        finally:
            s.close()
    raise Exception('No response received from the controller')
    
@timeout(5)
def receive_response(s, expected_frame_index):
    global BUFFER_SIZE
    logger.debug('Waiting for response from endpoint')
    data = s.recv(BUFFER_SIZE)
    byte_data = bytearray(data)
    frame_index = int(byte_data[2])
    logger.debug('Received %s from endpoint (frame index %d)', list(data), frame_index)
    if frame_index!=expected_frame_index:
        logger.debug('Frame index received ({}) does not match expected ({}), ignoring'.format(frame_index, expected_frame_index))
        return None
    return byte_data


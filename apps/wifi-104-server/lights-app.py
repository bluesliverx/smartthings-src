#!/usr/bin/env python2.7

from flask import Flask, request, abort, jsonify
#from logging.handlers import RotatingFileHandler
import logging
import socket
import platform

# App for uwsgi
app = Flask(__name__)

ENDPOINT = '192.168.1.104'
ENDPOINT_PORT = 8899
INTERNAL_ADDRESS = [0x11, 0x0e, 0x59]

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
    return message_response(turn_lights_on(zone))

@app.route('/zones/<zone>/off', methods=['PUT'])
def zone_off(zone):
    return message_response(turn_lights_off(zone))

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
    
def convert_color(value):
    if type(value) is int:
        return value
    if not value or value=='null':
        return 0
    return int(value)

def turn_lights_on(zone=None):
    logger.info('Turning lights on for zone %s', zone)
    all_zones = get_zone_mask(zone)
    zone_status = get_zone_status(True, True)
    # Send command twice since this is what the java app does - maybe something with frame index incrementing?
    command = create_command(0x0f, 0x08, all_zones, zone_status)
    send_command(command)
    send_command(command)
    #send_command(command, receive=True)
    return 'Successfully turned lights on'

def turn_lights_off(zone=None):
    logger.info('Turning lights off for zone %s', zone)
    all_zones = get_zone_mask(zone)
    zone_status = get_zone_status(False, False)
    # Send command twice since this is what the java app does - maybe something with frame index incrementing?
    command = create_command(0x0f, 0x08, all_zones, zone_status)
    send_command(command)
    send_command(command)
    #send_command(command, receive=True)
    return 'Successfully turned lights off'

def set_color(zone, red, green, blue, white, brightness):
    logger.info('Setting color for zone %d to red %s green %d blue %d white %d brightness %d', 
        zone, red, green, blue, white, brightness)
    command = create_command(0x04, 0x01,
        # Only target the one zone
        get_zone_mask(zone),
        # Arguments: RGBW + brightness
        [red, green, blue, white, brightness]
    )
    send_command(command)
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
    global BUFFER_SIZE
    command_bytes = bytearray(command)
    logger.debug('Sending command to endpoint %s:%d - %s', ENDPOINT, ENDPOINT_PORT, command)
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((ENDPOINT, ENDPOINT_PORT))
    s.send(command_bytes)
    if receive:
        logger.debug('Waiting for response from endpoint')
        data = s.recv(BUFFER_SIZE)
        logger.debug('Received %s from endpoint', data)
    s.close()


# If logging does not work in prod with uwsgi, we can try this
#if __name__ == '__main__':
#    handler = RotatingFileHandler('foo.log', maxBytes=10000, backupCount=1)
#    handler.setLevel(logging.INFO)
#    app.logger.addHandler(handler)
#    app.run()

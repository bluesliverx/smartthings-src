/**
 *	Wifi 104 Integration (Local)
 *
 *	Copyright 2017 Brian Saville
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */

def version() {
    return "1.0\n© 2017 Brian Saville"
}

preferences {
    input("mac", "string", title: "MAC Address (no colons)", required: true, displayDuringSetup: true)
    input(title:"", description: "Version: ${version()}", type: "paragraph", element: "paragraph")
}

metadata {
    definition (name: "Wifi 104 Local", namespace: "bluesliverx", author: "Brian Saville") {
        capability "Color Control"
        capability "Switch"
        capability "Refresh"

//        attribute "energy_str", "string"
//        attribute "energy_yesterday", "string"
//        attribute "energy_last7days", "string"
//        attribute "energy_life", "string"
//        attribute "power_details", "string"
//        attribute "efficiency", "string"
//        attribute "efficiency_yesterday", "string"
//        attribute "efficiency_last7days", "string"

        attribute("zone1Switch", "enum", ["on", "off"])
        attribute("zone2Switch", "enum", ["on", "off"])
        attribute("zone1Color", "string")
        attribute("zone2Color", "string")

        command "zone1On"
        command "zone2On"
        command "zone1Off"
        command "zone2Off"
        command "lightsOn"
        command "lightsOff"
        command "setZone1Color"
        command "setZone2Color"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        valueTile("zone1Color", "device.color") {
            state "color", label: '${currentValue}', defaultState: true
        }
        controlTile("zone1ColorControl", "device.color", "color", height: 6, width: 6, inactiveLabel: false) {
            state "color", action: "setZone1Color"
        }
        multiAttributeTile(name:"zone1", type: "lighting", width: 6, height: 4) {
            tileAttribute ("zone1Switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"zone1Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"zone1On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"zone1Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"zone1On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("zone1Color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setZone1Color"
            }
        }
        multiAttributeTile(name:"zone2", width: 6, height: 4) {
            tileAttribute ("zone2Switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"zone2Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"zone2On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"zone2Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"zone2On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("zone2Color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setZone2Color"
            }
        }

        main "zone1Color"
        details(["zone1ColorControl", "zone1", "zone2", "refresh"])
    }
}

def updated() {
    log.info("Updated")
//    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
//        state.updatedLastRanAt = now()
//        log.trace("$device.displayName - updated() called with settings: ${settings.inspect()}")
//        state.remove('api')
//        state.remove('installationDate')
//        state.maxPower = settings.confNumInverters * settings.confInverterSize
//        // Notify health check about this device with timeout interval equal to 5 failed update requests
//        // (add 30 seconds so we don't collide with the 5th request in case that succeeds)
//        def healthCheckInterval = 5 * settings.pollingInterval.toInteger() * 60 + 30
//        sendEvent(name: "checkInterval", value: healthCheckInterval, data: [protocol: "lan", hubHardwareId: device.hub.hardwareID], displayed: false)
//        log.trace("$device.displayName - setting checkInterval for device health check to ${healthCheckInterval} seconds")
//        pullData()
//    } else {
//        log.trace("$device.displayName - updated() ran within the last 2 seconds - skipping")
//    }
}

private def updateDNI() {
    if (!state.dni || state.dni != device.deviceNetworkId || (settings.mac && settings.mac != device.deviceNetworkId)) {
        log.debug("Setting DNI to ${settings.mac}")
        device.setDeviceNetworkId(settings.mac)
        state.dni = device.deviceNetworkId
    }
}

def parse(String description) {
    log.info("Parse ${description}")
}

def refresh() {
    log.debug("Refresh")
}

def zone1On() {
    log.debug("Zone 1 on")
    sendEvent(name:"zone1Switch", value:"on")
}

def zone1Off() {
    log.debug("Zone 1 off")
    sendEvent(name:"zone1Switch", value:"off")
}

def setZone1Color(value) {
    log.debug("set zone 1 color to ${value}")
    if (setColor(1, value)) {
        sendEvent(name: "zone1Color", value: value.hex)
    }
}

def zone2On() {
    log.debug("Zone 2 on")
    sendEvent(name:"zone2Switch", value:"on")
}

def zone2Off() {
    log.debug("Zone 2 off")
    sendEvent(name:"zone2Switch", value:"off")
}

def setZone2Color(value) {
    log.debug("set zone 2 color to ${value}")
    if (setColor(2, value)) {
        sendEvent(name: "zone2Color", value: value.hex)
    }
}

//def toggleOffColorTiles() {
//    sendEvent(name: "zone1Color", value: device.zone1Color, displayed: false, isStateChange: true)
//    sendEvent(name: "zone2Color", value: device.zone2Color, displayed: false, isStateChange: true)
//}

// Return hex-string interpretation of byte array
public String bytesToHex(byte[] bytes) {
    final char[] hexArray = "0123456789ABCDEF".toCharArray();
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
}


// Commands to send to the wifi 104

def lightsOn() {
    log.info("Turning lights on")
    sendCommand([0x55,0xaa,0x15,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x03,0x00,0x00,0x00,0xea])
}

def lightsOff() {
    log.info("Turning lights off")
    sendCommand([0x55,0xaa,0x17,0x0f,0x08,0x11,0x0e,0x59,0xff,0xff,0x02,0x00,0x00,0x00,0x00,0xea])
}

def setColor(int zone, value) {
    log.info "setColor: ${value}"

    if (value.hex) {
        log.info "setting color with hex"
        def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
        sendCommand([0x55,0xaa,0x0e,0x04,0x01,0x11,0x0e,0x59,zone,0x00,0x05,c[0],c[1],c[2],0x00,0xff,0x00,0x00,0xea])
    } else {
        log.info "setting color with hue & saturation"
        log.error "Can't do this yet, still needs implementing"
//        def hue = value.hue ?: device.currentValue("hue")
//        def saturation = value.saturation ?: device.currentValue("saturation")
//        if (hue == null) hue = 13
//        if (saturation == null) saturation = 13
//        def rgb = huesatToRGB(hue as Integer, saturation as Integer)
//        def whiteValue = device.currentValue("colorTemperature")
//        result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2])
    }

//    if (value.hue) sendEvent(name: "hue", value: value.hue, displayed: false)
//    if (value.hex) sendEvent(name: "color", value: value.hex, displayed: false)
//    if (value.switch) sendEvent(name: "switch", value: value.switch, displayed: false)
//    if (value.saturation) sendEvent(name: "saturation", value: value.saturation, displayed: false)

//    commands(result)
}

private byte[] hexListToByteArray(List bytes) {
    int len = bytes.size();
    byte[] data = new byte[len];
    for (int i = 0; i < len; i++) {
        data[i] = (byte) bytes[i];
    }
    return data;
}

private void sendCommand(List command) {
    updateDNI()

    byte[] bytes = hexListToByteArray(command)
    String strCommand = new String(bytes, "ISO-8859-1")
    log.info "${command.length} ${strCommand} to MAC ${getDataValue('mac')}"
    sendHubCommand(new physicalgraph.device.HubAction(strCommand, physicalgraph.device.Protocol.LAN, getDataValue("mac")))
}

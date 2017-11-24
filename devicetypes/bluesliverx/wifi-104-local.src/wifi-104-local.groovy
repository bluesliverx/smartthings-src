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
    input("host", "string", title: "IP:Port", required: true, displayDuringSetup: true)
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

        attribute("allZonesSwitch", "enum", ["on", "off", "turningOff", "turningOn"])
        attribute("zone1Switch", "enum", ["on", "off", "turningOff", "turningOn"])
        attribute("zone2Switch", "enum", ["on", "off", "turningOff", "turningOn"])
        attribute("zone1Color", "string")
        attribute("zone1Level", "number")
        attribute("zone2Color", "string")
        attribute("zone2Level", "number")

        command "allZonesOn"
        command "allZonesOff"
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
        valueTile("allZonesValue", "allZonesSwitch") {
            state "on", label:'On', action:"allZonesOff", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC"
            state "off", label:'Off', action:"allZonesOn", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff"
        }
        standardTile("allZones", "allZonesSwitch", width: 6, height: 4) {
            state "on", label:'On', action:"allZonesOff", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
            state "off", label:'Off', action:"allZonesOn", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'Turning On', action:"allZonesOff", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
            state "turningOff", label:'Turning Off', action:"allZonesOn", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
        }
        multiAttributeTile(name:"zone1", type: "lighting", width: 6, height: 4) {
            tileAttribute ("zone1Switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'Zone 1 On', action:"zone1Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'Zone 1 Off', action:"zone1On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'Zone 1 Turning On', action:"zone1Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'Zone 1 Turning Off', action:"zone1On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("zone1Color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setZone1Color"
            }
        }
        multiAttributeTile(name:"zone2", width: 6, height: 4) {
            tileAttribute ("zone2Switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'Zone 2 On', action:"zone2Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'Zone 2 Off', action:"zone2On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'Zone 2 Turning On', action:"zone2Off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'Zone 2 Turning Off', action:"zone2On", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("zone2Color", key: "COLOR_CONTROL") {
                attributeState "color", action:"setZone2Color"
            }
        }

        main "allZonesValue"
        details(["allZones", "zone1", "zone2", "refresh"])
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

def allZonesOn() {
    log.debug("All zones on")
    lightsOn()
    sendEvent(name:"zone1Switch", value:"on")
    sendEvent(name:"zone2Switch", value:"on")
    sendEvent(name:"allZonesSwitch", value:"on")
}

def allZonesOff() {
    log.debug("All zones off")
    lightsOff()
    sendEvent(name:"zone1Switch", value:"off")
    sendEvent(name:"zone2Switch", value:"off")
    sendEvent(name:"allZonesSwitch", value:"off")
}

def zone1On() {
    log.debug("Zone 1 on")
    lightsOn(1)
    sendEvent(name:"zone1Switch", value:"on")
    sendEvent(name:"allZonesSwitch", value:"on")
}

def zone1Off() {
    log.debug("Zone 1 off")
    lightsOff(1)
    sendEvent(name:"zone1Switch", value:"off")
    sendEvent(name:"allZonesSwitch", value:"off")
}

def setZone1Color(value) {
    log.debug("set zone 1 color to ${value}")
    setColor(1, value)
    sendEvent(name: "zone1Color", value: value.hex, data: value)
}

def zone2On() {
    lightsOn(2)
    log.debug("Zone 2 on")
    sendEvent(name:"zone2Switch", value:"on")
}

def zone2Off() {
    log.debug("Zone 2 off")
    lightsOff(2)
    sendEvent(name:"zone2Switch", value:"off")
}

def setZone2Color(value) {
    log.debug("set zone 2 color to ${value}")
    setColor(2, value)
    sendEvent(name: "zone2Color", value: value.hex, data: value)
}

//def toggleOffColorTiles() {
//    sendEvent(name: "zone1Color", value: device.zone1Color, displayed: false, isStateChange: true)
//    sendEvent(name: "zone2Color", value: device.zone2Color, displayed: false, isStateChange: true)
//}


// Commands to send to the wifi 104

def lightsOn(def zone='all') {
    log.info("Turning lights on")
    sendCommand(zone, 'on')
}

def lightsOff(def zone='all') {
    log.info("Turning lights off")
    sendCommand(zone, 'off')
}

def setColor(int zone, value) {
    log.info "setColor: ${value}"

    if (value.hex) {
        log.info "setting color with hex"
        def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
        sendCommand(zone, 'color', [
                red:c[0],
                green:c[1],
                blue:c[2],
                white:0,
                brightness:255,
        ])
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

private void sendCommand(def zone, def action, def queryParams=[:]) {
    updateDNI()
    log.info("Sending zone ${zone} command ${action} with params ${queryParams} to ${settings.host}")

    sendHubCommand(new physicalgraph.device.HubAction(
            method: "PUT",
            path: "/lights/zones/${zone}/${action}",
            headers: [
                    HOST: settings.host
            ],
            query: queryParams
    ))
}

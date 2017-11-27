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
import groovy.transform.Field

def version() {
    return "1.0\n© 2017 Brian Saville"
}

// The maximum number of zones supported
def MAX_ZONES = 12
// Number between 1 and MAX_ZONES (inclusive)
// This MUST match the value configured in the device, since settings are not available when creating/managing tiles
def NUM_ZONES = 2

@Field def red = 'ff0000'
@Field def green = '00ff00'
@Field def blue = '0000ff'
@Field def orange = 'ffff00'
@Field def purple = 'ff00ff'
@Field def COLORS = [
        'Fall':[orange, green],
        'Christmas': [red, green],
        'Halloween': ['ff8800', purple],
//        'Easter': TODO
//        'Valentines': TODO
        'JulyFourth': [red, blue],
        'Red':[red, red],
        'Green':[green, green],
        'Blue':[blue, blue],
]

preferences {
    input("mac", "string", title: "MAC Address (no colons)", required: true, displayDuringSetup: true)
    input("host", "string", title: "IP:Port", required: true, displayDuringSetup: true)
    input("zones", "number", range:"1..${MAX_ZONES}", title: "Number of Zones (1-${MAX_ZONES})", required: true, displayDuringSetup: true)
    input(title:"", description: "Version: ${version()}", type: "paragraph", element: "paragraph")
}

metadata {
    definition (name: "Wifi 104 Local", namespace: "bluesliverx", author: "Brian Saville") {
        capability "Color Control"
        capability "Switch"
        capability "Refresh"
        capability "Polling"

        attribute("allZonesSwitch", "enum", ["on", "off", "turningOff", "turningOn"])
        (1..MAX_ZONES).each { int zone ->
            attribute("zone${zone}Switch", "enum", ["on", "off", "turningOff", "turningOn"])
            attribute("zone${zone}Color", "string")
            attribute("zone${zone}Level", "number")
        }

        (1..MAX_ZONES).each { int zone ->
            command "zone${zone}On"
            command "zone${zone}Off"
            command "setZone${zone}Color"
        }

        // Color combinations
        COLORS.each { String name, def colors ->
            command "color${name}"
        }
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        standardTile("allZonesSwitchControl", "allZonesSwitch", width: 6, height: 4) {
            state "on", label:'On', action:"off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
            state "off", label:'Off', action:"on", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'Turning On', action:"on", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOn"
            state "turningOff", label:'Turning Off', action:"off", icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOff"
        }

        (1..MAX_ZONES).each { int zone ->
            standardTile("zone${zone}SwitchControl".toString(), "zone${zone}Switch".toString(), width: 3, height: 3, decoration: 'flat') {
                state "on", label: "Zone ${zone}", action: "zone${zone}Off", icon: "st.Seasonal Winter.seasonal-winter-011", backgroundColor: "#00A0DC", nextState: "turningOff"
                state "off", label: "Zone ${zone}", action: "zone${zone}On", icon: "st.Seasonal Winter.seasonal-winter-011", backgroundColor: "#ffffff", nextState: "turningOn"
                state "turningOn", label: "Zone ${zone}", action: "zone${zone}On", icon: "st.Seasonal Winter.seasonal-winter-011", backgroundColor: "#00A0DC", nextState: "turningOn"
                state "turningOff", label: "Zone ${zone}", action: "zone${zone}Off", icon: "st.Seasonal Winter.seasonal-winter-011", backgroundColor: "#ffffff", nextState: "turningOff"
            }
        }

        (1..MAX_ZONES).each { int zone ->
            controlTile("zone${zone}ColorSelector".toString(), "zone${zone}Color".toString(), "color", height: 3, width: 3, inactiveLabel: false) {
                state "color", action: "setZone${zone}Color"
            }
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        standardTile("colorsLabel", "null", decoration: "flat", width: 6, height: 2) {
            state "default", label:'Colors', defaultState: true
        }
        COLORS.each { String name, def colors ->
            standardTile("setColor${name}".toString(), "null", width: 2, height: 2, decoration: "flat") {
                state "empty", label: name.replaceAll('([a-z])([A-Z])', '$1 $2'), action: "color${name}", defaultState: true
            }
        }


        main "allZonesSwitchControl"

        // Build dynamic details list that only includes the number of zones it should
        def detailsItems = ["allZonesSwitchControl"]
        for (int zone=1; zone <= NUM_ZONES; zone+=2) {
            // Handle odd number of zones correctly
            detailsItems.add("zone${zone}SwitchControl")
            if ((zone + 1) <= NUM_ZONES) {
                detailsItems.add("zone${zone + 1}SwitchControl")
            }
            detailsItems.add("zone${zone}ColorSelector")
            if ((zone + 1) <= NUM_ZONES) {
                detailsItems.add("zone${zone + 1}ColorSelector")
            }
        }

        detailsItems.add("refresh")
        detailsItems.add("colorsLabel")

        COLORS.each { String name, def colors ->
            detailsItems.add("setColor${name}")
        }
        details(detailsItems)
    }
}

def parse(description) {
    log.info("Parse ${description}")
}

def refresh() {
    log.debug("Refreshing data from server")
    sendHubCommand(getCommand('all', 'status', [:], 'GET'))
}

def poll() {
    log.debug("Poll called")
    refresh()
}

def on() {
    log.debug("All zones on")
    return lightsOn()
}

def off() {
    log.debug("All zones off")
    return lightsOff()
}


def zone1On() {
    return lightsOn(1)
}
def zone1Off() {
    return lightsOff(1)
}
def setZone1Color(value) {
    return setColor(1, value)
}

def zone2On() {
    return lightsOn(2)
}
def zone2Off() {
    return lightsOff(2)
}
def setZone2Color(value) {
    return setColor(2, value)
}

def zone3On() {
    return lightsOn(3)
}
def zone3Off() {
    return lightsOff(3)
}
def setZone3Color(value) {
    return setColor(3, value)
}

def zone4On() {
    return lightsOn(4)
}
def zone4Off() {
    return lightsOff(4)
}
def setZone4Color(value) {
    return setColor(4, value)
}

def zone5On() {
    return lightsOn(5)
}
def zone5Off() {
    return lightsOff(5)
}
def setZone5Color(value) {
    return setColor(5, value)
}

def zone6On() {
    return lightsOn(6)
}
def zone6Off() {
    return lightsOff(6)
}
def setZone6Color(value) {
    return setColor(6, value)
}

def zone7On() {
    return lightsOn(7)
}
def zone7Off() {
    return lightsOff(7)
}
def setZone7Color(value) {
    return setColor(7, value)
}

def zone8On() {
    return lightsOn(8)
}
def zone8Off() {
    return lightsOff(8)
}
def setZone8Color(value) {
    return setColor(8, value)
}

def zone9On() {
    return lightsOn(9)
}
def zone9Off() {
    return lightsOff(9)
}
def setZone9Color(value) {
    return setColor(9, value)
}

def zone10On() {
    return lightsOn(10)
}
def zone10Off() {
    return lightsOff(10)
}
def setZone10Color(value) {
    return setColor(10, value)
}

def zone11On() {
    return lightsOn(11)
}
def zone11Off() {
    return lightsOff(11)
}
def setZone11Color(value) {
    return setColor(11, value)
}

def zone12On() {
    return lightsOn(12)
}
def zone12Off() {
    return lightsOff(12)
}
def setZone12Color(value) {
    return setColor(12, value)
}


// Color combinations

/**
 * Set colors for zones, in the form of 0011ff, where 00 is the red color, 11 is the green color,
 * and ff is the red color.
 * @param colors A color string for each zone
 */
def setColorsForZones(String name) {
    def colors = COLORS[name]
    log.info("Setting colors to ${name}, zones: ${colors}")
    return (1..colors.size()).collect { int zone ->
        return sendHubCommand(setColor(zone, [hex:"#${colors[zone-1]}"]))
    }
}
def colorRed() {
    return setColorsForZones('Red')
}
def colorGreen() {
    return setColorsForZones('Green')
}
def colorBlue() {
    return setColorsForZones('Blue')
}
def colorFall() {
    return setColorsForZones('Fall')
}
def colorChristmas() {
    return setColorsForZones('Christmas')
}
def colorHalloween() {
    return setColorsForZones('Halloween')
}
def colorEaster() {
    return setColorsForZones('Easter')
}
def colorValentines() {
    return setColorsForZones('Valentines')
}
def colorJulyFourth() {
    return setColorsForZones('JulyFourth')
}


// Commands to send to the wifi 104

def lightsOn(def zone='all') {
    log.info("Turning lights on for zone ${zone}")
    return getCommand(zone, 'on')
}

def lightsOff(def zone='all') {
    log.info("Turning lights off for zone ${zone}")
    return getCommand(zone, 'off')
}

def setColor(int zone, value) {
    if (value.hex) {
        log.info "Setting color for zone ${zone} to ${value}"
        sendEvent(name: "zone${zone}Color".toString(), value: value.hex, data: value)
        def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
        return getCommand(zone, 'color', [
                red:c[0],
                green:c[1],
                blue:c[2],
                white:0,
                brightness:255,
        ])
    } else {
        log.error "Setting color with hue and saturation yet is not yet implemented"
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

private def updateDNI() {
    if (!state.dni || state.dni != device.deviceNetworkId || (settings.mac && settings.mac != device.deviceNetworkId)) {
        log.debug("Setting DNI to ${settings.mac}")
        device.setDeviceNetworkId(settings.mac)
        state.dni = device.deviceNetworkId
    }
}

def callbackMethod(physicalgraph.device.HubResponse hubResponse) {
    def json = hubResponse.json
    log.debug("Received response from server: ${json}")
    if (!json) {
        log.warn("Response received from server was invalid: ${hubResponse}")
    } else if (json.message) {
        log.info("Received message from server: ${json.message}")
        refresh();
    } else if (json.zones) {
        log.info("Received status information from the server: ${json.zones} (${settings.zones} max zones)")
        def zonesOn = false
        for (int zone=1; zone <= settings.zones; zone++) {
            // The keys end up as strings, so convert to string for them
            def status = json.zones[zone.toString()] ? 'on' : 'off'
            log.debug("Setting zone${zone}Switch to ${status}")
            sendEvent(name:"zone${zone}Switch".toString(), value:status)
            if (status=='on') {
                zonesOn = true
            }
        }
        def status = zonesOn ? 'on' : 'off'
        log.debug("Setting allZonesSwitch to ${status}")
        sendEvent(name:"allZonesSwitch", value:status)
    } else {
        log.warn("Unrecognized response from server, cannot handle: ${hubResponse}")
    }
}

private physicalgraph.device.HubAction getCommand(def zone, def action, def queryParams=[:], def method="PUT") {
    updateDNI()
    def url = "/lights/zones/${zone}/${action}"
    log.info("Sending ${method} request to ${settings.host}${url} with params ${queryParams} (${device.deviceNetworkId})")

    return new physicalgraph.device.HubAction(device.deviceNetworkId, [callback:'callbackMethod'],
            method: method,
            path: url,
            headers: [
                    HOST: settings.host
            ],
            query: queryParams,
    )
}

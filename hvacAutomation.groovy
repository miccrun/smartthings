definition(
    name: "HVAC Automation",
    namespace: "miccrun",
    author: "Michael Chang",
    description: "HVAC Automation",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences() {
    section("Choose thermostat... ") {
        input "thermostat", "capability.thermostat", required: true
    }
    section("Heat setting..." ) {
        input "heatingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Heat setting at sleep..." ) {
        input "sleepHeatingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Heat setting at away..." ) {
        input "awayHeatingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Air conditioning setting...") {
        input "coolingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Air conditioning setting at sleep...") {
        input "sleepCoolingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Air conditioning setting at away...") {
        input "awayCoolingSetpoint", "decimal", title: "Degrees", required: true
    }
    section("Choose temperature sensor to use instead of the thermostat's... ") {
        input "sensor", "capability.temperatureMeasurement", title: "Temperature Sensors", required: true
    }
    section("Choose outside temperature sensor... ") {
        input "outside", "capability.temperatureMeasurement", title: "Outside Temperature Sensors", required: true
    }
    section("Heating threshold..." ) {
        input "heatingThreshold", "decimal", title: "Degrees", required: true
    }
    section("Cooling threshold..." ) {
        input "coolingThreshold", "decimal", title: "Degrees", required: true
    }
}

def installed() {
    subscribeEvents()
}

def updated() {
    unsubscribe()
    unschedule()
    subscribeEvents()
}

def subscribeEvents() {
    subscribe(location, "mode", changedLocationMode)
    subscribe(sensor, "temperature", temperatureHandler)
    subscribe(outside, "temperature", temperatureHandler)
    subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatMode", temperatureHandler)
    schedule("19 * * * * ?", temperatureHandler)
}

def changedLocationMode(evt) {
    evaluate()
}

def temperatureHandler(evt) {
    evaluate()
}

private getHeatSetpoint() {
    def now = new Date()
    def hour = now.format("HH", location.timeZone).toInteger()
    def minute = now.format("mm", location.timeZone).toInteger()
    def minutes = hour * 60 + minute
    def day = now.format("EEE", location.timeZone)
    def morningStartTime
    def morningEndTime

    if (thermostat.id == "e60b60ba-846e-4706-a0d9-c4493199c1ad") {

        if (day == "Sat" || day == "Sun") {
            morningStartTime = 520
                morningEndTime = 570
        } else {
            morningStartTime = 490
                morningEndTime = 540
        }

        if (location.mode == "Home" ) {

            if (minutes <= morningEndTime || minutes >= 1380) {
                return heatingSetpoint
            } else {
                return awayHeatingSetpoint
            }
        } else if (location.mode == "Night" ) {
            if (minutes >= morningStartTime && minutes < 720) {
                return heatingSetpoint
            } else {
                return sleepHeatingSetpoint
            }
        } else if (location.mode == "Away" ) {
            return awayHeatingSetpoint
        }
    } else if (thermostat.id == "22ebc975-065d-412a-92b9-913a6695f01c") {

        if (day == "Sat" || day == "Sun") {
            morningStartTime = 550
        } else {
            morningStartTime = 510
        }

        if (location.mode == "Home" ) {
            if (minutes >= morningStartTime || minutes <= 1380) {
                return heatingSetpoint
            } else {
                return awayHeatingSetpoint
            }
        } else if (location.mode == "Night" ) {
            if (minutes >= morningStartTime && minutes < 720) {
                return heatingSetpoint
            } else {
                return sleepHeatingSetpoint
            }
        } else if (location.mode == "Away" ) {
            return awayHeatingSetpoint
        }
    }
}

private getCoolSetpoint() {
    def now = new Date()
    def hour = now.format("HH", location.timeZone).toInteger()
    def minute = now.format("mm", location.timeZone).toInteger()
    def minutes = hour * 60 + minute
    def day = now.format("EEE", location.timeZone)

    if (day == "Sat" || day == "Sun") {
        if ((minutes >= 520 && minutes < 570) || (minutes >= 1380 || minutes <= 40)) {
            return heatingSetpoint
        } else {
            return 65.0
        }

    } else {
        if ((minutes >= 490 && minutes < 540) || (minutes >= 1380 || minutes <= 10)) {
            return heatingSetpoint
        } else {
            return 65.0
        }
    }
}

private evaluate() {
    def threshold = 1.0
    def heatSetpoint = getHeatSetpoint()
    def coolSetpoint = 80

    log.debug("Evaluating: home mode: $location.mode, thermostat mode: $thermostat.currentThermostatMode, thermostat temp: $thermostat.currentTemperature, " +
        "thermostat heat setpoint: $thermostat.currentHeatingSetpoint, thermostat cool setpoint: $thermostat.currentCoolingSetpoint, " +
        "remote sensor temp: $sensor.currentTemperature, desire heat setpoint: $heatSetpoint, desire cool setpoint: $coolSetpoint, " +
        "outside temperature: $outside.currentTemperature")

    if (outside.currentTemperature < heatingThreshold) {
        // change to heating mode
        if (thermostat.currentThermostatMode != "heat") {
            thermostat.heat()
            log.debug("set to heat mode")
        }
    } else if (outside.currentTemperature > coolingThreshold) {
        // change to cooling mode
        if (thermostat.currentThermostatMode != "cool") {
            thermostat.cool()
            log.debug("set to cool mode")
        }
    } else {
        // good weather, turn off
        if (thermostat.currentThermostatMode != "off") {
            thermostat.off()
            log.debug("turn off")
        }
    }

    if (thermostat.currentThermostatMode in ["cool","auto"]) {
        if (sensor.currentTemperature - coolSetpoint >= threshold) {
            if (thermostat.currentCoolingSetpoint != thermostat.currentTemperature - 2) {
                thermostat.setCoolingSetpoint(thermostat.currentTemperature - 2)
                log.debug "cooling to ${thermostat.currentTemperature - 2}"
            }
        }
        else if (coolSetpoint - sensor.currentTemperature >= threshold) {
            if (thermostat.currentCoolingSetpoint != thermostat.currentTemperature + 2) {
                thermostat.setCoolingSetpoint(thermostat.currentTemperature + 2)
                log.debug "idle to ${thermostat.currentTemperature + 2}"
            }
        }
    }
    if (thermostat.currentThermostatMode in ["heat","emergency heat","auto"]) {
        if (heatSetpoint - sensor.currentTemperature >= threshold) {
            if (thermostat.currentHeatingSetpoint != thermostat.currentTemperature + 2) {
                thermostat.setHeatingSetpoint(thermostat.currentTemperature + 2)
                log.debug "heating to ${thermostat.currentTemperature + 2}"
            }
        }
        else if (sensor.currentTemperature - heatSetpoint >= threshold) {
            if (thermostat.currentHeatingSetpoint != thermostat.currentTemperature - 2) {
                thermostat.setHeatingSetpoint(thermostat.currentTemperature - 2)
                log.debug "idle to ${thermostat.currentTemperature - 2}"
            }
        }
    }
}

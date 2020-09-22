/**
 * Strobe a switch, triggered by another switch.
 *
 * NB This code uses integers rather than booleans for the control variables,
 * as the boolean weren't persisting correctly!?
 */

// -------------------------------- Configuration --------------------------------

definition(
	name: "Strobe",
	namespace: "github.com/sebrichards",
	author: "Seb Richards",
	description: "Strobe a switch, triggered by another switch",
	category: "Utilities",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", uninstall: true, install: true) {
		section {

			input "thisName", "text", title: "Name", submitOnChange: true, required: true
			if(thisName) app.updateLabel("$thisName")

			input "trigger", "capability.switch", title: "Trigger switch", submitOnChange: true, required: true
			input "outputs", "capability.switch", title: "Output switches", multiple: true, submitOnChange: true, required: true

			input "onSecs", "number", title: "On for how many seconds?", width: 6, required: true
			input "offSecs", "number", title: "Off for how many seconds?", width: 6, required: true

			input "debugLogging", "bool", title: "Enable debug logging", width: 6
		}
	}
}

// -------------------------------- Lifecycle --------------------------------

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	subscribe(trigger, "switch.on", handleTriggerOn)
	subscribe(trigger, "switch.off", handleTriggerOff)

	// Init state
	state.running = 0
	state.terminate = 0
	state.on = 0
}

// -------------------------------- Logic --------------------------------

def handleTriggerOn(evt) {

	debug("handleTriggerOn()")

	// Not running
	if (state.running == 0) {
		info("Starting strobe")
		state.running = 1
		loop()

	// Running, but awaiting termination
	} else if (state.terminate == 1) {
		info("Cancelled termination request")
		state.terminate = 0
	}
}

def handleTriggerOff(evt) {

	debug("handleTriggerOff()")

	if (state.running == 1) {
		info("Requested termination...")
		state.terminate = 1
	}
}

def loop() {

	debug("loop()")

	if (state.terminate == 1) {

		info("Terminated strobe")

		// If on, turn off
		if (state.on == 1) {
			outputs.off()
			state.on = 0
		}

		// Reset state
		state.running = 0
		state.terminate = 0

	} else if (state.on == 1) {

		info("Turning switch off")

		// Turn off
		outputs.off()
		state.on = 0

		// Schedule loop
		runIn(offSecs, loop)

	} else {

		info("Turning switch on")

		// Turn on
		outputs.on()
		state.on = 1

		// Schedule loop
		runIn(onSecs, loop)
	}
}

// -------------------------------- Helpers --------------------------------

def info(text) {
	log.info(text)
}

def debug(text) {
	if (debugLogging) log.debug(text)
}
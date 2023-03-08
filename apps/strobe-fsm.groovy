/**
 * Strobe a switch, triggered by another switch.
 *
 * Modelled (approximately) as an FSM, with the following states:
 *   - OFF
 *   - RUNNING_OFF
 *   - RUNNING_ON
 *   - RUNNING_OFF_TERMINATING
 *   - RUNNING_ON_TERMINATING
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
	setState("OFF")
}

// -------------------------------- Logic --------------------------------

def handleTriggerOn(evt) {

	debug("handleTriggerOn()")

	def current = getState()

	switch(current) {

		case "OFF":
			info("Starting strobe")
			setState("RUNNING_OFF")
			runIn(60, escapeHatch)
			loop()
			break

		case "RUNNING_ON":
		case "RUNNING_OFF":
			debug("Nothing to do (${current})")
			break

		case "RUNNING_ON_TERMINATING":
			info("Cancelled termination request")
			setState("RUNNING_ON")
			break

		case "RUNNING_OFF_TERMINATING":
			info("Cancelled termination request")
			setState("RUNNING_OFF")
			break

		default:
			debug("Unhandled state: ${current}")
			break;

	}
}

def handleTriggerOff(evt) {

	debug("handleTriggerOff()")

	def current = getState()

	switch(current) {

		case "RUNNING_ON":
			info("Requested termination...")
			setState("RUNNING_ON_TERMINATING")
			break

		case "RUNNING_OFF":
			info("Requested termination...")
			setState("RUNNING_OFF_TERMINATING")
			break

		default:
			debug("Nothing to do (${current})")
			break;

	}
}

def loop() {

	debug("loop()")

	def current = getState()

	switch(current) {

		case "RUNNING_ON":

			info("Turning switch off")
			outputs.off()

			setState("RUNNING_OFF")
			runIn(offSecs, loop)

			break

		case "RUNNING_OFF":

			info("Turning switch on")
			outputs.on()

			setState("RUNNING_ON")
			runIn(onSecs, loop)

			break

		case "RUNNING_ON_TERMINATING":

			info("Turning switch off")
			outputs.off()

			// NB don't break, continue into next case

		case "RUNNING_OFF_TERMINATING":

			unschedule()

			info("Terminated strobe")
			setState("OFF")

			break

		default:
			debug("Unhandled state: ${current}")
			break;
	}
}

def escapeHatch() {

	unschedule()

	log.error("ESCAPE HATCH — potential deadlock / trigger bug. Forcefully terminating strobe exectuion!")

	outputs.off()
	setState("OFF")
}

// -------------------------------- Helpers --------------------------------

def getState() {
	return state.state
}

def setState(name) {
	state.state = name
	debug("STATE --> ${name}")
}

def info(text) {
	log.info(text)
}

def debug(text) {
	if (debugLogging) log.debug(text)
}
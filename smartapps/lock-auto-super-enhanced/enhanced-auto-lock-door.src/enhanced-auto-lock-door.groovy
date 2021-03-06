definition(
    name: "Enhanced Auto Lock Door",
    namespace: "Lock Auto Super Enhanced",
    author: "Arnaud",
    description: "Automatically locks a specific door after X minutes when closed  and unlocks it when open after X seconds.",
    category: "Safety & Security",
    iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg"
)

preferences{
    section("Select the door lock:") {
        input "lock1", "capability.lock", required: true
    }
    section("Select the door contact sensor:") {
    	input "contact", "capability.contactSensor", required: true
    }   
    section("Automatically lock the door when closed...") {
        input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section("Automatically unlock the door when open...") {
        input "secondsLater", "number", title: "Delay (in seconds):", required: true
    }
    section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "phoneNumber", "phone", title: "Enter phone number to send text notification.", required: false
	}
}

def installed(){
    initialize()
}

def updated(){
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(lock1, "unlock", doorHandler, [filterEvents: false])  
    subscribe(contact, "contact.open", doorHandler)
	subscribe(contact, "contact.closed", doorHandler)
}

def lockDoor(){
    log.debug "Locking the door."
    lock1.lock()
    log.debug ( "Sending Push Notification..." ) 
    if ( sendPushMessage != "No" ) sendPush( "${lock1} locked after ${contact} was closed for ${minutesLater} minutes!" )
    log.debug("Sending text message...")
    if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} locked after ${contact} was closed for ${minutesLater} minutes!" )
}

def unlockDoor(){
    log.debug "Unlocking the door."
    lock1.unlock()
    log.debug ( "Sending Push Notification..." ) 
    if ( sendPushMessage != "No" ) sendPush( "${lock1} unlocked after ${contact} was opened for ${secondsLater} seconds!" )
    log.debug("Sending text message...")
    if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened for ${secondsLater} seconds!" )
}

def doorHandler(evt){
    if ((contact.latestValue("contact") == "open") && (evt.value == "locked")) { // If the door is open and a person locks the door then...  
        def delay = (secondsLater) // runIn uses seconds
        runIn( delay, unlockDoor )   // ...schedule (in minutes) to unlock...  We don't want the door to be closed while the lock is engaged. 
    }
    else if ((contact.latestValue("contact") == "open") && (evt.value == "unlocked")) { // If the door is open and a person unlocks it then...
        unschedule( unlockDoor ) // ...we don't need to unlock it later.
	}
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }   
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
        def delay = (minutesLater * 60) // runIn uses seconds
        runIn( delay, lockDoor ) // ...schedule (in minutes) to lock.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "open")) { // If a person opens an unlocked door...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
        def delay = (minutesLater * 60) // runIn uses seconds
        runIn( delay, lockDoor ) // ...schedule (in minutes) to lock.
	}
    else { //Opening or Closing door when locked (in case you have a handle lock)
    	log.debug "Unlocking the door."
		lock1.unlock()
        log.debug ( "Sending Push Notification..." ) 
    	if ( sendPushMessage != "No" ) sendPush( "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!" )
        log.debug("Sending text message...")
    	if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!" )
		}
}
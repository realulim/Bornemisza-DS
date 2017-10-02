// Custom Events
var AUTH_SUCCESS = "Authentication successful";
var AUTH_FAILED = "Authentication failed";
var AUTH_ERROR = "Error while authenticating";
var LOADING_IN_PROGRESS = "Loading Data in Progress";
var LOADING_DONE = "Loading Data completed";
var LOADING_ERROR = "Error while loading Data";
var JUST_MARRIED = "User just logged in successfully";
var ENABLE_SPINNER = "Enable Loading Indicator";
var DISABLE_SPINNER = "Disable Loading Indicator";
var START_CONTINUOUS = "Start Continuous Mode";
var STOP_CONTINUOUS = "Stop Continuous Mode";

// Functions
function setBackground(color) {
    document.documentElement.setAttribute("style", "background-color: " + color)
}

function shakeElement(elem) {
    elem.classList.add("shake");
    window.setTimeout(function () {
        elem.classList.remove("shake");
    }, 1000);
}

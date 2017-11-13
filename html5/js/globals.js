// Custom Events
var AUTH_SUCCESS = "Authentication successful";
var AUTH_FAILED = "Authentication failed";
var AUTH_ERROR = "Error while authenticating";
var REGISTRATION_FAILED = "Registration failed";
var REGISTRATION_ERROR = "Error while registering";
var SHOW_BARON_MESSAGE = "Show message from the Baron";
var LOGOUT_REQUESTED = "Logout requested by User";
var LOADING_IN_PROGRESS = "Loading Data in Progress";
var LOADING_DONE = "Loading Data completed";
var LOADING_ERROR = "Error while loading Data";
var JUST_MARRIED = "User just logged in successfully";
var TOGGLE_SPINNER = "Toggle Loading Indicator";
var ENABLE_SPINNER = "Enable Loading Indicator";
var DISABLE_SPINNER = "Disable Loading Indicator";
var START_SINGLE = "Start Single Request Mode";
var STOP_SINGLE = "Stop Single Request Mode";
var START_LOOP = "Start Loop Requests Mode";
var STOP_LOOP = "Stop Loop Requests Mode";
var START_BATCH = "Start Batch Requests Mode";
var STOP_BATCH = "Stop Batch Requests Mode";
var UPDATE_APPSERVERS_CHART = "Update Application Servers Chart";
var UPDATE_DBSERVERS_CHART = "Update Database Servers Chart";

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

function inputLimiter(e, allow) {
    var AllowableCharacters = '';

    if (allow == 'Letters') { AllowableCharacters = ' ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'; }
    if (allow == 'Numbers') { AllowableCharacters = '1234567890'; }
    if (allow == 'NameCharacters') { AllowableCharacters = ' ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890@-._/"'; }

    var k = document.all ? parseInt(e.keyCode) : parseInt(e.which);
    if (k != 13 && k != 8 && k != 0) {
        if ((e.ctrlKey == false) && (e.altKey == false)) {
            return (AllowableCharacters.indexOf(String.fromCharCode(k)) != -1);
        } else {
            return true;
        }
    } else {
        return true;
    }
}

// Custom Events
var AUTH_SUCCESS = "Authentication successful";
var AUTH_FAILED = "Authentication failed";
var AUTH_ERROR = "Error while authenticating";
var LOADING_IN_PROGRESS = "Loading Data in Progress";
var LOADING_DONE = "Loading Data completed";
var JUST_MARRIED = "User just logged in successfully"

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

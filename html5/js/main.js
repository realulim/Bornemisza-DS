// Custom Events
var AUTH_SUCCESS = "Authentication successful"
var AUTH_FAILED = "Authentication failed"
var ERROR = "Error"

// Functions
function flipDialog() {
    document.getElementById('card').classList.toggle("flipped");
}
function displayMessage(show, elemId) {
    var elem = document.getElementById(elemId)
    if (show) elem.classList.remove("hidden")
    else elem.classList.add("hidden")
}
function shakeElement(elemId) {
    document.getElementById(elemId).classList.add("shake");
    window.setTimeout(function () {
        document.getElementById(elemId).classList.remove("shake");
    }, 1000);
}
function createAuthorizationHeader(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return "Basic " + hash;
}

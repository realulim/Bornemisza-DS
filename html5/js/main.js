// Custom Events
var AUTH_SUCCESS = "Authentication successful"
var AUTH_FAILED = "Authentication failed"
var AUTH_ERROR = "Error while authenticating"

// Functions
function flipDialog() {
    document.getElementById('card').classList.toggle("flipped");
}
function displayMessage(elemId, fadeInOut) {
    var elem = document.getElementById(elemId)
    hideAllMessages()
    elem.classList.remove("hidden")
    if (fadeInOut) elem.classList.add("fade-in-out")
}
function hideAllMessages() {
    var elems = document.getElementsByClassName("msg");
    for (let i = 0; i < elems.length; i++) {
        elems[i].classList.add("hidden")
    }
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

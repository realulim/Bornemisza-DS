// Custom Events
var AUTH_SUCCESS = "Authentication successful"
var AUTH_FAILED = "Authentication failed"
var AUTH_ERROR = "Error while authenticating"

// Functions
function flipDialog() {
    document.getElementById('card').classList.toggle("flipped");
}
function shakeElement(elemId) {
    var elem = document.getElementById(elemId)
    elem.classList.add("shake");
    window.setTimeout(function () {
        elem.classList.remove("shake");
    }, 1000);
}
function createAuthorizationHeader(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return "Basic " + hash;
}

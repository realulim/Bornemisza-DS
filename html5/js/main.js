// Custom Events
var AUTH_SUCCESS = "Authentication successful"
var AUTH_FAILED = "Authentication failed"
var ERROR = "Error"

// Functions
function flipDialog() {
    document.getElementById('card').classList.toggle("flipped");
}
function createAuthorizationHeader(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return "Basic " + hash;
}

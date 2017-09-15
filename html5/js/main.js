function flipDialog() {
    document.getElementById('card').classList.toggle("flipped");
}
function createAuthorizationHeader(user, password) {
    var tok = user + ':' + password;
    var hash = btoa(tok);
    return "Basic " + hash;
}

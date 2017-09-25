/*
 |--------------------------------------------------------------------------
 | Browser-sync config file
 |--------------------------------------------------------------------------
 |
 | For up-to-date information about the options:
 |   http://www.browsersync.io/docs/options/
 |
 | There are more options than you see here, these are just the ones that are
 | set internally. See the website for more info.
 |
 |
 */
module.exports = {
    "ui": false,
    "files": ["bin/tags.js", "css/*.css", "index.html"],
    "server": false,
    "logPrefix": "browser-sync",
    "rewriteRules": [],
    "open": false,
    "browser": "firefox",
    "hostnameSuffix": false,
    "plugins": [],
    "startPath": "/index.html",
    middleware: function (req, res, next) {
        if (req.url === '/main.html') {
            req.url = '/index.html';
        }
        return next();
    }
};
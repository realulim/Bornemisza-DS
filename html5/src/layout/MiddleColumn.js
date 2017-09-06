var m = require("mithril")

var Front = require("../views/Login")
var Back = require("../views/Register")

module.exports = {
    view: function () {
        return m(".cards",
            m("[id='card']",
                [
                    m("figure.front", m(Front)
                    ),
                    m("figure.back", m(Back)
                    )
                ]
            )
        )
    }
}

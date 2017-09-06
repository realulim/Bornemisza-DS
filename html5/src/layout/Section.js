var m = require("mithril")

var MiddleColumn = require("./MiddleColumn")

module.exports = {
    view: function () {
        return m("section.section",
            m(".columns",
                [
                    m(".column.is-3", ""
                    ),
                    m(".column.is-6", m(MiddleColumn)
                    ),
                    m(".column.is-3", ""
                    )
                ]
            )
        )
    }
}

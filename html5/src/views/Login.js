var m = require("mithril")

module.exports = {
    view: function () {
        return m(".login",
            [
                m("h1",
                    "Member Authentication"
                ),
                m("form[action=''][method='post']",
                    [
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Username or Email'][required=''][type='text'][value='']"),
                                m("span.icon.is-left",
                                    m("i.fa.fa-user.fa-fw")
                                )
                            ]
                        ),
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Password'][required=''][type='password'][value='']"),
                                m("span.icon.is-left",
                                    m("i.fa.fa-lock.fa-fw")
                                )
                            ]
                        ),
                        m("p.field.is-grouped",
                        ),
                        m("p.is-justified-left-and-right",
                            [
                                m("span",
                                    [
                                        "Not a member yet? ",
                                        m("a[href='javascript:flipDialog();']",
                                            "Join us!"
                                        )
                                    ]
                                ),
                                m("span",
                                    m("button[type='submit']",
                                        "Sign in"
                                    )
                                )
                            ]
                        ),
                        m("p")
                    ]
                )
            ]
        )
    }
}

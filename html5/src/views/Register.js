var m = require("mithril")

module.exports = {
    view: function () {
        return m(".login",
            [
                m("h1",
                    "Member Registration"
                ),
                m("form[action=''][method='post']",
                    [
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Your Username'][required=''][type='text'][value='']"),
                                m("span.icon.is-left",
                                    m("i.fa.fa-user.fa-fw")
                                )
                            ]
                        ),
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Your Email'][required=''][type='email'][value='']"),
                                m("span.icon.is-left",
                                    m("i.fa.fa-envelope.fa-fw")
                                )
                            ]
                        ),
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Your Password'][required=''][type='password'][value='']"),
                                m("span.icon.is-left",
                                    m("i.fa.fa-lock.fa-fw")
                                )
                            ]
                        ),
                        m("p.field.control.has-icons-left",
                            [
                                m("input[placeholder='Please confirm your Password'][required=''][type='password'][value='']"),
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
                                        "Already a member? ",
                                        m("a[href='javascript:flipDialog();']",
                                            "Sign in now!"
                                        )
                                    ]
                                ),
                                m("span",
                                    m("button[type='submit']",
                                        "Register"
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

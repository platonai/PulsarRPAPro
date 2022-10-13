package ai.platon.exotic.examples.sites.topEc.chinese.login.taobao

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.event.impl.LoginHandler
import ai.platon.pulsar.session.PulsarSession

class TaobaoLoginHandler(
    username: String,
    password: String,
    loginUrl: String = "https://login.taobao.com",
    activateSelector: String = ".password-login-tab-item",
    usernameSelector: String = "input#fm-login-id",
    passwordSelector: String = "input#fm-login-password",
    submitSelector: String = "button[type=submit]",
    warnUpUrl: String? = null,
) : LoginHandler(
    loginUrl,
    usernameSelector, username, passwordSelector, password,
    submitSelector, warnUpUrl, activateSelector
)

fun main() {
    val portalUrl = "https://s.taobao.com/search?spm=a21bo.jianhua.201867-main.24.5af911d9wFOWsc&q=收纳"
    val args = "-i 1s -ii 5m -ol a[href~=detail] -ignoreFailure"

    // login parameters
    val username = System.getenv("PULSAR_TAOBAO_USERNAME") ?: "MustFallUsername"
    val password = System.getenv("PULSAR_TAOBAO_PASSWORD") ?: "MustFallPassword"

    val session: PulsarSession = PulsarContexts.createSession()
    val options = session.options(args)
    val loginHandler = TaobaoLoginHandler(username, password, warnUpUrl = portalUrl)
    options.event.browseEvent.onBrowserLaunched.addLast(loginHandler)

    session.loadOutPages(portalUrl, options)

    PulsarContexts.await()
}

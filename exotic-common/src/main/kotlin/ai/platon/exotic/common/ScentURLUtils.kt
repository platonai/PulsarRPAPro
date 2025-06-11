package ai.platon.exotic.common

import java.net.URI
import java.net.URISyntaxException

object ScentURLUtils {

    /**
     * Normalizes a URL by removing redundant slashes and normalizing the URI.
     * @param url The URL to normalize.
     * @return The normalized URL as a string.
     * */
    fun normalizeUrl(url: String): String {
        val uri = URI(url).normalize()

        // 直接判断路径是否为null或空，简化逻辑
        val normalizedPath = uri.path?.replace(Regex("/+"), "/") ?: "/"

        return URI(
            uri.scheme,
            uri.userInfo,
            uri.host,
            uri.port,
            normalizedPath,
            uri.query,
            uri.fragment
        ).toString()
    }

    /**
     * Builds a server URL from the given hostname, port, context path, and path.
     * @param hostname The hostname of the server.
     * @param port The port number of the server.
     * @param contextPath The context path of the server.
     * @param path The specific path to append to the context path.
     * @return The constructed server URL as a string.
     * */
    @Throws(URISyntaxException::class)
    fun buildServerUrl(hostname: String, port: Int, contextPath: String, path: String = ""): String {
        val combinedPath = listOf(contextPath, path)
            .filter { it.isNotBlank() }
            .joinToString("/")
            .replace(Regex("/+"), "/")
            .let { if (!it.startsWith("/")) "/$it" else it }
        val url = "http://$hostname:$port$combinedPath"
        return normalizeUrl(url)
    }
}

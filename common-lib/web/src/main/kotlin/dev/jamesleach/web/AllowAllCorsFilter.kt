package dev.jamesleach.web

import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletResponse

/**
 * Add headers to allow all CORS.
 */
@Component
class AllowAllCorsFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        val response = res as HttpServletResponse
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080")
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
        response.setHeader("Access-Control-Max-Age", "3600")
        response.setHeader(
            "Access-Control-Allow-Headers",
            "Content-Type, Accept, X-Requested-With, remember-me".lowercase()
        )
        chain.doFilter(req, res)
    }

    override fun init(filterConfig: FilterConfig) {}
    override fun destroy() {}
}
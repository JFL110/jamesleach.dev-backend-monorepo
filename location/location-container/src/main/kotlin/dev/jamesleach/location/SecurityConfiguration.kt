package dev.jamesleach.location

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.web.servlet.invoke

@EnableWebSecurity
internal class SecurityConfiguration : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity?) {
        http {
            cors { }
            csrf { disable() }
            httpBasic { disable() }
            authorizeRequests {
                authorize("/ping", permitAll)
                authorize("/error", permitAll)
            }
        }
    }
}
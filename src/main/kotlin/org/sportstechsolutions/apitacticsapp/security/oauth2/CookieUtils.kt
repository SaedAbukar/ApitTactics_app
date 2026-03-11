package org.sportstechsolutions.apitacticsapp.security.oauth2

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.SerializationUtils
import java.util.*

object CookieUtils {
    fun getCookie(request: HttpServletRequest, name: String): Cookie? {
        return request.cookies?.firstOrNull { it.name == name }
    }

    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = Cookie(name, value)
        cookie.path = "/"
        cookie.isHttpOnly = true
        cookie.maxAge = maxAge
        response.addCookie(cookie)
    }

    fun deleteCookie(request: HttpServletRequest, response: HttpServletResponse, name: String) {
        request.cookies?.forEach { cookie ->
            if (cookie.name == name) {
                cookie.value = ""
                cookie.path = "/"
                cookie.maxAge = 0
                response.addCookie(cookie)
            }
        }
    }

    fun serialize(obj: Any): String = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(obj))

    fun <T> deserialize(cookie: Cookie, cls: Class<T>): T =
        cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.value)))
}
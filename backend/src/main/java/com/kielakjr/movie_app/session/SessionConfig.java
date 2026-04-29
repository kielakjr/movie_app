package com.kielakjr.movie_app.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${server.servlet.session.cookie.name:MOVIE_APP_SESSION}") String name,
            @Value("${server.servlet.session.cookie.http-only:true}") boolean httpOnly,
            @Value("${server.servlet.session.cookie.same-site:Lax}") String sameSite,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure
    ) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(name);
        serializer.setUseHttpOnlyCookie(httpOnly);
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        serializer.setCookiePath("/");
        return serializer;
    }
}

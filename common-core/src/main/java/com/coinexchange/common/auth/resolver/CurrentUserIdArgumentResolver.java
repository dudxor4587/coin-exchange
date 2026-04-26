package com.coinexchange.common.auth.resolver;

import com.coinexchange.common.auth.JwtTokenProvider;
import com.coinexchange.common.auth.annotation.CurrentUserId;
import com.coinexchange.common.auth.exception.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.coinexchange.common.auth.exception.AuthExceptionType.INVALID_TOKEN;
import static com.coinexchange.common.auth.exception.AuthExceptionType.TOKEN_NOT_FOUND;

@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
                parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Long resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null || request.getCookies() == null) {
            throw new AuthException(TOKEN_NOT_FOUND);
        }

        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                String token = cookie.getValue();
                return jwtTokenProvider.getIdFromToken(token);
            }
        }

        throw new AuthException(INVALID_TOKEN);
    }
}

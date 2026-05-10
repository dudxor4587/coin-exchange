package com.coinexchange.common.auth.resolver;

import com.coinexchange.common.auth.annotation.CurrentUserId;
import com.coinexchange.common.auth.exception.AuthException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.coinexchange.common.auth.exception.AuthExceptionType.INVALID_TOKEN;
import static com.coinexchange.common.auth.exception.AuthExceptionType.TOKEN_NOT_FOUND;

public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException(TOKEN_NOT_FOUND);
        }
        if (auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new AuthException(INVALID_TOKEN);
    }
}

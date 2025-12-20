package me.boonyarit.finance.resolver;

import me.boonyarit.finance.annotation.CurrentUser;
import me.boonyarit.finance.entity.UserEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUser.class) != null &&
            UserEntity.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public @Nullable Object resolveArgument(
        @NonNull MethodParameter parameter,
        @Nullable ModelAndViewContainer mavContainer,
        @NonNull NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
            authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof UserEntity user) {
            return user;
        }

        throw new IllegalArgumentException("User is not authenticated");
    }
}

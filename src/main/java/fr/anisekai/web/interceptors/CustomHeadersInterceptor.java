package fr.anisekai.web.interceptors;

import fr.anisekai.BuildInfo;
import fr.anisekai.web.annotations.DeprecatedApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class CustomHeadersInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            ModelAndView modelAndView
    ) {

        response.addHeader("X-ANSK-Version", BuildInfo.getVersion());

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }

        DeprecatedApi annotation = handlerMethod.getMethodAnnotation(DeprecatedApi.class);

        if (annotation == null) return;

        response.addHeader("Deprecation", String.format("@%s", annotation.since()));
    }

}

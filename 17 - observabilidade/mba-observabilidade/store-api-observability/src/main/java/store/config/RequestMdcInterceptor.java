package store.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
class RequestMdcInterceptor implements HandlerInterceptor {

    private final BuildProperties buildProperties;

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {
        MDC.put("appName", buildProperties.getName());
        MDC.put("appVersion", buildProperties.getVersion());
        MDC.put("appBuildDate", buildProperties.getTime().toString());
        MDC.put("traceId", request.getHeader("x-trace-id"));
        MDC.put("host", request.getHeader("Host"));
        return true;
    }
}

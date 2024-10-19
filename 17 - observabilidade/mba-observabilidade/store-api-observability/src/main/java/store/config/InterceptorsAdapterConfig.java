package store.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
class InterceptorsAdapterConfig implements WebMvcConfigurer {

    private final RequestMdcInterceptor requestMdcInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}

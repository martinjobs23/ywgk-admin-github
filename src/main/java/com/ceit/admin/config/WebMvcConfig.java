package com.ceit.admin.config;

import java.util.List;

import com.ceit.admin.common.interceptors.AuthInterceptor;
import com.ceit.config.WebMvcConfigurer;
import com.ceit.interceptor.InterceptorRegistry;
import com.ceit.ioc.annotations.Configuration;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor()).addPathPatterns("/*").excludePathPatterns("/login/**");
    }

    @Override
    public void addScanPath(List<String> paths) {
        paths.add("com.ceit.test");
    }

}

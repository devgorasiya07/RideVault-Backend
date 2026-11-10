package com.ridevault.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC Web Configuration.
 * Maps dedicated portal endpoints to their respective HTML templates.
 *
 * Endpoints:
 *  - /rider or /rider-portal  --> rider.html
 *  - /driver or /driver-portal --> driver.html
 *  - /labs or /hub            --> index.html
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/rider").setViewName("forward:/rider.html");
        registry.addViewController("/rider-portal").setViewName("forward:/rider.html");
        registry.addViewController("/driver").setViewName("forward:/driver.html");
        registry.addViewController("/driver-portal").setViewName("forward:/driver.html");
        registry.addViewController("/labs").setViewName("forward:/index.html");
        registry.addViewController("/hub").setViewName("forward:/index.html");
    }
}

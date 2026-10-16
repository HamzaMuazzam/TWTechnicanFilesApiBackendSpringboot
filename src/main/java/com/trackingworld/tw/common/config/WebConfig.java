package com.trackingworld.tw.common.config;

import com.trackingworld.tw.common.ApiPaths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the admin dashboard SPA from classpath:/static/admin.
 * Real files (assets, favicon) are served as-is; any other /admin/** path falls back to
 * index.html so React Router's history mode works on refresh / deep links.
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String SPA_LOCATION = "classpath:/static/admin/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ApiPaths.ADMIN_SPA + "/**")
                .addResourceLocations(SPA_LOCATION)
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(ApiPaths.ADMIN_SPA).setViewName("forward:" + ApiPaths.ADMIN_SPA + "/index.html");
        registry.addViewController(ApiPaths.ADMIN_SPA + "/").setViewName("forward:" + ApiPaths.ADMIN_SPA + "/index.html");
    }

    static final class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (looksLikeFile(resourcePath)) {
                return null;
            }
            Resource index = location.createRelative("index.html");
            return index.exists() && index.isReadable() ? index : null;
        }

        /** "assets/app.js", "favicon.ico" -> real file requests, never rewritten to index.html. */
        private static boolean looksLikeFile(String path) {
            String last = path.substring(path.lastIndexOf('/') + 1);
            return path.startsWith("assets/") || last.contains(".");
        }
    }
}

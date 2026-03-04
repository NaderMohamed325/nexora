package com.neo.nexora.filter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {
    private final Bucket bucket;

    public RateLimitingFilter(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(servletRequest, servletResponse); // Forward the request if rate limiting is not hit
        } else {
            ((HttpServletResponse) servletResponse).setStatus(429); // Return 429 if rate limit is exceeded
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"You have exceeded the rate limit. Please try again later.\"}");
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}

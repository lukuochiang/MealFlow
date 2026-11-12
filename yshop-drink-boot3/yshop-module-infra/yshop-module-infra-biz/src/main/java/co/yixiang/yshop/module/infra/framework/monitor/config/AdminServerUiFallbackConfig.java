package co.yixiang.yshop.module.infra.framework.monitor.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.List;

/**
 * 修复 Spring Boot Admin（SBA）“服务监控”菜单 404（NoResourceFoundException）的问题。
 *
 * <p>现象：点击菜单打开 {@code /admin/applications}（Vue SPA history 模式深链接），直接落到
 * {@code ResourceHttpRequestHandler} 找静态文件，找不到就抛异常 → 前端“系统异常”。
 * 正常应由 SBA 自带的 {@code HomepageForwardingFilter} 转发到首页、并由其 UI 资源处理器返回
 * {@code index.html}。但在本部署里 SBA 的 {@code AdminServerUiAutoConfiguration} 因
 * {@code @ConditionalOnBean} 时序问题未正常生效，导致：① UI 静态资源处理器可能未注册；
 * ② 转发到目录 {@code /admin/} 时 welcome-file 解析在转发请求下失败。
 *
 * <p>这里显式兜底：
 * <ol>
 *   <li>注册 {@code /admin/**} 静态资源 → SBA UI 在 jar 内的真实位置
 *       （{@code classpath:/META-INF/spring-boot-admin-server-ui/}），保证 index.html / main.js 等可访问；</li>
 *   <li>用过滤器把“GET + 浏览器 HTML + 非静态资源 + 非后台业务接口 /admin-api”的 /admin 深链接，
 *       转发到<b>具体的 {@code /admin/index.html} 文件</b>（而非目录），避开转发下 welcome 解析失败。</li>
 * </ol>
 */
@Configuration
public class AdminServerUiFallbackConfig implements WebMvcConfigurer {

    /** SBA 上下文路径，与 spring.boot.admin.context-path 保持一致（默认 /admin） */
    @Value("${spring.boot.admin.context-path:/admin}")
    private String contextPath;

    /** SBA UI 静态资源在 jar 内的真实位置（见 spring-boot-admin-server-ui 的 AdminServerUiProperties） */
    private static final String SBA_UI_RESOURCE_LOCATION = "classpath:/META-INF/spring-boot-admin-server-ui/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = normalizeBase(contextPath);
        // 显式注册 SBA UI 静态资源；若 SBA 自带处理器已生效，本注册与之等价（路径/位置相同），不会冲突
        registry.addResourceHandler(base + "/**")
                .addResourceLocations(SBA_UI_RESOURCE_LOCATION)
                .addResourceLocations(SBA_UI_RESOURCE_LOCATION + "extensions/");
    }

    @Bean
    public FilterRegistrationBean<Filter> adminServerUiSpaFallbackFilter() {
        String base = normalizeBase(contextPath);
        // 转发到「具体的 index.html 文件」，而非目录 /admin/（转发请求下目录 welcome 解析会失败）
        final String fallback = base + "/index.html";

        Filter filter = new SpaFallbackFilter(base, fallback);
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("adminServerUiSpaFallbackFilter");
        registration.addUrlPatterns("/*");
        // 在 DispatcherServlet / SBA 资源处理之前介入；晚于 Spring Security 不影响鉴权
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registration;
    }

    private static String normalizeBase(String contextPath) {
        String base = (contextPath == null || contextPath.isEmpty()) ? "/admin" : contextPath;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * SPA 深链接 fallback：把符合条件的 /admin 深链接转发到 SBA 首页文件。
     * 用构造参数传入路径，避免匿名内部类引用外层局部变量。
     */
    private static final class SpaFallbackFilter implements Filter {

        private final String base;
        private final String fallback;

        private SpaFallbackFilter(String base, String fallback) {
            this.base = base;
            this.fallback = fallback;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (!(request instanceof HttpServletRequest httpRequest)
                    || !(response instanceof HttpServletResponse httpResponse)) {
                chain.doFilter(request, response);
                return;
            }
            String path = httpRequest.getServletPath();
            if (path == null || path.isEmpty()) {
                path = httpRequest.getRequestURI();
            }
            if (shouldForward(path, httpRequest) && !httpResponse.isCommitted()) {
                httpRequest.getRequestDispatcher(fallback).forward(request, response);
                return;
            }
            chain.doFilter(request, response);
        }

        private boolean shouldForward(String path, HttpServletRequest request) {
            // 仅处理 GET
            if (!"GET".equals(request.getMethod())) {
                return false;
            }
            // 处理 /admin 本身及 /admin/xxx 深链接；排除后台业务接口前缀 /admin-api
            boolean isAdminPath = path.equals(base) || path.startsWith(base + "/");
            if (!isAdminPath || path.startsWith(base + "-api/")) {
                return false;
            }
            // 已指向首页文件 / 目录，避免循环
            if (path.equals(base + "/") || path.equals(base + "/index.html")) {
                return false;
            }
            // 静态资源（带扩展名，如 .js/.css/.png）不转发，交给资源处理器
            String lastSegment = path.substring(path.lastIndexOf('/') + 1);
            if (lastSegment.indexOf('.') >= 0) {
                return false;
            }
            // 仅浏览器 HTML 请求才转发（API 调用 Accept 为 application/json，不命中）
            List<MediaType> accepts = MediaType.parseMediaTypes(request.getHeader(HttpHeaders.ACCEPT));
            return accepts.stream().anyMatch(t -> t.includes(MediaType.TEXT_HTML));
        }
    }
}

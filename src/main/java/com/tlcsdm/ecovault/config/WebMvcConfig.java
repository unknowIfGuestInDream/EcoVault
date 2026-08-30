package com.tlcsdm.ecovault.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 静态资源缓存配置。
 *
 * <p>
 * 为 favicon、CSS、JS、图片等静态资源注册资源处理器，并设置 30 天公共缓存头
 * ({@code Cache-Control: max-age=2592000, public})，减少重复下载、提升页面加载速度。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see WebMvcConfigurer
 */
@Configuration

public class WebMvcConfig implements WebMvcConfigurer {

	/**
	 * 为静态资源添加 30 天公共缓存头，确保浏览器复用 favicon、CSS、JS 等文件。
	 * @param registry registry参数。
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		CacheControl cacheControl = CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();
		registry.addResourceHandler("/favicon.ico")
			.addResourceLocations("classpath:/static/")
			.setCacheControl(cacheControl);
		registry.addResourceHandler("/css/**")
			.addResourceLocations("classpath:/static/css/")
			.setCacheControl(cacheControl);
		registry.addResourceHandler("/js/**")
			.addResourceLocations("classpath:/static/js/")
			.setCacheControl(cacheControl);
		registry.addResourceHandler("/images/**")
			.addResourceLocations("classpath:/static/images/")
			.setCacheControl(cacheControl);
	}

}

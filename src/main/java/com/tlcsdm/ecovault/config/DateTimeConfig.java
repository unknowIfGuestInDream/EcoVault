package com.tlcsdm.ecovault.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 统一 Spring MVC 的时间格式与系统时区配置。
 *
 * <p>
 * 系统统一使用北京时间 (东八区，GMT+8)。日期时间对外展示格式为 {@code yyyy-MM-dd HH:mm:ss} (不含中间的 {@code T})， 具体
 * JSON 字段通过 {@code @JsonFormat} 注解声明，便于用户管理、日志管理等页面直接展示可读的北京时间。
 * </p>
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see WebMvcConfigurer
 */
@Configuration
/**
 * 日期时间配置类，提供统一的日期与时间格式化能力。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see WebMvcConfigurer
 */
public class DateTimeConfig implements WebMvcConfigurer {

	/** 统一日期格式模式。 */
	public static final String DATE_PATTERN = "yyyy-MM-dd";

	/** TIME_PATTERN字段说明。 */
	public static final String TIME_PATTERN = "HH:mm:ss";

	/** 统一日期时间格式模式。 */
	public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	/** TIME_ZONE字段说明。 */
	public static final String TIME_ZONE = "GMT+8";

	/** 系统默认使用的时区。 */
	public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(TIME_ZONE);

	/**
	 * 应用启动时将 JVM 默认时区设置为北京时间，确保 {@link LocalDateTime#now()} 等取到东八区时间。
	 */
	@PostConstruct
	public void initDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
	}

	/**
	 * 处理addFormatters相关业务。
	 * @param registry registry参数。
	 */
	@Override
	public void addFormatters(FormatterRegistry registry) {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateFormatter(DateTimeFormatter.ofPattern(DATE_PATTERN));
		registrar.setTimeFormatter(DateTimeFormatter.ofPattern(TIME_PATTERN));
		registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
		registrar.registerFormatters(registry);
	}

}

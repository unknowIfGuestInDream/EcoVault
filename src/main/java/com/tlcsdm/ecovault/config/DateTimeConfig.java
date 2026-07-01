package com.tlcsdm.ecovault.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 统一 Jackson 与 Spring MVC 的时间格式配置。
 */
@Configuration
public class DateTimeConfig implements WebMvcConfigurer {

    public static final String DATE_PATTERN = "yyyy/MM/dd";

    public static final String TIME_PATTERN = "HH:mm:ss";

    public static final String DATE_TIME_PATTERN = "yyyy/MM/dd HH:mm:ss";

    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("GMT+8");

    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(DATE_PATTERN));
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern(TIME_PATTERN));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
        registrar.registerFormatters(registry);
    }
}

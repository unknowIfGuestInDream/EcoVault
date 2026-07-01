package com.tlcsdm.ecovault.security;

import com.tlcsdm.ecovault.config.BrowserSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 浏览器请求守卫过滤器。
 *
 * <p>
 * 对 API 请求执行来源白名单与 User-Agent 合规校验，仅允许受信任网页上的现代浏览器访问接口。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Component
public class BrowserRequestGuardFilter extends OncePerRequestFilter {

	private static final Pattern EDGE_PATTERN = Pattern.compile("(?:Edg|EdgiOS)/(\\d+)");

	private static final Pattern CHROME_PATTERN = Pattern.compile("(?:Chrome|CriOS)/(\\d+)");

	private static final Pattern FIREFOX_PATTERN = Pattern.compile("(?:Firefox|FxiOS)/(\\d+)");

	private static final Pattern SAFARI_PATTERN = Pattern.compile("Version/(\\d+).+Safari/");

	private static final List<String> BOT_KEYWORDS = List.of("bot", "spider", "crawler", "curl/", "wget/",
			"python-requests", "postmanruntime", "insomnia", "httpclient", "okhttp");

	private final BrowserSecurityProperties browserSecurityProperties;

	public BrowserRequestGuardFilter(BrowserSecurityProperties browserSecurityProperties) {
		this.browserSecurityProperties = browserSecurityProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isTrustedOrigin(request.getHeader(HttpHeaders.ORIGIN))) {
			writeForbidden(response, "请求来源不在允许名单内");
			return;
		}
		if (!isAllowedUserAgent(request.getHeader(HttpHeaders.USER_AGENT))) {
			writeForbidden(response, "仅允许受支持浏览器访问接口");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isTrustedOrigin(String origin) {
		return !StringUtils.hasText(origin) || browserSecurityProperties.getAllowedOrigins().contains(origin);
	}

	private boolean isAllowedUserAgent(String userAgent) {
		if (!StringUtils.hasText(userAgent)) {
			return false;
		}
		String normalized = userAgent.toLowerCase(Locale.ROOT);
		if (BOT_KEYWORDS.stream().anyMatch(normalized::contains)) {
			return false;
		}
		Integer edgeVersion = extractMajorVersion(EDGE_PATTERN, userAgent);
		if (edgeVersion != null) {
			return edgeVersion >= browserSecurityProperties.getMinEdgeVersion();
		}
		Integer chromeVersion = extractMajorVersion(CHROME_PATTERN, userAgent);
		if (chromeVersion != null && !normalized.contains("edg/") && !normalized.contains("edgios/")) {
			return chromeVersion >= browserSecurityProperties.getMinChromeVersion();
		}
		Integer firefoxVersion = extractMajorVersion(FIREFOX_PATTERN, userAgent);
		if (firefoxVersion != null) {
			return firefoxVersion >= browserSecurityProperties.getMinFirefoxVersion();
		}
		Integer safariVersion = extractMajorVersion(SAFARI_PATTERN, userAgent);
		return safariVersion != null && !normalized.contains("chrome/") && !normalized.contains("crios/")
				&& safariVersion >= browserSecurityProperties.getMinSafariVersion();
	}

	private Integer extractMajorVersion(Pattern pattern, String userAgent) {
		Matcher matcher = pattern.matcher(userAgent);
		if (!matcher.find()) {
			return null;
		}
		return Integer.parseInt(matcher.group(1));
	}

	private void writeForbidden(HttpServletResponse response, String message) throws IOException {
		response.sendError(HttpStatus.FORBIDDEN.value(), message);
	}

}

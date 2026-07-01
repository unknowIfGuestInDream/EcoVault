package com.tlcsdm.ecovault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览器访问安全策略配置。
 *
 * <p>
 * 用于维护受信任来源白名单与浏览器最低版本限制，避免开放接口被非网页客户端直接调用。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@ConfigurationProperties(prefix = "ecovault.security.request")
public class BrowserSecurityProperties {

	private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000", "http://127.0.0.1:3000",
			"http://localhost:8100", "http://127.0.0.1:8100", "https://eco.tlcsdm.com"));

	private int minChromeVersion = 120;

	private int minEdgeVersion = 120;

	private int minFirefoxVersion = 120;

	private int minSafariVersion = 17;

	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public int getMinChromeVersion() {
		return minChromeVersion;
	}

	public void setMinChromeVersion(int minChromeVersion) {
		this.minChromeVersion = minChromeVersion;
	}

	public int getMinEdgeVersion() {
		return minEdgeVersion;
	}

	public void setMinEdgeVersion(int minEdgeVersion) {
		this.minEdgeVersion = minEdgeVersion;
	}

	public int getMinFirefoxVersion() {
		return minFirefoxVersion;
	}

	public void setMinFirefoxVersion(int minFirefoxVersion) {
		this.minFirefoxVersion = minFirefoxVersion;
	}

	public int getMinSafariVersion() {
		return minSafariVersion;
	}

	public void setMinSafariVersion(int minSafariVersion) {
		this.minSafariVersion = minSafariVersion;
	}

}

package com.tlcsdm.ecovault.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求相关工具方法。
 *
 * @author unknowIfGuestInDream
 */
public final class WebUtil {

	private WebUtil() {
	}

	/**
	 * 获取客户端真实 IP，兼容常见反向代理头。
	 * @param request HTTP 请求
	 * @return 客户端 IP
	 */
	public static String getClientIp(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}
		String ip = request.getHeader("X-Forwarded-For");
		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			// 可能包含多级代理，取第一个
			int comma = ip.indexOf(',');
			return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
		}
		ip = request.getHeader("X-Real-IP");
		if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
			return ip.trim();
		}
		return request.getRemoteAddr();
	}

}

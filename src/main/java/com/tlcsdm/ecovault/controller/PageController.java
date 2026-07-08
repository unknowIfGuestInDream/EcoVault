package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.security.SecurityUtils;
import com.tlcsdm.ecovault.service.RolePermissionService;
import com.tlcsdm.ecovault.service.impl.PasswordServiceImpl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;

/**
 * 页面路由控制器 (Thymeleaf 服务端渲染)。
 *
 * <p>
 * 仅负责返回视图名称，具体数据通过前端 JS 调用 REST 接口获取。受保护页面的访问控制由 Spring Security
 * 统一处理；对于「可配置页面」，进一步依据角色页面权限进行校验，无权限时重定向到控制台。
 * </p>
 */
@Controller
public class PageController {

	private static final MediaType IMAGE_X_ICON = MediaType.parseMediaType("image/x-icon");

	private final RolePermissionService rolePermissionService;

	public PageController(RolePermissionService rolePermissionService) {
		this.rolePermissionService = rolePermissionService;
	}

	/**
	 * 首页。
	 * @return 视图名
	 */
	@GetMapping("/")
	public String index() {
		return "index";
	}

	/**
	 * 登录页。
	 * @return 视图名
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * favicon 图标。
	 * @return 图标资源
	 */
	@GetMapping(value = "/favicon.ico", produces = "image/x-icon")
	public ResponseEntity<Resource> favicon() {
		Resource favicon = new ClassPathResource("static/favicon.ico");
		return ResponseEntity.ok()
			.contentType(IMAGE_X_ICON)
			.cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
			.body(favicon);
	}

	/**
	 * 控制台首页。
	 * @return 视图名
	 */
	@GetMapping("/dashboard")
	public String dashboard() {
		return "dashboard";
	}

	/**
	 * 密码管理页。
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/passwords")
	public String passwords(Model model) {
		model.addAttribute("maskedSecret", PasswordServiceImpl.MASKED_SECRET);
		return guard("/passwords", "passwords");
	}

	/**
	 * 财务管理 - 工资管理页。
	 * @return 视图名
	 */
	@GetMapping("/finance")
	public String finance() {
		return guard("/finance", "finance");
	}

	/**
	 * 财务管理 - 收入支出管理页。
	 * @return 视图名
	 */
	@GetMapping("/finance/ledger")
	public String ledger() {
		return guard("/finance/ledger", "ledger");
	}

	/**
	 * 个人中心页。
	 * @return 视图名
	 */
	@GetMapping("/profile")
	public String profile() {
		return "profile";
	}

	/**
	 * 后台管理首页 (仅管理员)。
	 * @return 视图名
	 */
	@GetMapping("/admin")
	public String admin() {
		return "admin";
	}

	/**
	 * 后台管理 - 用户管理页 (仅管理员)。
	 * @return 视图名
	 */
	@GetMapping("/admin/users")
	public String users() {
		return "users";
	}

	/**
	 * 后台管理 - 日志管理页 (仅管理员)。
	 * @return 视图名
	 */
	@GetMapping("/admin/logs")
	public String logs() {
		return "logs";
	}

	/**
	 * 后台管理 - 角色管理页 (仅管理员)。
	 * @return 视图名
	 */
	@GetMapping("/admin/roles")
	public String roles() {
		return "roles";
	}

	/**
	 * 校验当前用户是否有权访问指定路径，无权限时重定向到控制台。
	 * @param path 页面路径
	 * @param view 目标视图名
	 * @return 视图名或重定向
	 */
	private String guard(String path, String view) {
		if (rolePermissionService.canAccessPath(SecurityUtils.getCurrentUser().getUser(), path)) {
			return view;
		}
		return "redirect:/dashboard";
	}

}

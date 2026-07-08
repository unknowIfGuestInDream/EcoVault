package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 页面路由控制器集成测试。
 *
 * @author unknowIfGuestInDream
 */
class PageControllerTest extends AbstractWebMvcTest {

	@Test
	@DisplayName("公开页面：首页与登录页可匿名访问")
	void publicPages() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("EcoVault")));
		mockMvc.perform(get("/login")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("受保护页面匿名访问跳转到登录页")
	void protectedPagesRedirect() throws Exception {
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/finance/ledger", "/profile",
				"/admin/users", "/admin/logs", "/admin/roles" }) {
			mockMvc.perform(get(path))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/login"));
		}
	}

	@Test
	@DisplayName("认证用户可访问受保护页面")
	void authenticatedPages() throws Exception {
		var user = authFor(securityUser(1000L, "pageuser", Role.USER));
		for (String path : new String[] { "/dashboard", "/passwords", "/finance", "/finance/ledger", "/profile" }) {
			mockMvc.perform(get(path).with(authentication(user))).andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("工资页展示到账信息顺序并提示年终奖可不填写月份")
	void financePageShowsPaymentSection() throws Exception {
		var user = authFor(securityUser(1001L, "financeuser", Role.USER));
		String html = mockMvc.perform(get("/finance").with(authentication(user)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("月份（年终奖可留空）")))
			.andExpect(content().string(containsString("aria-label=\"关闭\"")))
			.andExpect(content().string(containsString("const month = annual ? 0")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(html).contains("到账信息", "<label>实发金额</label>").doesNotContain("税后附加", "实发金额（银行卡到账）");
		assertOrdered(html, "id=\"f-preTaxSalary\"", "id=\"f-incomeTax\"", "id=\"f-afterTaxSalary\"",
				"id=\"f-seriousIllnessMedical\"", "id=\"f-heatingAllowance\"", "id=\"f-netPay\"");
	}

	private void assertOrdered(String value, String... fragments) {
		int previousIndex = -1;
		for (String fragment : fragments) {
			int currentIndex = value.indexOf(fragment);
			assertThat(currentIndex).as("未找到片段: %s", fragment).isGreaterThanOrEqualTo(0);
			assertThat(currentIndex).isGreaterThan(previousIndex);
			previousIndex = currentIndex;
		}
	}

	@Test
	@DisplayName("后台页面仅管理员可访问，普通用户 403")
	void adminPage() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		var user = authFor(securityUser(2L, "user", Role.USER));
		for (String path : new String[] { "/admin", "/admin/users", "/admin/logs", "/admin/roles" }) {
			mockMvc.perform(get(path).with(authentication(admin))).andExpect(status().isOk());
			mockMvc.perform(get(path).with(authentication(user))).andExpect(status().isForbidden());
		}
	}

	@Test
	@DisplayName("后台首页与用户管理页内容分离")
	void adminOverviewAndUsersPageSeparated() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		mockMvc.perform(get("/admin").with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("构建信息")))
			.andExpect(content().string(containsString("系统状态")))
			.andExpect(content().string(containsString("Actuator 端点概览")))
			.andExpect(content().string(containsString("/api/admin/actuator-endpoints")))
			.andExpect(content().string(not(containsString("后台首页"))))
			.andExpect(content().string(not(containsString("创建用户"))))
			.andExpect(content().string(not(containsString("用户列表"))));
		mockMvc.perform(get("/admin/users").with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("创建用户")))
			.andExpect(content().string(containsString("用户列表")));
	}

	@Test
	@DisplayName("缺少页面权限时 guard 重定向到控制台")
	void configurablePageGuardRedirectsWhenPermissionMissing() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		var user = authFor(securityUser(2L, "user", Role.USER));
		try {
			mockMvc
				.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"pages\":[\"passwords\",\"salary\"]}"))
				.andExpect(status().isOk());
			mockMvc.perform(get("/finance/ledger").with(authentication(user)))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/dashboard"));
		}
		finally {
			mockMvc
				.perform(put("/api/admin/roles/{role}/permissions", "USER").with(authentication(admin))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"pages\":[\"passwords\",\"salary\",\"ledger\"]}"))
				.andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("密码管理页默认使用表格展示并提供详情与关闭按钮")
	void passwordsPageShowsTableAndDetailUi() throws Exception {
		var user = authFor(securityUser(1002L, "pwuser", Role.USER));
		mockMvc.perform(get("/passwords").with(authentication(user)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("列表默认仅显示脱敏密码")))
			.andExpect(content().string(containsString("data-masked-secret=\"******\"")))
			.andExpect(content().string(containsString("<th>密码</th>")))
			.andExpect(content().string(containsString("密码详情")))
			.andExpect(content().string(containsString("aria-label=\"关闭\"")))
			.andExpect(content().string(not(containsString("分类</label>"))))
			.andExpect(content().string(not(containsString("strengthLevel"))));
	}

	@Test
	@DisplayName("公共脚本为确认弹窗提供键盘交互")
	void appJsIncludesConfirmDialogKeyboardSupport() throws Exception {
		mockMvc.perform(get("/js/app.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("event.key === \"Escape\"")))
			.andExpect(content().string(containsString("event.key === \"Enter\"")))
			.andExpect(content().string(containsString("event.key !== \"Tab\"")));
	}

}

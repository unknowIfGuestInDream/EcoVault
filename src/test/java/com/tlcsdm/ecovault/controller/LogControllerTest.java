package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 日志管理接口集成测试，覆盖分页查询与 CSV 导出，及管理员/普通用户不同的数据范围。
 *
 * @author unknowIfGuestInDream
 */
class LogControllerTest extends AbstractWebMvcTest {

	@Test
	@DisplayName("管理员分页查询全部日志")
	void adminList() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		mockMvc
			.perform(get("/api/logs").param("module", "用户管理")
				.param("keyword", "登录")
				.param("page", "0")
				.param("size", "10")
				.with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.content").exists());
	}

	@Test
	@DisplayName("普通用户仅查询自身日志，且分页参数被规整")
	void userListPagination() throws Exception {
		var user = authFor(securityUser(4001L, "loguser", Role.USER));
		// 负页码与超大 size 会被规整到合法范围
		mockMvc.perform(get("/api/logs").param("page", "-5").param("size", "9999").with(authentication(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").exists());
	}

	@Test
	@DisplayName("导出日志 CSV 含 BOM、表头与附件头")
	void exportCsv() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		mockMvc.perform(get("/api/logs/export").with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("operation-logs.csv")))
			.andExpect(content().string(containsString("时间,用户,模块,操作,方法,IP,状态,耗时(ms)")));
	}

	@Test
	@DisplayName("未认证访问日志接口返回 401")
	void unauthenticated() throws Exception {
		mockMvc.perform(get("/api/logs")).andExpect(status().isUnauthorized());
	}

}

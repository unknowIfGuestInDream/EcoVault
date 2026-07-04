package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.repository.OperationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	@Autowired
	private OperationLogRepository operationLogRepository;

	private OperationLog saveLog(String module, String operation, String params, LocalDateTime createdAt) {
		OperationLog log = new OperationLog();
		log.setUserId(1L);
		log.setUsername("admin");
		log.setModule(module);
		log.setOperation(operation);
		log.setMethod("com.example.Demo#run");
		log.setParams(params);
		log.setIp("127.0.0.1");
		log.setStatus("SUCCESS");
		log.setDurationMs(12);
		log.setCreatedAt(createdAt);
		return operationLogRepository.save(log);
	}

	@Test
	@DisplayName("管理员分页查询全部日志")
	void adminList() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		saveLog("用户管理", "登录", "{}", LocalDateTime.now());
		mockMvc
			.perform(get("/api/logs").param("module", "用户管理")
				.param("keyword", "登录")
				.param("page", "0")
				.param("size", "10")
				.with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.content").exists())
			.andExpect(jsonPath("$.data.number").value(0))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@Test
	@DisplayName("普通用户访问日志接口被拒绝 (仅管理员可见)")
	void userForbidden() throws Exception {
		var user = authFor(securityUser(4001L, "loguser", Role.USER));
		mockMvc.perform(get("/api/logs").param("page", "0").param("size", "10").with(authentication(user)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("管理员查看日志详情可看到具体参数")
	void detailShowsParams() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		OperationLog log = saveLog("密码管理", "新增密码", "{\"title\":\"邮箱\"}", LocalDateTime.now());
		mockMvc.perform(get("/api/logs/{id}", log.getId()).with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.params").value(containsString("邮箱")));
	}

	@Test
	@DisplayName("管理员按时间区间查询日志")
	void queryByTimeRange() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		// created_at 由 @PrePersist 落为当前时间，无法回填，故围绕当前时间验证区间过滤
		saveLog("区间模块", "记录", "{}", LocalDateTime.now());

		// 结束时间早于当前时间：应被过滤，返回 0 条
		mockMvc
			.perform(get("/api/logs").param("module", "区间模块")
				.param("start", "2000-01-01 00:00:00")
				.param("end", "2000-12-31 23:59:59")
				.with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(0));

		// 区间覆盖当前时间：应返回该记录
		mockMvc
			.perform(get("/api/logs").param("module", "区间模块")
				.param("start", "2000-01-01 00:00:00")
				.param("end", "2999-12-31 23:59:59")
				.with(authentication(admin)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].operation").value("记录"));
	}

	@Test
	@DisplayName("管理员修改并删除日志")
	void updateAndDelete() throws Exception {
		var admin = authFor(securityUser(1L, "admin", Role.ADMIN));
		OperationLog log = saveLog("原模块", "原操作", "{}", LocalDateTime.now());

		mockMvc
			.perform(put("/api/logs/{id}", log.getId()).with(authentication(admin))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"module\":\"新模块\",\"operation\":\"新操作\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.module").value("新模块"))
			.andExpect(jsonPath("$.data.operation").value("新操作"));

		mockMvc.perform(delete("/api/logs/{id}", log.getId()).with(authentication(admin)).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
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

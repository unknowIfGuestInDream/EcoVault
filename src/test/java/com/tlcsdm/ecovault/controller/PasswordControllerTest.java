package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.PasswordEntryRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 密码管理接口集成测试，覆盖增删改查、按标签筛选与鉴权。
 *
 * @author unknowIfGuestInDream
 */
class PasswordControllerTest extends AbstractWebMvcTest {

	private final SecurityUser owner = securityUser(2001L, "pwowner", Role.USER);

	private Authentication auth() {
		return authFor(owner);
	}

	private long createEntry(String title, List<String> tags) throws Exception {
		PasswordEntryRequest request = new PasswordEntryRequest(title, "account", "Abcdef123!@#", "https://example.com",
				"备注", "分类", tags);
		String body = mockMvc
			.perform(post("/api/passwords").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.secret").value("Abcdef123!@#"))
			.andExpect(jsonPath("$.data.strengthLevel").value("STRONG"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(body).path("data").path("id").asLong();
	}

	@Test
	@DisplayName("完整增删改查与详情/筛选流程")
	void fullCrud() throws Exception {
		long id = createEntry("GitHub", List.of("工作", "代码"));

		// 详情
		mockMvc.perform(get("/api/passwords/{id}", id).with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("GitHub"))
			.andExpect(jsonPath("$.data.secret").value("Abcdef123!@#"));

		// 列表 (含关键字与标签筛选)
		mockMvc.perform(get("/api/passwords").param("keyword", "Git").param("tag", "工作").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].title").value("GitHub"))
			.andExpect(jsonPath("$.data[0].secret").value("******"));

		// 更新
		PasswordEntryRequest update = new PasswordEntryRequest("GitLab", "acc2", "NewSecret1!", "https://gitlab.com",
				"n", "c", List.of("工作"));
		mockMvc
			.perform(put("/api/passwords/{id}", id).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("GitLab"));

		// 删除
		mockMvc.perform(delete("/api/passwords/{id}", id).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0));
	}

	@Test
	@DisplayName("参数校验失败返回 400")
	void validationFails() throws Exception {
		PasswordEntryRequest invalid = new PasswordEntryRequest("", "acc", "secret", null, null, null, null);
		mockMvc
			.perform(post("/api/passwords").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalid)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("查询不存在的条目返回业务错误")
	void getMissing() throws Exception {
		mockMvc.perform(get("/api/passwords/{id}", 999999L).with(authentication(auth())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("未认证访问密码接口返回 401")
	void unauthenticated() throws Exception {
		mockMvc.perform(get("/api/passwords")).andExpect(status().isUnauthorized());
	}

}

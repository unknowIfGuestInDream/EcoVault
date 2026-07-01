package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 财务 - 工资接口集成测试，覆盖录入、更新、统计、导出与删除。
 *
 * @author unknowIfGuestInDream
 */
class SalaryControllerTest extends AbstractWebMvcTest {

	private final SecurityUser owner = securityUser(3001L, "salaryowner", Role.USER);

	private Authentication auth() {
		return authFor(owner);
	}

	private long save(int year, int month, String base) throws Exception {
		SalaryRequest request = new SalaryRequest(year, month, new BigDecimal(base), new BigDecimal("1000"),
				new BigDecimal("500"), new BigDecimal("800"), "备注");
		String body = mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(body).path("data").path("id").asLong();
	}

	@Test
	@DisplayName("录入/更新/统计/删除完整流程")
	void fullFlow() throws Exception {
		long id = save(2030, 1, "10000");
		save(2030, 2, "11000");

		// 列表 (按年份)
		mockMvc.perform(get("/api/finance/salaries").param("year", "2030").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));

		// 全部列表 (无年份)
		mockMvc.perform(get("/api/finance/salaries").with(authentication(auth()))).andExpect(status().isOk());

		// 统计
		mockMvc.perform(get("/api/finance/salaries/statistics").param("year", "2030").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.monthlyTrend.length()").value(2));

		// 更新指定记录
		SalaryRequest update = new SalaryRequest(2030, 1, new BigDecimal("12000"), new BigDecimal("0"),
				new BigDecimal("0"), new BigDecimal("0"), "调整");
		mockMvc
			.perform(put("/api/finance/salaries/{id}", id).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.net").value(12000));

		// 删除
		mockMvc.perform(delete("/api/finance/salaries/{id}", id).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("导出工资 CSV 含 BOM、表头与附件头")
	void exportCsv() throws Exception {
		save(2031, 6, "9000");
		mockMvc.perform(get("/api/finance/salaries/export").param("year", "2031").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("salary.csv")))
			.andExpect(content().string(containsString("年份,月份,基本工资")));
	}

	@Test
	@DisplayName("更新不存在的工资记录返回业务错误")
	void updateMissing() throws Exception {
		SalaryRequest request = new SalaryRequest(2030, 3, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null);
		mockMvc
			.perform(put("/api/finance/salaries/{id}", 888888L).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("参数校验失败返回 400")
	void validationFails() throws Exception {
		// 月份非法 (超出 1-12)
		SalaryRequest invalid = new SalaryRequest(2030, 13, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null);
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalid)))
			.andExpect(status().isBadRequest());
	}

}

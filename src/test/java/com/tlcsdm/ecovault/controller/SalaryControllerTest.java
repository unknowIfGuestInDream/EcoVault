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
 * 财务 - 工资接口集成测试，覆盖录入、更新、统计、导出、年终奖与删除。
 *
 * @author unknowIfGuestInDream
 */
class SalaryControllerTest extends AbstractWebMvcTest {

	private final SecurityUser owner = securityUser(3001L, "salaryowner", Role.USER);

	private Authentication auth() {
		return authFor(owner);
	}

	/**
	 * 构造工资请求：基本工资 base、奖金 1000、租房补助 500、医疗扣除 800，其余为 null。
	 */
	private SalaryRequest requestOf(int year, int month, String base) {
		return new SalaryRequest(year, month, new BigDecimal(base), null, new BigDecimal("500"), null, null, null, null,
				new BigDecimal("1000"), null, null, null, new BigDecimal("800"), null, null, null, null, null, null,
				"备注");
	}

	private long save(int year, int month, String base) throws Exception {
		String body = mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestOf(year, month, base))))
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

		// 更新指定记录：仅基本工资 12000，其余为 0 -> 实发 = 12000
		SalaryRequest update = new SalaryRequest(2030, 1, new BigDecimal("12000"), null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, "调整");
		mockMvc
			.perform(put("/api/finance/salaries/{id}", id).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.netPay").value(12000));

		// 删除
		mockMvc.perform(delete("/api/finance/salaries/{id}", id).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("录入年终奖记录 (月份为 0) 并在统计中单独汇总")
	void annualBonusFlow() throws Exception {
		SalaryRequest annual = new SalaryRequest(2032, 0, null, null, null, null, null, null, null,
				new BigDecimal("50000"), null, null, null, null, null, null, null, null, null, null, "年终奖");
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(annual)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.annualBonus").value(true));

		mockMvc.perform(get("/api/finance/salaries/statistics").param("year", "2032").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalAnnualBonus").value(50000))
			.andExpect(jsonPath("$.data.monthlyTrend.length()").value(0));
	}

	@Test
	@DisplayName("导出工资 CSV 含 BOM、表头与附件头")
	void exportCsv() throws Exception {
		save(2031, 6, "9000");
		mockMvc.perform(get("/api/finance/salaries/export").param("year", "2031").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("salary.csv")))
			.andExpect(content().string(containsString("年份,月份,基本工资,绩效工资")));
	}

	@Test
	@DisplayName("更新不存在的工资记录返回业务错误")
	void updateMissing() throws Exception {
		mockMvc
			.perform(put("/api/finance/salaries/{id}", 888888L).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestOf(2030, 3, "0"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("参数校验失败返回 400")
	void validationFails() throws Exception {
		// 月份非法 (超出 0-12)
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestOf(2030, 13, "0"))))
			.andExpect(status().isBadRequest());
	}

}

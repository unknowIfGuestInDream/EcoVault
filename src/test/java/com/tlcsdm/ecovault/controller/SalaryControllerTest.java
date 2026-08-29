package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 财务 - 工资接口集成测试，覆盖录入、更新、统计、导出、年终奖、删除、跨年查询与 CSV 导入。
 *
 * @author unknowIfGuestInDream
 */
class SalaryControllerTest extends AbstractWebMvcTest {

	private final SecurityUser owner = securityUser(3001L, "salaryowner", Role.USER);

	private Authentication auth() {
		return authFor(owner);
	}

	/**
	 * 构造工资请求：基本工资 base、奖金 1000、租房补助 500、医疗扣除 800、实发金额 netPay，其余为 null。
	 */
	private SalaryRequest requestOf(int year, int month, String base, String netPay) {
		return new SalaryRequest(year, month, new BigDecimal(base), null, new BigDecimal("500"), null, null, null, null,
				new BigDecimal("1000"), null, null, null, new BigDecimal("800"), null, null, null, null, null, null,
				new BigDecimal(netPay), "备注");
	}

	private long save(int year, int month, String base, String netPay) throws Exception {
		String body = mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestOf(year, month, base, netPay))))
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
		long id = save(2030, 1, "10000", "10666.66");
		save(2030, 2, "11000", "11777.77");

		// 列表 (按年份区间，startYear=endYear=2030)
		mockMvc
			.perform(get("/api/finance/salaries").param("startYear", "2030")
				.param("endYear", "2030")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].preTaxSalary").value(10700))
			.andExpect(jsonPath("$.data[0].incomeTax").value(0))
			.andExpect(jsonPath("$.data[0].afterTaxSalary").value(10700))
			.andExpect(jsonPath("$.data[0].seriousIllnessMedical").value(0))
			.andExpect(jsonPath("$.data[0].heatingAllowance").value(0))
			.andExpect(jsonPath("$.data[0].netPay").value(10666.66))
			.andExpect(jsonPath("$.data[1].netPay").value(11777.77));

		// 全部列表 (无年份)
		mockMvc.perform(get("/api/finance/salaries").with(authentication(auth()))).andExpect(status().isOk());

		// 统计
		mockMvc
			.perform(get("/api/finance/salaries/statistics").param("startYear", "2030")
				.param("endYear", "2030")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.monthlyTrend.length()").value(2))
			.andExpect(jsonPath("$.data.composition.baseSalary").value(21000))
			.andExpect(jsonPath("$.data.composition.bonus").value(2000))
			.andExpect(jsonPath("$.data.deductionComposition.medical").value(1600))
			.andExpect(jsonPath("$.data.deductionComposition.incomeTax").value(0));

		// 更新指定记录
		SalaryRequest update = new SalaryRequest(2030, 1, new BigDecimal("12000"), null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, new BigDecimal("11999.99"), "调整");
		mockMvc
			.perform(put("/api/finance/salaries/{id}", id).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.netPay").value(11999.99));

		// 删除
		mockMvc.perform(delete("/api/finance/salaries/{id}", id).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("录入年终奖记录 (月份为 0) 并在统计中单独汇总")
	void annualBonusFlow() throws Exception {
		SalaryRequest annual = new SalaryRequest(2032, 0, null, null, null, null, null, null, null,
				new BigDecimal("50000"), null, null, null, null, null, null, null, null, null, null,
				new BigDecimal("50000"), "年终奖");
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(annual)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.annualBonus").value(true));

		mockMvc
			.perform(get("/api/finance/salaries/statistics").param("startYear", "2032")
				.param("endYear", "2032")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalAnnualBonus").value(50000))
			.andExpect(jsonPath("$.data.monthlyTrend.length()").value(0))
			.andExpect(jsonPath("$.data.composition.baseSalary").value(0))
			.andExpect(jsonPath("$.data.deductionComposition.medical").value(0));
	}

	@Test
	@DisplayName("导出工资 CSV 含 BOM、表头、附件头及正确文件名")
	void exportCsv() throws Exception {
		save(2031, 6, "9000", "9123.45");
		SalaryRequest annual = new SalaryRequest(2031, 0, null, null, null, null, null, null, null,
				new BigDecimal("30000"), null, null, null, null, null, null, null, null, null, null,
				new BigDecimal("30000"), "年终奖");
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(annual)))
			.andExpect(status().isOk());

		// 单年导出，文件名含年份
		mockMvc
			.perform(get("/api/finance/salaries/export").param("startYear", "2031")
				.param("endYear", "2031")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("salary_2031.csv")))
			.andExpect(content().string(containsString("年份,月份,基本工资,绩效工资")))
			.andExpect(content().string(containsString("医疗,养老,失业,公积金")))
			.andExpect(content().string(containsString("税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额")))
			.andExpect(content().string(containsString("9123.45")))
			.andExpect(content().string(containsString("年终奖")));

		// 全量导出，文件名为 salary_all.csv
		mockMvc.perform(get("/api/finance/salaries/export").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("salary_all.csv")));
	}

	@Test
	@DisplayName("跨年区间查询返回对应年份数据")
	void crossYearRangeQuery() throws Exception {
		save(2040, 1, "10000", "10000");
		save(2041, 1, "11000", "11000");
		save(2042, 1, "12000", "12000");

		// 查询 2040-2041，应返回 2 条
		mockMvc
			.perform(get("/api/finance/salaries").param("startYear", "2040")
				.param("endYear", "2041")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));

		// 查询 2040-2042，应返回 3 条
		mockMvc
			.perform(get("/api/finance/salaries").param("startYear", "2040")
				.param("endYear", "2042")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(3));

		// 跨年导出，文件名含年份范围
		mockMvc
			.perform(get("/api/finance/salaries/export").param("startYear", "2040")
				.param("endYear", "2042")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("salary_2040-2042.csv")));
	}

	@Test
	@DisplayName("仅提供 startYear 时等同于单年查询")
	void singleYearViaStartYear() throws Exception {
		save(2045, 3, "9000", "9000");

		mockMvc.perform(get("/api/finance/salaries").param("startYear", "2045").with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	@DisplayName("CSV 导入：成功导入多行数据")
	void importCsvSuccess() throws Exception {
		String csvContent = "\uFEFF年份,月份,基本工资,绩效工资,租房补助,伙食补助,交通补贴,加班费,加班补助,奖金,应发工资,"
				+ "医疗保险缴费基数,养老失业缴费基数,公积金缴费基数,医疗,养老,失业,公积金,扣除项合计,税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额,备注\n"
				+ "2050,1,10000,2000,500,300,200,0,0,1000,14000,5000,8000,6000,400,800,100,600,1900,12100,300,11800,0,0,11800,测试1\n"
				+ "2050,年终奖,0,0,0,0,0,0,0,30000,30000,0,0,0,0,0,0,0,0,0,0,0,0,0,30000,年终奖\n";

		MockMultipartFile file = new MockMultipartFile("file", "salary_import.csv", "text/csv; charset=UTF-8",
				csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/finance/salaries/import").file(file).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(2));

		// 验证导入后可查到数据
		mockMvc
			.perform(get("/api/finance/salaries").param("startYear", "2050")
				.param("endYear", "2050")
				.with(authentication(auth())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	@DisplayName("CSV 导入：上传空文件返回 400")
	void importCsvEmptyFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

		mockMvc.perform(multipart("/api/finance/salaries/import").file(file).with(authentication(auth())).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	@DisplayName("更新不存在的工资记录返回业务错误")
	void updateMissing() throws Exception {
		mockMvc
			.perform(put("/api/finance/salaries/{id}", 888888L).with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestOf(2030, 3, "0", "0"))))
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
				.content(objectMapper.writeValueAsString(requestOf(2030, 13, "0", "0"))))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("未传实发金额时不再兼容旧回填结构")
	void missingNetPayNoLongerBackfills() throws Exception {
		SalaryRequest request = new SalaryRequest(2033, 3, new BigDecimal("10000"), null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, new BigDecimal("300"), new BigDecimal("200"),
				new BigDecimal("100"), null, "未传实发");
		mockMvc
			.perform(post("/api/finance/salaries").with(authentication(auth()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.preTaxSalary").value(10000))
			.andExpect(jsonPath("$.data.afterTaxSalary").value(9700))
			.andExpect(jsonPath("$.data.netPay").value(0));
	}

}

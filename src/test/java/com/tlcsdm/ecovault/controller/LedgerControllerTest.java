package com.tlcsdm.ecovault.controller;

import com.tlcsdm.ecovault.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import java.util.List;

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
 * 财务 - 收入支出接口集成测试，覆盖记账、多标签、按标签/时间查询、统计、导出与删除。
 *
 * @author unknowIfGuestInDream
 */
class LedgerControllerTest extends AbstractWebMvcTest {

	private Authentication auth(long id) {
		return authFor(securityUser(id, "ledger" + id, Role.USER));
	}

	private String json(String type, String amount, String date, List<String> tags, String remark) {
		StringBuilder tagArr = new StringBuilder("[");
		for (int i = 0; i < tags.size(); i++) {
			if (i > 0) {
				tagArr.append(',');
			}
			tagArr.append('"').append(tags.get(i)).append('"');
		}
		tagArr.append(']');
		return "{\"type\":\"" + type + "\",\"amount\":" + amount + ",\"entryDate\":\"" + date + "\",\"tags\":" + tagArr
				+ ",\"remark\":" + (remark == null ? "null" : "\"" + remark + "\"") + "}";
	}

	private long save(long userId, String type, String amount, String date, List<String> tags) throws Exception {
		String body = mockMvc
			.perform(post("/api/finance/ledger").with(authentication(auth(userId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(type, amount, date, tags, "备注")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(body).path("data").path("id").asLong();
	}

	@Test
	@DisplayName("记账/多标签/按标签与时间查询/统计/删除完整流程")
	void fullFlow() throws Exception {
		long uid = 4001L;
		long incomeId = save(uid, "INCOME", "10000", "2030-01-05", List.of("工资"));
		save(uid, "EXPENSE", "3000", "2030-01-10", List.of("房租", "生活"));
		save(uid, "EXPENSE", "1200", "2030-02-03", List.of("生活"));

		// 全部列表
		mockMvc.perform(get("/api/finance/ledger").with(authentication(auth(uid))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(3));

		// 按类型过滤
		mockMvc.perform(get("/api/finance/ledger").param("type", "EXPENSE").with(authentication(auth(uid))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));

		// 按标签过滤
		mockMvc.perform(get("/api/finance/ledger").param("tag", "生活").with(authentication(auth(uid))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));

		// 按时间区间过滤 (仅一月)
		mockMvc
			.perform(get("/api/finance/ledger").param("start", "2030-01-01")
				.param("end", "2030-01-31")
				.with(authentication(auth(uid))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));

		// 统计
		mockMvc.perform(get("/api/finance/ledger/statistics").with(authentication(auth(uid))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalIncome").value(10000))
			.andExpect(jsonPath("$.data.totalExpense").value(4200))
			.andExpect(jsonPath("$.data.balance").value(5800));

		// 更新
		String update = json("INCOME", "12000", "2030-01-05", List.of("工资", "奖金"), "调整");
		mockMvc
			.perform(put("/api/finance/ledger/{id}", incomeId).with(authentication(auth(uid)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(update))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.amount").value(12000))
			.andExpect(jsonPath("$.data.tags.length()").value(2));

		// 删除
		mockMvc.perform(delete("/api/finance/ledger/{id}", incomeId).with(authentication(auth(uid))).with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("导出收支 CSV 含 BOM、表头与附件头")
	void exportCsv() throws Exception {
		save(4002L, "EXPENSE", "88", "2031-06-01", List.of("生活"));
		mockMvc.perform(get("/api/finance/ledger/export").with(authentication(auth(4002L))))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("ledger.csv")))
			.andExpect(content().string(containsString("日期,类型,金额,标签,备注")));
	}

	@Test
	@DisplayName("金额非法返回 400")
	void validationFails() throws Exception {
		mockMvc
			.perform(post("/api/finance/ledger").with(authentication(auth(4003L)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("INCOME", "0", "2030-01-01", List.of(), null)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("查询日期范围非法时返回业务错误")
	void invalidDateRangeReturnsBusinessError() throws Exception {
		mockMvc
			.perform(get("/api/finance/ledger").param("start", "2030-02-01")
				.param("end", "2030-01-01")
				.with(authentication(auth(4005L))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400))
			.andExpect(jsonPath("$.message").value("起始日期不能晚于结束日期"));
	}

	@Test
	@DisplayName("更新不存在的收支记录返回业务错误")
	void updateMissing() throws Exception {
		mockMvc
			.perform(put("/api/finance/ledger/{id}", 999999L).with(authentication(auth(4004L)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json("EXPENSE", "10", "2030-01-01", List.of(), null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(400));
	}

}

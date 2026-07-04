package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.LedgerRequest;
import com.tlcsdm.ecovault.dto.LedgerResponse;
import com.tlcsdm.ecovault.dto.LedgerStatistics;
import com.tlcsdm.ecovault.entity.LedgerEntry;
import com.tlcsdm.ecovault.entity.LedgerType;
import com.tlcsdm.ecovault.repository.LedgerEntryRepository;
import com.tlcsdm.ecovault.service.impl.LedgerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 财务 - 收入支出服务单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class LedgerServiceImplTest {

	@Mock
	private LedgerEntryRepository repository;

	@InjectMocks
	private LedgerServiceImpl service;

	private LedgerEntry salary;

	private LedgerEntry rent;

	private LedgerEntry food;

	@BeforeEach
	void setUp() {
		salary = entry(1L, LedgerType.INCOME, "10000", LocalDate.of(2026, 1, 5), Set.of("工资"), "一月工资");
		rent = entry(2L, LedgerType.EXPENSE, "3000", LocalDate.of(2026, 1, 10), Set.of("房租", "生活"), "房租");
		food = entry(3L, LedgerType.EXPENSE, "1200", LocalDate.of(2026, 2, 3), Set.of("生活"), "餐饮");
	}

	private LedgerEntry entry(Long id, LedgerType type, String amount, LocalDate date, Set<String> tags,
			String remark) {
		LedgerEntry e = new LedgerEntry();
		e.setId(id);
		e.setUserId(1L);
		e.setType(type);
		e.setAmount(new BigDecimal(amount));
		e.setEntryDate(date);
		e.setTags(new LinkedHashSet<>(tags));
		e.setRemark(remark);
		return e;
	}

	@Test
	@DisplayName("创建收支记录会归一化标签并落库")
	void createNormalizesTags() {
		when(repository.save(any(LedgerEntry.class))).thenAnswer(inv -> {
			LedgerEntry e = inv.getArgument(0);
			e.setId(9L);
			return e;
		});

		LedgerRequest request = new LedgerRequest("income", new BigDecimal("500"), LocalDate.of(2026, 3, 1),
				List.of("奖金", " ", "奖金", "补贴"), "季度奖");
		LedgerResponse response = service.create(1L, request);

		assertThat(response.id()).isEqualTo(9L);
		assertThat(response.type()).isEqualTo("INCOME");
		// 去重、去空白
		assertThat(response.tags()).containsExactly("奖金", "补贴");
	}

	@Test
	@DisplayName("创建时收支类型非法应抛出异常")
	void createRejectsInvalidType() {
		LedgerRequest request = new LedgerRequest("unknown", new BigDecimal("500"), LocalDate.of(2026, 3, 1), List.of(),
				null);
		assertThatThrownBy(() -> service.create(1L, request)).isInstanceOf(BusinessException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("创建时缺少收支类型应抛出异常")
	void createRejectsMissingType() {
		LedgerRequest request = new LedgerRequest(null, new BigDecimal("500"), LocalDate.of(2026, 3, 1), List.of(),
				null);
		assertThatThrownBy(() -> service.create(1L, request)).isInstanceOf(BusinessException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("更新不存在的记录应抛出异常")
	void updateMissingThrows() {
		when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
		LedgerRequest request = new LedgerRequest("expense", new BigDecimal("10"), LocalDate.of(2026, 3, 1), List.of(),
				null);
		assertThatThrownBy(() -> service.update(1L, 99L, request)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("删除他人记录应抛出异常")
	void deleteMissingThrows() {
		when(repository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.delete(1L, 2L)).isInstanceOf(BusinessException.class);
		verify(repository, never()).delete(any());
	}

	@Test
	@DisplayName("查询起始日期晚于结束日期应抛出异常")
	void listRejectsInvalidRange() {
		assertThatThrownBy(() -> service.list(1L, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null))
			.isInstanceOf(BusinessException.class);
		verify(repository, never()).search(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("查询会将空白标签归一化为 null 并透传类型")
	void listNormalizesFilters() {
		when(repository.search(eq(1L), eq(LedgerType.EXPENSE), isNull(), isNull(), isNull()))
			.thenReturn(List.of(rent, food));

		List<LedgerResponse> result = service.list(1L, "expense", null, null, "  ");

		assertThat(result).hasSize(2);
		verify(repository).search(eq(1L), eq(LedgerType.EXPENSE), isNull(), isNull(), isNull());
	}

	@Test
	@DisplayName("统计计算收入、支出、结余与按标签/月度汇总")
	void statisticsAggregates() {
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(salary, rent, food));

		LedgerStatistics stats = service.statistics(1L, null, null, null, null);

		assertThat(stats.totalIncome()).isEqualByComparingTo("10000");
		assertThat(stats.totalExpense()).isEqualByComparingTo("4200");
		assertThat(stats.balance()).isEqualByComparingTo("5800");
		assertThat(stats.count()).isEqualTo(3);
		// 支出标签「生活」在房租和餐饮中累计 3000 + 1200
		assertThat(stats.expenseByTag()).anySatisfy(ta -> {
			if ("生活".equals(ta.tag())) {
				assertThat(ta.amount()).isEqualByComparingTo("4200");
			}
		});
		// 两个月度数据点 2026-01 与 2026-02
		assertThat(stats.monthlyTrend()).hasSize(2);
		assertThat(stats.monthlyTrend().get(0).label()).isEqualTo("2026-01");
		assertThat(stats.monthlyTrend().get(0).income()).isEqualByComparingTo("10000");
		assertThat(stats.monthlyTrend().get(1).label()).isEqualTo("2026-02");
		assertThat(stats.monthlyTrend().get(1).expense()).isEqualByComparingTo("1200");
	}

	@Test
	@DisplayName("统计对空标签记录归类为未分类")
	void statisticsFallsBackToUncategorizedTag() {
		LedgerEntry uncategorized = entry(4L, LedgerType.EXPENSE, "88", LocalDate.of(2026, 2, 6), Set.of(), "杂项");
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(uncategorized));

		LedgerStatistics stats = service.statistics(1L, null, null, null, null);

		assertThat(stats.expenseByTag()).singleElement().satisfies(tagAmount -> {
			assertThat(tagAmount.tag()).isEqualTo("未分类");
			assertThat(tagAmount.amount()).isEqualByComparingTo("88");
		});
	}

	@Test
	@DisplayName("导出 CSV 含 BOM、表头并对含分隔符的字段转义")
	void exportCsvEscapes() {
		LedgerEntry tricky = entry(4L, LedgerType.EXPENSE, "88", LocalDate.of(2026, 2, 6), Set.of("生活"), "含,逗号\"引号");
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(tricky));

		String csv = service.exportCsv(1L, null, null, null, null);

		assertThat(csv).startsWith("\uFEFF");
		assertThat(csv).contains("日期,类型,金额,标签,备注");
		assertThat(csv).contains("支出");
		// 备注中的逗号与引号被正确转义
		assertThat(csv).contains("\"含,逗号\"\"引号\"");
	}

	@Test
	@DisplayName("导出 CSV 时空标签与空备注输出为空字符串")
	void exportCsvHandlesEmptyValues() {
		LedgerEntry blank = entry(5L, LedgerType.INCOME, "66", LocalDate.of(2026, 2, 7), Set.of(), "");
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(blank));

		String csv = service.exportCsv(1L, null, null, null, null);

		assertThat(csv).contains("2026-02-07,收入,66,,");
	}

	@Test
	@DisplayName("导出 CSV 时 null 标签集合与 null 备注安全回退为空")
	void exportCsvHandlesNullValues() {
		LedgerEntry blank = entry(6L, LedgerType.EXPENSE, "12", LocalDate.of(2026, 2, 8), Set.of("临时"), "普通备注");
		blank.setTags(null);
		blank.setRemark(null);
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(blank));

		String csv = service.exportCsv(1L, null, null, null, null);

		assertThat(csv).contains("2026-02-08,支出,12,,");
	}

	@Test
	@DisplayName("导出 CSV 对换行与普通文本分别按规则处理")
	void exportCsvHandlesNewlineAndPlainText() {
		LedgerEntry newline = entry(7L, LedgerType.EXPENSE, "20", LocalDate.of(2026, 2, 9), Set.of("日常"), "第一行\n第二行");
		LedgerEntry plain = entry(8L, LedgerType.INCOME, "30", LocalDate.of(2026, 2, 10), Set.of("工资"), "普通备注");
		LedgerEntry quoteOnly = entry(9L, LedgerType.EXPENSE, "40", LocalDate.of(2026, 2, 11), Set.of("其他"), "引号\"文本");
		when(repository.search(eq(1L), isNull(), isNull(), isNull(), isNull()))
			.thenReturn(List.of(newline, plain, quoteOnly));

		String csv = service.exportCsv(1L, null, null, null, null);

		assertThat(csv).contains("\"第一行\n第二行\"");
		assertThat(csv).contains("2026-02-10,收入,30,工资,普通备注");
		assertThat(csv).contains("\"引号\"\"文本\"");
	}

}

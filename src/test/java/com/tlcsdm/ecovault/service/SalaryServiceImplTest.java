package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.dto.SalaryResponse;
import com.tlcsdm.ecovault.dto.SalaryStatistics;
import com.tlcsdm.ecovault.entity.SalaryRecord;
import com.tlcsdm.ecovault.repository.SalaryRecordRepository;
import com.tlcsdm.ecovault.service.impl.SalaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 财务 - 工资服务单元测试。
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class SalaryServiceImplTest {

	@Mock
	private SalaryRecordRepository repository;

	@InjectMocks
	private SalaryServiceImpl service;

	private SalaryRecord jan;

	private SalaryRecord feb;

	@BeforeEach
	void setUp() {
		jan = record(1L, 2025, 1, "10000", "2000", "500", "1000");
		feb = record(2L, 2025, 2, "10000", "0", "500", "1000");
	}

	private SalaryRecord record(Long id, int year, int month, String base, String bonus, String allowance,
			String deduction) {
		SalaryRecord r = new SalaryRecord();
		r.setId(id);
		r.setUserId(1L);
		r.setYear(year);
		r.setMonth(month);
		r.setBaseSalary(new BigDecimal(base));
		r.setBonus(new BigDecimal(bonus));
		r.setAllowance(new BigDecimal(allowance));
		r.setDeduction(new BigDecimal(deduction));
		return r;
	}

	@Test
	@DisplayName("统计计算合计/均值/最高/最低正确")
	void statisticsComputed() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan, feb));

		SalaryStatistics stats = service.statistics(1L, null);

		// jan net = 10000+2000+500-1000 = 11500; feb net = 10000+0+500-1000 = 9500
		assertThat(stats.totalNet()).isEqualByComparingTo("21000");
		assertThat(stats.averageNet()).isEqualByComparingTo("10500.00");
		assertThat(stats.maxNet()).isEqualByComparingTo("11500");
		assertThat(stats.minNet()).isEqualByComparingTo("9500");
		assertThat(stats.totalBonus()).isEqualByComparingTo("2000");
		assertThat(stats.monthlyTrend()).hasSize(2);
		assertThat(stats.monthlyTrend().get(0).label()).isEqualTo("2025-01");
		assertThat(stats.composition().baseSalary()).isEqualByComparingTo("20000");
	}

	@Test
	@DisplayName("无数据时统计返回全零")
	void statisticsEmpty() {
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2099)).thenReturn(List.of());

		SalaryStatistics stats = service.statistics(1L, 2099);

		assertThat(stats.totalNet()).isEqualByComparingTo("0");
		assertThat(stats.monthlyTrend()).isEmpty();
	}

	@Test
	@DisplayName("导出 CSV 含 BOM 与表头及数据行")
	void exportCsv() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan));

		String csv = service.exportCsv(1L, null);

		assertThat(csv).startsWith("\uFEFF");
		assertThat(csv).contains("年份,月份,基本工资");
		assertThat(csv).contains("2025,1,10000");
	}

	@Test
	@DisplayName("保存时若已存在同年月记录则更新")
	void saveUpsert() {
		SalaryRequest request = new SalaryRequest(2025, 1, new BigDecimal("12000"), new BigDecimal("1000"),
				new BigDecimal("0"), new BigDecimal("500"), "调薪");
		when(repository.findByUserIdAndYearAndMonth(1L, 2025, 1)).thenReturn(Optional.of(jan));
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		assertThat(response.baseSalary()).isEqualByComparingTo("12000");
		assertThat(response.net()).isEqualByComparingTo("12500"); // 12000+1000+0-500
	}

	@Test
	@DisplayName("统计中出现递增净收入时刷新最高值，并对空 remark 正确处理")
	void statisticsUpdatesMaxWhenIncreasing() {
		SalaryRecord mar = record(3L, 2025, 3, "20000", "5000", "1000", "1000");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(jan, feb, mar));

		SalaryStatistics stats = service.statistics(1L, 2025);

		// mar net = 20000+5000+1000-1000 = 25000 为最高
		assertThat(stats.maxNet()).isEqualByComparingTo("25000");
		assertThat(stats.minNet()).isEqualByComparingTo("9500");
	}

	@Test
	@DisplayName("保存不存在的年月记录时新建")
	void saveNewRecord() {
		SalaryRequest request = new SalaryRequest(2026, 6, new BigDecimal("8000"), new BigDecimal("500"), null, null,
				"新记录");
		when(repository.findByUserIdAndYearAndMonth(1L, 2026, 6)).thenReturn(Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		// null 补贴/扣款按 0 处理
		assertThat(response.gross()).isEqualByComparingTo("8500");
		assertThat(response.net()).isEqualByComparingTo("8500");
	}

	@Test
	@DisplayName("更新存在的记录成功")
	void updateSuccess() {
		when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(jan));
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));
		SalaryRequest request = new SalaryRequest(2025, 1, new BigDecimal("15000"), new BigDecimal("0"),
				new BigDecimal("0"), new BigDecimal("0"), "更新");

		SalaryResponse response = service.update(1L, 1L, request);

		assertThat(response.baseSalary()).isEqualByComparingTo("15000");
	}

	@Test
	@DisplayName("删除存在的记录调用仓储删除")
	void deleteSuccess() {
		when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(jan));

		service.delete(1L, 1L);

		org.mockito.Mockito.verify(repository).delete(jan);
	}

	@Test
	@DisplayName("删除不存在的记录抛出业务异常")
	void deleteMissingThrows() {
		when(repository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(1L, 50L)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("按年份查询列表使用年份维度仓储方法")
	void listByYear() {
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(jan, feb));

		List<SalaryResponse> result = service.list(1L, 2025);

		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("不带年份查询列表使用全量仓储方法")
	void listAll() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan));

		List<SalaryResponse> result = service.list(1L, null);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("按年份导出并对含逗号的备注进行 CSV 转义")
	void exportCsvByYearWithEscaping() {
		SalaryRecord withComma = record(9L, 2025, 4, "10000", "0", "0", "0");
		withComma.setRemark("含,逗号\"引号");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(withComma));

		String csv = service.exportCsv(1L, 2025);

		assertThat(csv).contains("\"含,逗号\"\"引号\"");
	}

	@Test
	@DisplayName("导出时对空串、换行、引号与普通备注分别正确处理")
	void exportCsvEscapesVariousRemarks() {
		SalaryRecord empty = record(10L, 2025, 5, "10000", "0", "0", "0");
		empty.setRemark("");
		SalaryRecord plain = record(11L, 2025, 6, "10000", "0", "0", "0");
		plain.setRemark("普通备注");
		SalaryRecord newline = record(12L, 2025, 7, "10000", "0", "0", "0");
		newline.setRemark("第一行\n第二行");
		SalaryRecord quote = record(13L, 2025, 8, "10000", "0", "0", "0");
		quote.setRemark("仅含\"引号");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(empty, plain, newline, quote));

		String csv = service.exportCsv(1L, 2025);

		// 普通备注不加引号 (备注为行末字段)
		assertThat(csv).contains(",普通备注\n");
		// 含换行或引号的备注被引号包裹
		assertThat(csv).contains("\"第一行\n第二行\"");
		assertThat(csv).contains("\"仅含\"\"引号\"");
	}

}

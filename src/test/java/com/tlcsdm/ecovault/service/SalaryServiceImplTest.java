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
import java.util.Map;
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
		jan = record(1L, 2025, 1, "10000", "2000", "500", "1000", "11500");
		feb = record(2L, 2025, 2, "10000", "0", "500", "1000", "9500");
	}

	private SalaryRecord record(Long id, int year, int month, String base, String bonus, String housing,
			String medicalDeduction, String netPay) {
		SalaryRecord r = new SalaryRecord();
		r.setId(id);
		r.setUserId(1L);
		r.setYear(year);
		r.setMonth(month);
		r.setBaseSalary(new BigDecimal(base));
		r.setBonus(new BigDecimal(bonus));
		r.setHousingAllowance(new BigDecimal(housing));
		r.setMedicalDeduction(new BigDecimal(medicalDeduction));
		r.setNetPay(new BigDecimal(netPay));
		return r;
	}

	private SalaryRequest request(int year, int month, String base, String bonus, String netPay) {
		return new SalaryRequest(year, month, new BigDecimal(base), null, null, null, null, null, null,
				new BigDecimal(bonus), null, null, null, null, null, null, null, null, null, null,
				new BigDecimal(netPay), "备注");
	}

	@Test
	@DisplayName("统计计算合计/均值/最高/最低正确")
	void statisticsComputed() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan, feb));

		SalaryStatistics stats = service.statistics(1L, null, null);

		// jan net = 10000+2000+500-1000 = 11500; feb net = 10000+0+500-1000 = 9500
		assertThat(stats.totalNet()).isEqualByComparingTo("21000");
		assertThat(stats.averageNet()).isEqualByComparingTo("10500.00");
		assertThat(stats.maxNet()).isEqualByComparingTo("11500");
		assertThat(stats.minNet()).isEqualByComparingTo("9500");
		assertThat(stats.totalBonus()).isEqualByComparingTo("2000");
		assertThat(stats.totalAnnualBonus()).isEqualByComparingTo("0");
		assertThat(stats.monthlyTrend()).hasSize(2);
		assertThat(stats.monthlyTrend().get(0).label()).isEqualTo("2025-01");
		assertThat(stats.composition().baseSalary()).isEqualByComparingTo("20000");
		assertThat(stats.composition().performanceSalary()).isEqualByComparingTo("0");
		assertThat(stats.composition().housingAllowance()).isEqualByComparingTo("1000");
		assertThat(stats.composition().mealAllowance()).isEqualByComparingTo("0");
		assertThat(stats.composition().transportAllowance()).isEqualByComparingTo("0");
		assertThat(stats.composition().overtimePay()).isEqualByComparingTo("0");
		assertThat(stats.composition().overtimeAllowance()).isEqualByComparingTo("0");
		assertThat(stats.composition().bonus()).isEqualByComparingTo("2000");
		assertThat(stats.deductionComposition().medical()).isEqualByComparingTo("2000");
		assertThat(stats.deductionComposition().pension()).isEqualByComparingTo("0");
		assertThat(stats.deductionComposition().unemployment()).isEqualByComparingTo("0");
		assertThat(stats.deductionComposition().housingFund()).isEqualByComparingTo("0");
		assertThat(stats.deductionComposition().incomeTax()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("年终奖记录单独汇总且不计入月度趋势")
	void statisticsSeparatesAnnualBonus() {
		SalaryRecord annual = record(3L, 2025, SalaryRecord.ANNUAL_BONUS_MONTH, "0", "50000", "0", "0", "50000");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(annual, jan, feb));

		SalaryStatistics stats = service.statistics(1L, 2025, 2025);

		// 年终奖 net = 50000；不进入 totalNet 与 monthlyTrend
		assertThat(stats.totalAnnualBonus()).isEqualByComparingTo("50000");
		assertThat(stats.totalNet()).isEqualByComparingTo("21000");
		assertThat(stats.monthlyTrend()).hasSize(2);
	}

	@Test
	@DisplayName("无数据时统计返回全零")
	void statisticsEmpty() {
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2099)).thenReturn(List.of());

		SalaryStatistics stats = service.statistics(1L, 2099, 2099);

		assertThat(stats.totalNet()).isEqualByComparingTo("0");
		assertThat(stats.averageNet()).isEqualByComparingTo("0");
		assertThat(stats.maxNet()).isEqualByComparingTo("0");
		assertThat(stats.minNet()).isEqualByComparingTo("0");
		assertThat(stats.totalAnnualBonus()).isEqualByComparingTo("0");
		assertThat(stats.monthlyTrend()).isEmpty();
	}

	@Test
	@DisplayName("仅有年终奖时月度指标为零但年终奖合计正确")
	void statisticsOnlyAnnualBonus() {
		SalaryRecord annual = record(3L, 2025, SalaryRecord.ANNUAL_BONUS_MONTH, "0", "30000", "0", "0", "30000");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(annual));

		SalaryStatistics stats = service.statistics(1L, 2025, 2025);

		assertThat(stats.totalNet()).isEqualByComparingTo("0");
		assertThat(stats.averageNet()).isEqualByComparingTo("0");
		assertThat(stats.totalAnnualBonus()).isEqualByComparingTo("30000");
		assertThat(stats.monthlyTrend()).isEmpty();
	}

	@Test
	@DisplayName("导出 CSV 含 BOM 与表头及数据行")
	void exportCsv() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan));

		String csv = service.exportCsv(1L, null, null).get("csv");

		assertThat(csv).startsWith("\uFEFF");
		assertThat(csv).contains("年份,月份,基本工资,绩效工资");
		assertThat(csv).contains("税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额");
		assertThat(csv).contains("2025,1,10000");
	}

	@Test
	@DisplayName("导出 CSV 年终奖行月份列显示为年终奖")
	void exportCsvAnnualBonus() {
		SalaryRecord annual = record(3L, 2025, SalaryRecord.ANNUAL_BONUS_MONTH, "0", "50000", "0", "0", "50000");
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(annual));

		String csv = service.exportCsv(1L, null, null).get("csv");

		assertThat(csv).contains("2025,年终奖,0");
	}

	@Test
	@DisplayName("保存时若已存在同年月记录则更新")
	void saveUpsert() {
		SalaryRequest request = request(2025, 1, "12000", "1000", "12345.67");
		when(repository.findByUserIdAndYearAndMonth(1L, 2025, 1)).thenReturn(Optional.of(jan));
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		assertThat(response.baseSalary()).isEqualByComparingTo("12000");
		assertThat(response.netPay()).isEqualByComparingTo("12345.67");
		assertThat(response.annualBonus()).isFalse();
	}

	@Test
	@DisplayName("保存年终奖记录 (月份为 0)")
	void saveAnnualBonus() {
		SalaryRequest request = request(2025, SalaryRecord.ANNUAL_BONUS_MONTH, "0", "60000", "60000");
		when(repository.findByUserIdAndYearAndMonth(1L, 2025, 0)).thenReturn(Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		assertThat(response.annualBonus()).isTrue();
		assertThat(response.grossPay()).isEqualByComparingTo("60000");
	}

	@Test
	@DisplayName("统计中出现递增净收入时刷新最高值")
	void statisticsUpdatesMaxWhenIncreasing() {
		SalaryRecord mar = record(3L, 2025, 3, "20000", "5000", "1000", "1000", "25000");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(jan, feb, mar));

		SalaryStatistics stats = service.statistics(1L, 2025, 2025);

		// mar net = 20000+5000+1000-1000 = 25000 为最高
		assertThat(stats.maxNet()).isEqualByComparingTo("25000");
		assertThat(stats.minNet()).isEqualByComparingTo("9500");
	}

	@Test
	@DisplayName("保存不存在的年月记录时新建并使用显式实发金额")
	void saveNewRecord() {
		SalaryRequest request = new SalaryRequest(2026, 6, new BigDecimal("8000"), null, null, null, null, null, null,
				new BigDecimal("500"), null, null, null, null, null, null, null, null, null, null,
				new BigDecimal("8200"), "新记录");
		when(repository.findByUserIdAndYearAndMonth(1L, 2026, 6)).thenReturn(Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		assertThat(response.grossPay()).isEqualByComparingTo("8500");
		assertThat(response.netPay()).isEqualByComparingTo("8200");
	}

	@Test
	@DisplayName("未提供实发金额时不再按税后工资与补贴回填")
	void saveWithoutNetPayNoLongerFallsBack() {
		SalaryRequest request = new SalaryRequest(2026, 7, new BigDecimal("10000"), null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, new BigDecimal("300"), new BigDecimal("200"),
				new BigDecimal("100"), null, "缺少实发");
		when(repository.findByUserIdAndYearAndMonth(1L, 2026, 7)).thenReturn(Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		SalaryResponse response = service.save(1L, request);

		assertThat(response.preTaxSalary()).isEqualByComparingTo("10000");
		assertThat(response.afterTaxSalary()).isEqualByComparingTo("9700");
		assertThat(response.netPay()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("更新存在的记录成功")
	void updateSuccess() {
		when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(jan));
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> inv.getArgument(0));
		SalaryRequest request = request(2025, 1, "15000", "0", "14999.99");

		SalaryResponse response = service.update(1L, 1L, request);

		assertThat(response.baseSalary()).isEqualByComparingTo("15000");
		assertThat(response.netPay()).isEqualByComparingTo("14999.99");
	}

	@Test
	@DisplayName("更新不存在的记录抛出业务异常")
	void updateMissingThrows() {
		when(repository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(1L, 50L, request(2025, 1, "0", "0", "0")))
			.isInstanceOf(BusinessException.class);
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

		List<SalaryResponse> result = service.list(1L, 2025, 2025);

		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("不带年份查询列表使用全量仓储方法")
	void listAll() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan));

		List<SalaryResponse> result = service.list(1L, null, null);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("按年份导出并对含逗号的备注进行 CSV 转义")
	void exportCsvByYearWithEscaping() {
		SalaryRecord withComma = record(9L, 2025, 4, "10000", "0", "0", "0", "10000");
		withComma.setRemark("含,逗号\"引号");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(withComma));

		String csv = service.exportCsv(1L, 2025, 2025).get("csv");

		assertThat(csv).contains("\"含,逗号\"\"引号\"");
	}

	@Test
	@DisplayName("导出时对空串、换行、引号与普通备注分别正确处理")
	void exportCsvEscapesVariousRemarks() {
		SalaryRecord empty = record(10L, 2025, 5, "10000", "0", "0", "0", "10000");
		empty.setRemark("");
		SalaryRecord plain = record(11L, 2025, 6, "10000", "0", "0", "0", "10000");
		plain.setRemark("普通备注");
		SalaryRecord newline = record(12L, 2025, 7, "10000", "0", "0", "0", "10000");
		newline.setRemark("第一行\n第二行");
		SalaryRecord quote = record(13L, 2025, 8, "10000", "0", "0", "0", "10000");
		quote.setRemark("仅含\"引号");
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(empty, plain, newline, quote));

		String csv = service.exportCsv(1L, 2025, 2025).get("csv");

		// 普通备注不加引号 (备注为行末字段)
		assertThat(csv).contains(",普通备注\n");
		// 含换行或引号的备注被引号包裹
		assertThat(csv).contains("\"第一行\n第二行\"");
		assertThat(csv).contains("\"仅含\"\"引号\"");
	}

	@Test
	@DisplayName("跨年区间查询调用范围仓储方法并返回全部数据")
	void listByYearRange() {
		SalaryRecord r2022 = record(20L, 2022, 1, "9000", "0", "0", "0", "9000");
		SalaryRecord r2023 = record(21L, 2023, 1, "9500", "0", "0", "0", "9500");
		when(repository.findByUserIdAndYearBetweenOrderByYearAscMonthAsc(1L, 2022, 2023))
			.thenReturn(List.of(r2022, r2023));

		List<SalaryResponse> result = service.list(1L, 2022, 2023);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).year()).isEqualTo(2022);
		assertThat(result.get(1).year()).isEqualTo(2023);
	}

	@Test
	@DisplayName("仅提供 startYear 时等同于单年查询")
	void listByStartYearOnly() {
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2024)).thenReturn(List.of(jan));

		List<SalaryResponse> result = service.list(1L, 2024, null);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("导出文件名：全量导出为 salary_all.csv")
	void exportFilenameAll() {
		when(repository.findByUserIdOrderByYearAscMonthAsc(1L)).thenReturn(List.of(jan));

		Map<String, String> result = service.exportCsv(1L, null, null);

		assertThat(result.get("filename")).isEqualTo("salary_all.csv");
	}

	@Test
	@DisplayName("导出文件名：单年导出含年份")
	void exportFilenameSingleYear() {
		when(repository.findByUserIdAndYearOrderByMonthAsc(1L, 2025)).thenReturn(List.of(jan));

		Map<String, String> result = service.exportCsv(1L, 2025, 2025);

		assertThat(result.get("filename")).isEqualTo("salary_2025.csv");
	}

	@Test
	@DisplayName("导出文件名：跨年导出含年份范围")
	void exportFilenameYearRange() {
		SalaryRecord r2022 = record(20L, 2022, 1, "9000", "0", "0", "0", "9000");
		when(repository.findByUserIdAndYearBetweenOrderByYearAscMonthAsc(1L, 2022, 2024)).thenReturn(List.of(r2022));

		Map<String, String> result = service.exportCsv(1L, 2022, 2024);

		assertThat(result.get("filename")).isEqualTo("salary_2022-2024.csv");
	}

	@Test
	@DisplayName("CSV 导入：正常数据导入成功并返回条数")
	void importCsvSuccess() {
		when(repository.findByUserIdAndYearAndMonth(1L, 2060, 1)).thenReturn(java.util.Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
			SalaryRecord r = inv.getArgument(0);
			r.setId(99L);
			return r;
		});
		String importCsv = "年份,月份,基本工资,绩效工资,租房补助,伙食补助,交通补贴,加班费,加班补助,奖金,应发工资,"
				+ "医疗保险缴费基数,养老失业缴费基数,公积金缴费基数,医疗,养老,失业,公积金,扣除项合计,税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额,备注\n"
				+ "2060,1,10000,0,0,0,0,0,0,0,10000,0,0,0,0,0,0,0,0,10000,0,10000,0,0,10000,导入测试\n";

		int count = service.importCsv(1L, importCsv);

		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("CSV 导入：年终奖行月份为年终奖文本")
	void importCsvAnnualBonus() {
		when(repository.findByUserIdAndYearAndMonth(1L, 2061, 0)).thenReturn(java.util.Optional.empty());
		when(repository.save(any(SalaryRecord.class))).thenAnswer(inv -> {
			SalaryRecord r = inv.getArgument(0);
			r.setId(100L);
			return r;
		});
		String importCsv = "年份,月份,基本工资,绩效工资,租房补助,伙食补助,交通补贴,加班费,加班补助,奖金,应发工资,"
				+ "医疗保险缴费基数,养老失业缴费基数,公积金缴费基数,医疗,养老,失业,公积金,扣除项合计,税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额,备注\n"
				+ "2061,年终奖,0,0,0,0,0,0,0,50000,50000,0,0,0,0,0,0,0,0,0,0,0,0,0,50000,年终奖\n";

		int count = service.importCsv(1L, importCsv);

		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("CSV 导入：空内容抛出业务异常")
	void importCsvEmptyThrows() {
		assertThatThrownBy(() -> service.importCsv(1L, ""))
			.isInstanceOf(com.tlcsdm.ecovault.common.BusinessException.class);
	}

	@Test
	@DisplayName("CSV 导入：仅有表头无数据行抛出业务异常")
	void importCsvHeaderOnlyThrows() {
		String headerOnly = "年份,月份,基本工资,备注";
		assertThatThrownBy(() -> service.importCsv(1L, headerOnly))
			.isInstanceOf(com.tlcsdm.ecovault.common.BusinessException.class);
	}

	@Test
	@DisplayName("CSV 导入：数据行列数不足抛出业务异常")
	void importCsvTooFewColumnsThrows() {
		String bad = "年份,月份,基本工资\n2025,1,10000";
		assertThatThrownBy(() -> service.importCsv(1L, bad))
			.isInstanceOf(com.tlcsdm.ecovault.common.BusinessException.class);
	}

	@Test
	@DisplayName("CSV 导入：数值格式错误时抛出业务异常")
	void importCsvMalformedNumberThrows() {
		String bad = "年份,月份,基本工资,绩效工资,租房补助,伙食补助,交通补贴,加班费,加班补助,奖金,应发工资,"
				+ "医疗保险缴费基数,养老失业缴费基数,公积金缴费基数,医疗,养老,失业,公积金,扣除项合计,税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额,备注\n"
				+ "2025,1,abc,0,0,0,0,0,0,0,10000,0,0,0,0,0,0,0,0,10000,0,10000,0,0,10000,格式错误\n";

		assertThatThrownBy(() -> service.importCsv(1L, bad))
			.isInstanceOf(com.tlcsdm.ecovault.common.BusinessException.class);
	}

}

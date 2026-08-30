package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.SalaryRequest;
import com.tlcsdm.ecovault.dto.SalaryResponse;
import com.tlcsdm.ecovault.dto.SalaryStatistics;
import com.tlcsdm.ecovault.entity.SalaryRecord;
import com.tlcsdm.ecovault.repository.SalaryRecordRepository;
import com.tlcsdm.ecovault.service.SalaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务 - 工资数据管理服务实现。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see SalaryService
 */
@Service
public class SalaryServiceImpl implements SalaryService {

	private final SalaryRecordRepository repository;

	/**
	 * 构造SalaryServiceImpl实例并注入所需依赖。
	 * @param repository repository参数。
	 */
	public SalaryServiceImpl(SalaryRecordRepository repository) {
		this.repository = repository;
	}

	/**
	 * 保存业务数据。
	 * @param userId 用户编号。
	 * @param request 请求参数对象。
	 * @return 保存后的业务结果。
	 */
	@Override
	@Transactional
	public SalaryResponse save(Long userId, SalaryRequest request) {
		SalaryRecord record = repository.findByUserIdAndYearAndMonth(userId, request.year(), request.month())
			.orElseGet(SalaryRecord::new);
		record.setUserId(userId);
		applyRequest(record, request);
		return toResponse(repository.save(record));
	}

	/**
	 * 更新已有业务数据。
	 * @param userId 用户编号。
	 * @param id 主键或记录编号。
	 * @param request 请求参数对象。
	 * @return 更新后的业务结果。
	 */
	@Override
	@Transactional
	public SalaryResponse update(Long userId, Long id, SalaryRequest request) {
		SalaryRecord record = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("工资记录不存在"));
		applyRequest(record, request);
		return toResponse(repository.save(record));
	}

	/**
	 * 删除指定业务数据。
	 * @param userId 用户编号。
	 * @param id 主键或记录编号。
	 */
	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		SalaryRecord record = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("工资记录不存在"));
		repository.delete(record);
	}

	/**
	 * 查询业务数据列表。
	 * @param userId 用户编号。
	 * @param startYear 起始年份。
	 * @param endYear 结束年份。
	 * @return 业务数据列表。
	 */
	@Override
	@Transactional(readOnly = true)
	public List<SalaryResponse> list(Long userId, Integer startYear, Integer endYear) {
		return query(userId, startYear, endYear).stream().map(this::toResponse).collect(Collectors.toList());
	}

	/**
	 * 统计并汇总业务数据。
	 * @param userId 用户编号。
	 * @param startYear 起始年份。
	 * @param endYear 结束年份。
	 * @return 统计结果。
	 */
	@Override
	@Transactional(readOnly = true)
	public SalaryStatistics statistics(Long userId, Integer startYear, Integer endYear) {
		List<SalaryRecord> all = query(userId, startYear, endYear);

		// 拆分当月工资与年终奖记录
		List<SalaryRecord> monthly = new ArrayList<>();
		BigDecimal totalAnnualBonus = BigDecimal.ZERO;
		for (SalaryRecord record : all) {
			if (record.isAnnualBonus()) {
				totalAnnualBonus = totalAnnualBonus.add(record.getNetPay());
			}
			else {
				monthly.add(record);
			}
		}

		BigDecimal totalNet = BigDecimal.ZERO;
		BigDecimal totalBonus = BigDecimal.ZERO;
		BigDecimal maxNet = null;
		BigDecimal minNet = null;

		BigDecimal cBase = BigDecimal.ZERO;
		BigDecimal cPerformance = BigDecimal.ZERO;
		BigDecimal cHousing = BigDecimal.ZERO;
		BigDecimal cMeal = BigDecimal.ZERO;
		BigDecimal cTransport = BigDecimal.ZERO;
		BigDecimal cOvertimePay = BigDecimal.ZERO;
		BigDecimal cOvertimeAllowance = BigDecimal.ZERO;

		BigDecimal dMedical = BigDecimal.ZERO;
		BigDecimal dPension = BigDecimal.ZERO;
		BigDecimal dUnemployment = BigDecimal.ZERO;
		BigDecimal dHousingFund = BigDecimal.ZERO;
		BigDecimal dIncomeTax = BigDecimal.ZERO;

		for (SalaryRecord record : monthly) {
			BigDecimal net = record.getNetPay();
			totalNet = totalNet.add(net);
			totalBonus = totalBonus.add(nz(record.getBonus()));
			if (maxNet == null || net.compareTo(maxNet) > 0) {
				maxNet = net;
			}
			if (minNet == null || net.compareTo(minNet) < 0) {
				minNet = net;
			}
			cBase = cBase.add(nz(record.getBaseSalary()));
			cPerformance = cPerformance.add(nz(record.getPerformanceSalary()));
			cHousing = cHousing.add(nz(record.getHousingAllowance()));
			cMeal = cMeal.add(nz(record.getMealAllowance()));
			cTransport = cTransport.add(nz(record.getTransportAllowance()));
			cOvertimePay = cOvertimePay.add(nz(record.getOvertimePay()));
			cOvertimeAllowance = cOvertimeAllowance.add(nz(record.getOvertimeAllowance()));
			dMedical = dMedical.add(nz(record.getMedicalDeduction()));
			dPension = dPension.add(nz(record.getPensionDeduction()));
			dUnemployment = dUnemployment.add(nz(record.getUnemploymentDeduction()));
			dHousingFund = dHousingFund.add(nz(record.getHousingFundDeduction()));
			dIncomeTax = dIncomeTax.add(nz(record.getIncomeTax()));
		}

		if (monthly.isEmpty()) {
			maxNet = BigDecimal.ZERO;
			minNet = BigDecimal.ZERO;
		}

		BigDecimal averageNet = monthly.isEmpty() ? BigDecimal.ZERO
				: totalNet.divide(BigDecimal.valueOf(monthly.size()), 2, RoundingMode.HALF_UP);

		List<SalaryStatistics.MonthlyPoint> trend = monthly.stream()
			.map(r -> new SalaryStatistics.MonthlyPoint(String.format("%04d-%02d", r.getYear(), r.getMonth()),
					r.getNetPay(), r.getGrossPay()))
			.collect(Collectors.toList());

		SalaryStatistics.Composition composition = new SalaryStatistics.Composition(cBase, cPerformance, cHousing,
				cMeal, cTransport, cOvertimePay, cOvertimeAllowance, totalBonus);
		SalaryStatistics.DeductionComposition deductionComposition = new SalaryStatistics.DeductionComposition(dMedical,
				dPension, dUnemployment, dHousingFund, dIncomeTax);

		return new SalaryStatistics(totalNet, averageNet, maxNet, minNet, totalBonus, totalAnnualBonus, trend,
				composition, deductionComposition);
	}

	/**
	 * 导出业务数据。
	 * @param userId 用户编号。
	 * @param startYear 起始年份。
	 * @param endYear 结束年份。
	 * @return 导出结果。
	 */
	@Override
	@Transactional(readOnly = true)
	public Map<String, String> exportCsv(Long userId, Integer startYear, Integer endYear) {
		List<SalaryRecord> records = query(userId, startYear, endYear);

		StringBuilder sb = new StringBuilder();
		// 加入 UTF-8 BOM，确保 Excel 正确识别中文
		sb.append('\uFEFF');
		sb.append("年份,月份,基本工资,绩效工资,租房补助,伙食补助,交通补贴,加班费,加班补助,奖金,应发工资,")
			.append("医疗保险缴费基数,养老失业缴费基数,公积金缴费基数,")
			.append("医疗,养老,失业,公积金,扣除项合计,税前工资,所得税,税后工资,大病医疗,采暖补贴,实发金额,备注\n");
		for (SalaryRecord r : records) {
			sb.append(r.getYear())
				.append(',')
				.append(monthLabel(r))
				.append(',')
				.append(nz(r.getBaseSalary()))
				.append(',')
				.append(nz(r.getPerformanceSalary()))
				.append(',')
				.append(nz(r.getHousingAllowance()))
				.append(',')
				.append(nz(r.getMealAllowance()))
				.append(',')
				.append(nz(r.getTransportAllowance()))
				.append(',')
				.append(nz(r.getOvertimePay()))
				.append(',')
				.append(nz(r.getOvertimeAllowance()))
				.append(',')
				.append(nz(r.getBonus()))
				.append(',')
				.append(r.getGrossPay())
				.append(',')
				.append(nz(r.getMedicalBase()))
				.append(',')
				.append(nz(r.getPensionUnemploymentBase()))
				.append(',')
				.append(nz(r.getHousingFundBase()))
				.append(',')
				.append(nz(r.getMedicalDeduction()))
				.append(',')
				.append(nz(r.getPensionDeduction()))
				.append(',')
				.append(nz(r.getUnemploymentDeduction()))
				.append(',')
				.append(nz(r.getHousingFundDeduction()))
				.append(',')
				.append(r.getTotalDeduction())
				.append(',')
				.append(r.getPreTaxSalary())
				.append(',')
				.append(nz(r.getIncomeTax()))
				.append(',')
				.append(r.getAfterTaxSalary())
				.append(',')
				.append(nz(r.getSeriousIllnessMedical()))
				.append(',')
				.append(nz(r.getHeatingAllowance()))
				.append(',')
				.append(r.getNetPay())
				.append(',')
				.append(escapeCsv(r.getRemark()))
				.append('\n');
		}

		String filename = buildExportFilename(startYear, endYear);
		Map<String, String> result = new HashMap<>();
		result.put("csv", sb.toString());
		result.put("filename", filename);
		return result;
	}

	/**
	 * 处理importCsv相关业务。
	 * @param userId 用户编号。
	 * @param csvContent csvContent参数。
	 * @return 方法执行结果。
	 */
	@Override
	@Transactional
	public int importCsv(Long userId, String csvContent) {
		if (csvContent == null || csvContent.isBlank()) {
			throw new BusinessException("CSV 内容为空");
		}
		// 去除 UTF-8 BOM
		String content = csvContent.startsWith("\uFEFF") ? csvContent.substring(1) : csvContent;
		String[] lines = content.split("\r?\n");
		if (lines.length < 2) {
			throw new BusinessException("CSV 至少需要表头行与一行数据");
		}

		int count = 0;
		// 从第 2 行（index=1）开始，跳过表头
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				continue;
			}
			String[] cols = parseCsvLine(line);
			if (cols.length < 25) {
				throw new BusinessException("第 " + (i + 1) + " 行列数不足，期望至少 25 列，实际 " + cols.length + " 列");
			}
			int year = parseIntCell(cols[0], i + 1, "年份");
			int month = "年终奖".equals(cols[1].trim()) ? SalaryRecord.ANNUAL_BONUS_MONTH
					: parseIntCell(cols[1], i + 1, "月份");

			// CSV 全量列映射（含派生列：cols[10]=应发工资, cols[18]=扣除项合计,
			// cols[19]=税前工资, cols[21]=税后工资，从 CSV 直接存入 DB）
			SalaryRequest req = new SalaryRequest(year, month, parseBd(cols[2]), // baseSalary
					parseBd(cols[3]), // performanceSalary
					parseBd(cols[4]), // housingAllowance
					parseBd(cols[5]), // mealAllowance
					parseBd(cols[6]), // transportAllowance
					parseBd(cols[7]), // overtimePay
					parseBd(cols[8]), // overtimeAllowance
					parseBd(cols[9]), // bonus
					parseBd(cols[11]), // medicalBase
					parseBd(cols[12]), // pensionUnemploymentBase
					parseBd(cols[13]), // housingFundBase
					parseBd(cols[14]), // medicalDeduction
					parseBd(cols[15]), // pensionDeduction
					parseBd(cols[16]), // unemploymentDeduction
					parseBd(cols[17]), // housingFundDeduction
					parseBd(cols[20]), // incomeTax
					parseBd(cols[22]), // seriousIllnessMedical
					parseBd(cols[23]), // heatingAllowance
					parseBd(cols[24]), // netPay
					cols.length > 25 ? unescapeCsv(cols[25]) : "");
			SalaryRecord record = repository.findByUserIdAndYearAndMonth(userId, year, month)
				.orElseGet(SalaryRecord::new);
			record.setUserId(userId);
			applyRequest(record, req);
			// 直接使用 CSV 中派生列数据，保留导入数据原值
			record.setStoredGrossPay(parseBd(cols[10]));
			record.setStoredTotalDeduction(parseBd(cols[18]));
			record.setStoredPreTaxSalary(parseBd(cols[19]));
			record.setStoredAfterTaxSalary(parseBd(cols[21]));
			repository.save(record);
			count++;
		}
		return count;
	}

	// ===== 私有辅助方法 =====

	private List<SalaryRecord> query(Long userId, Integer startYear, Integer endYear) {
		if (startYear == null && endYear == null) {
			return repository.findByUserIdOrderByYearAscMonthAsc(userId);
		}
		int sy = startYear != null ? startYear : endYear;
		int ey = endYear != null ? endYear : startYear;
		if (sy > ey) {
			int tmp = sy;
			sy = ey;
			ey = tmp;
		}
		if (sy == ey) {
			return repository.findByUserIdAndYearOrderByMonthAsc(userId, sy);
		}
		return repository.findByUserIdAndYearBetweenOrderByYearAscMonthAsc(userId, sy, ey);
	}

	private String buildExportFilename(Integer startYear, Integer endYear) {
		if (startYear == null && endYear == null) {
			return "salary_all.csv";
		}
		int sy = startYear != null ? startYear : endYear;
		int ey = endYear != null ? endYear : startYear;
		if (sy > ey) {
			int tmp = sy;
			sy = ey;
			ey = tmp;
		}
		return sy == ey ? "salary_" + sy + ".csv" : "salary_" + sy + "-" + ey + ".csv";
	}

	private String monthLabel(SalaryRecord record) {
		return record.isAnnualBonus() ? "年终奖" : String.valueOf(record.getMonth());
	}

	private void applyRequest(SalaryRecord record, SalaryRequest request) {
		record.setYear(request.year());
		record.setMonth(request.month());
		record.setBaseSalary(nz(request.baseSalary()));
		record.setPerformanceSalary(nz(request.performanceSalary()));
		record.setHousingAllowance(nz(request.housingAllowance()));
		record.setMealAllowance(nz(request.mealAllowance()));
		record.setTransportAllowance(nz(request.transportAllowance()));
		record.setOvertimePay(nz(request.overtimePay()));
		record.setOvertimeAllowance(nz(request.overtimeAllowance()));
		record.setBonus(nz(request.bonus()));
		record.setMedicalBase(nz(request.medicalBase()));
		record.setPensionUnemploymentBase(nz(request.pensionUnemploymentBase()));
		record.setHousingFundBase(nz(request.housingFundBase()));
		record.setMedicalDeduction(nz(request.medicalDeduction()));
		record.setPensionDeduction(nz(request.pensionDeduction()));
		record.setUnemploymentDeduction(nz(request.unemploymentDeduction()));
		record.setHousingFundDeduction(nz(request.housingFundDeduction()));
		record.setIncomeTax(nz(request.incomeTax()));
		record.setSeriousIllnessMedical(nz(request.seriousIllnessMedical()));
		record.setHeatingAllowance(nz(request.heatingAllowance()));
		record.setNetPay(nz(request.netPay()));
		record.setRemark(request.remark() == null ? "" : request.remark());
		// 手动录入/编辑时清除存储的派生值，让 getter 重新实时计算
		record.setStoredGrossPay(null);
		record.setStoredTotalDeduction(null);
		record.setStoredPreTaxSalary(null);
		record.setStoredAfterTaxSalary(null);
	}

	private BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String escapeCsv(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		String escaped = value.replace("\"", "\"\"");
		if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
			return "\"" + escaped + "\"";
		}
		return escaped;
	}

	private String unescapeCsv(String cell) {
		if (cell == null) {
			return "";
		}
		String s = cell.trim();
		if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
			s = s.substring(1, s.length() - 1).replace("\"\"", "\"");
		}
		return s;
	}

	/**
	 * 简单 CSV 行解析，支持双引号包围含逗号/换行的字段。
	 */
	private String[] parseCsvLine(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (inQuotes) {
				if (c == '"') {
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						sb.append('"');
						i++;
					}
					else {
						inQuotes = false;
					}
				}
				else {
					sb.append(c);
				}
			}
			else {
				if (c == '"') {
					inQuotes = true;
				}
				else if (c == ',') {
					fields.add(sb.toString());
					sb.setLength(0);
				}
				else {
					sb.append(c);
				}
			}
		}
		fields.add(sb.toString());
		return fields.toArray(new String[0]);
	}

	private int parseIntCell(String cell, int lineNum, String colName) {
		try {
			return Integer.parseInt(cell.trim());
		}
		catch (NumberFormatException e) {
			throw new BusinessException("第 " + lineNum + " 行" + colName + "格式错误: " + cell);
		}
	}

	private BigDecimal parseBd(String cell) {
		if (cell == null || cell.trim().isEmpty()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(cell.trim());
		}
		catch (NumberFormatException e) {
			throw new BusinessException("数值格式错误: \"" + cell.trim() + "\"，请确认 CSV 内容正确");
		}
	}

	private SalaryResponse toResponse(SalaryRecord record) {
		return new SalaryResponse(record.getId(), record.getYear(), record.getMonth(), record.isAnnualBonus(),
				record.getBaseSalary(), record.getPerformanceSalary(), record.getHousingAllowance(),
				record.getMealAllowance(), record.getTransportAllowance(), record.getOvertimePay(),
				record.getOvertimeAllowance(), record.getBonus(), record.getMedicalBase(),
				record.getPensionUnemploymentBase(), record.getHousingFundBase(), record.getMedicalDeduction(),
				record.getPensionDeduction(), record.getUnemploymentDeduction(), record.getHousingFundDeduction(),
				record.getIncomeTax(), record.getSeriousIllnessMedical(), record.getHeatingAllowance(),
				record.getGrossPay(), record.getTotalDeduction(), record.getPreTaxSalary(), record.getAfterTaxSalary(),
				record.getNetPay(), record.getRemark());
	}

}

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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务 - 工资数据管理服务实现。
 *
 * @author unknowIfGuestInDream
 */
@Service
public class SalaryServiceImpl implements SalaryService {

	private final SalaryRecordRepository repository;

	public SalaryServiceImpl(SalaryRecordRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public SalaryResponse save(Long userId, SalaryRequest request) {
		SalaryRecord record = repository.findByUserIdAndYearAndMonth(userId, request.year(), request.month())
			.orElseGet(SalaryRecord::new);
		record.setUserId(userId);
		applyRequest(record, request);
		return toResponse(repository.save(record));
	}

	@Override
	@Transactional
	public SalaryResponse update(Long userId, Long id, SalaryRequest request) {
		SalaryRecord record = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("工资记录不存在"));
		applyRequest(record, request);
		return toResponse(repository.save(record));
	}

	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		SalaryRecord record = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("工资记录不存在"));
		repository.delete(record);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SalaryResponse> list(Long userId, Integer year) {
		return query(userId, year).stream().map(this::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public SalaryStatistics statistics(Long userId, Integer year) {
		List<SalaryRecord> all = query(userId, year);

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

	@Override
	@Transactional(readOnly = true)
	public String exportCsv(Long userId, Integer year) {
		List<SalaryRecord> records = query(userId, year);

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
		return sb.toString();
	}

	private List<SalaryRecord> query(Long userId, Integer year) {
		return year == null ? repository.findByUserIdOrderByYearAscMonthAsc(userId)
				: repository.findByUserIdAndYearOrderByMonthAsc(userId, year);
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
		record.setRemark(request.remark());
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

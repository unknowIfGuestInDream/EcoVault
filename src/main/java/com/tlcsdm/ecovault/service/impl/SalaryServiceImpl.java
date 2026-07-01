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
		List<SalaryRecord> records = year == null ? repository.findByUserIdOrderByYearAscMonthAsc(userId)
				: repository.findByUserIdAndYearOrderByMonthAsc(userId, year);
		return records.stream().map(this::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public SalaryStatistics statistics(Long userId, Integer year) {
		List<SalaryRecord> records = year == null ? repository.findByUserIdOrderByYearAscMonthAsc(userId)
				: repository.findByUserIdAndYearOrderByMonthAsc(userId, year);

		if (records.isEmpty()) {
			BigDecimal zero = BigDecimal.ZERO;
			return new SalaryStatistics(zero, zero, zero, zero, zero, List.of(),
					new SalaryStatistics.Composition(zero, zero, zero));
		}

		BigDecimal totalNet = BigDecimal.ZERO;
		BigDecimal totalBonus = BigDecimal.ZERO;
		BigDecimal totalBase = BigDecimal.ZERO;
		BigDecimal totalAllowance = BigDecimal.ZERO;
		BigDecimal maxNet = records.get(0).getNet();
		BigDecimal minNet = records.get(0).getNet();

		for (SalaryRecord record : records) {
			BigDecimal net = record.getNet();
			totalNet = totalNet.add(net);
			totalBonus = totalBonus.add(nz(record.getBonus()));
			totalBase = totalBase.add(nz(record.getBaseSalary()));
			totalAllowance = totalAllowance.add(nz(record.getAllowance()));
			if (net.compareTo(maxNet) > 0) {
				maxNet = net;
			}
			if (net.compareTo(minNet) < 0) {
				minNet = net;
			}
		}

		BigDecimal averageNet = totalNet.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);

		List<SalaryStatistics.MonthlyPoint> trend = records.stream()
			.map(r -> new SalaryStatistics.MonthlyPoint(String.format("%04d-%02d", r.getYear(), r.getMonth()),
					r.getNet(), r.getGross()))
			.collect(Collectors.toList());

		return new SalaryStatistics(totalNet, averageNet, maxNet, minNet, totalBonus, trend,
				new SalaryStatistics.Composition(totalBase, totalBonus, totalAllowance));
	}

	@Override
	@Transactional(readOnly = true)
	public String exportCsv(Long userId, Integer year) {
		List<SalaryRecord> records = year == null ? repository.findByUserIdOrderByYearAscMonthAsc(userId)
				: repository.findByUserIdAndYearOrderByMonthAsc(userId, year);

		StringBuilder sb = new StringBuilder();
		// 加入 UTF-8 BOM，确保 Excel 正确识别中文
		sb.append('\uFEFF');
		sb.append("年份,月份,基本工资,奖金,补贴,扣款,税前总额,实发金额,备注\n");
		for (SalaryRecord r : records) {
			sb.append(r.getYear())
				.append(',')
				.append(r.getMonth())
				.append(',')
				.append(nz(r.getBaseSalary()))
				.append(',')
				.append(nz(r.getBonus()))
				.append(',')
				.append(nz(r.getAllowance()))
				.append(',')
				.append(nz(r.getDeduction()))
				.append(',')
				.append(r.getGross())
				.append(',')
				.append(r.getNet())
				.append(',')
				.append(escapeCsv(r.getRemark()))
				.append('\n');
		}
		return sb.toString();
	}

	private void applyRequest(SalaryRecord record, SalaryRequest request) {
		record.setYear(request.year());
		record.setMonth(request.month());
		record.setBaseSalary(nz(request.baseSalary()));
		record.setBonus(nz(request.bonus()));
		record.setAllowance(nz(request.allowance()));
		record.setDeduction(nz(request.deduction()));
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
		return new SalaryResponse(record.getId(), record.getYear(), record.getMonth(), record.getBaseSalary(),
				record.getBonus(), record.getAllowance(), record.getDeduction(), record.getGross(), record.getNet(),
				record.getRemark());
	}

}

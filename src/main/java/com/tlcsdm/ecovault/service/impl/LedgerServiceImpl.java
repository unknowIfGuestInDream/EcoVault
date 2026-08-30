package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.LedgerRequest;
import com.tlcsdm.ecovault.dto.LedgerResponse;
import com.tlcsdm.ecovault.dto.LedgerStatistics;
import com.tlcsdm.ecovault.entity.LedgerEntry;
import com.tlcsdm.ecovault.entity.LedgerType;
import com.tlcsdm.ecovault.repository.LedgerEntryRepository;
import com.tlcsdm.ecovault.service.LedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 财务 - 收入支出管理服务实现。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see LedgerService
 */
@Service
/**
 * 收支管理服务实现类，负责处理收支记录维护、统计和导出逻辑。 面向个人财务流水场景提供完整业务支撑。
 *
 * @author unknowIfGuestInDream
 * @since 1.0.0
 * @see LedgerService
 */
public class LedgerServiceImpl implements LedgerService {

	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final LedgerEntryRepository repository;

	/**
	 * 构造LedgerServiceImpl实例并注入所需依赖。
	 * @param repository repository参数。
	 */
	public LedgerServiceImpl(LedgerEntryRepository repository) {
		this.repository = repository;
	}

	/**
	 * 创建新的业务数据。
	 * @param userId 用户编号。
	 * @param request 请求参数对象。
	 * @return 保存后的业务结果。
	 */
	@Override
	@Transactional
	public LedgerResponse create(Long userId, LedgerRequest request) {
		LedgerEntry entry = new LedgerEntry();
		entry.setUserId(userId);
		applyRequest(entry, request);
		return toResponse(repository.save(entry));
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
	public LedgerResponse update(Long userId, Long id, LedgerRequest request) {
		LedgerEntry entry = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("收支记录不存在"));
		applyRequest(entry, request);
		return toResponse(repository.save(entry));
	}

	/**
	 * 删除指定业务数据。
	 * @param userId 用户编号。
	 * @param id 主键或记录编号。
	 */
	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		LedgerEntry entry = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("收支记录不存在"));
		repository.delete(entry);
	}

	/**
	 * 查询业务数据列表。
	 * @param userId 用户编号。
	 * @param type 类型条件。
	 * @param start 起始日期。
	 * @param end 结束日期。
	 * @param tag 标签条件。
	 * @return 业务数据列表。
	 */
	@Override
	@Transactional(readOnly = true)
	public List<LedgerResponse> list(Long userId, String type, LocalDate start, LocalDate end, String tag) {
		return query(userId, type, start, end, tag).stream().map(this::toResponse).collect(Collectors.toList());
	}

	/**
	 * 统计并汇总业务数据。
	 * @param userId 用户编号。
	 * @param type 类型条件。
	 * @param start 起始日期。
	 * @param end 结束日期。
	 * @param tag 标签条件。
	 * @return 统计结果。
	 */
	@Override
	@Transactional(readOnly = true)
	public LedgerStatistics statistics(Long userId, String type, LocalDate start, LocalDate end, String tag) {
		List<LedgerEntry> entries = query(userId, type, start, end, tag);

		BigDecimal totalIncome = BigDecimal.ZERO;
		BigDecimal totalExpense = BigDecimal.ZERO;
		Map<String, BigDecimal> incomeByTag = new LinkedHashMap<>();
		Map<String, BigDecimal> expenseByTag = new LinkedHashMap<>();
		Map<String, BigDecimal[]> monthly = new TreeMap<>();

		for (LedgerEntry entry : entries) {
			BigDecimal amount = nz(entry.getAmount());
			boolean income = entry.getType() == LedgerType.INCOME;
			if (income) {
				totalIncome = totalIncome.add(amount);
			}
			else {
				totalExpense = totalExpense.add(amount);
			}

			accumulateTags(income ? incomeByTag : expenseByTag, entry.getTags(), amount);

			String month = entry.getEntryDate() == null ? "" : entry.getEntryDate().format(MONTH_FORMATTER);
			BigDecimal[] point = monthly.computeIfAbsent(month,
					key -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
			if (income) {
				point[0] = point[0].add(amount);
			}
			else {
				point[1] = point[1].add(amount);
			}
		}

		List<LedgerStatistics.MonthlyPoint> trend = monthly.entrySet()
			.stream()
			.map(e -> new LedgerStatistics.MonthlyPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
			.collect(Collectors.toList());

		return new LedgerStatistics(totalIncome, totalExpense, totalIncome.subtract(totalExpense), entries.size(),
				toTagAmounts(incomeByTag), toTagAmounts(expenseByTag), trend);
	}

	/**
	 * 导出业务数据。
	 * @param userId 用户编号。
	 * @param type 类型条件。
	 * @param start 起始日期。
	 * @param end 结束日期。
	 * @param tag 标签条件。
	 * @return 导出结果。
	 */
	@Override
	@Transactional(readOnly = true)
	public String exportCsv(Long userId, String type, LocalDate start, LocalDate end, String tag) {
		List<LedgerEntry> entries = query(userId, type, start, end, tag);
		StringBuilder sb = new StringBuilder();
		// 加入 UTF-8 BOM，确保 Excel 正确识别中文
		sb.append('\uFEFF');
		sb.append("日期,类型,金额,标签,备注\n");
		for (LedgerEntry entry : entries) {
			sb.append(entry.getEntryDate() == null ? "" : entry.getEntryDate())
				.append(',')
				.append(entry.getType() == LedgerType.INCOME ? "收入" : "支出")
				.append(',')
				.append(nz(entry.getAmount()))
				.append(',')
				.append(escapeCsv(String.join("|", sortedTags(entry.getTags()))))
				.append(',')
				.append(escapeCsv(entry.getRemark()))
				.append('\n');
		}
		return sb.toString();
	}

	private List<LedgerEntry> query(Long userId, String type, LocalDate start, LocalDate end, String tag) {
		LedgerType ledgerType = parseType(type);
		String normalizedTag = (tag == null || tag.isBlank()) ? null : tag.trim();
		if (start != null && end != null && start.isAfter(end)) {
			throw new BusinessException("起始日期不能晚于结束日期");
		}
		return repository.search(userId, ledgerType, start, end, normalizedTag);
	}

	private void applyRequest(LedgerEntry entry, LedgerRequest request) {
		entry.setType(parseRequiredType(request.type()));
		entry.setAmount(nz(request.amount()));
		entry.setEntryDate(request.entryDate());
		entry.setTags(normalizeTags(request.tags()));
		entry.setRemark(request.remark());
	}

	private Set<String> normalizeTags(List<String> tags) {
		Set<String> result = new LinkedHashSet<>();
		if (tags != null) {
			for (String tag : tags) {
				if (tag != null && !tag.isBlank()) {
					result.add(tag.trim());
				}
			}
		}
		return result;
	}

	private List<String> sortedTags(Set<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(tags);
	}

	private void accumulateTags(Map<String, BigDecimal> target, Set<String> tags, BigDecimal amount) {
		if (tags == null || tags.isEmpty()) {
			target.merge("未分类", amount, BigDecimal::add);
			return;
		}
		for (String tag : tags) {
			target.merge(tag, amount, BigDecimal::add);
		}
	}

	private List<LedgerStatistics.TagAmount> toTagAmounts(Map<String, BigDecimal> map) {
		return map.entrySet()
			.stream()
			.map(e -> new LedgerStatistics.TagAmount(e.getKey(), e.getValue()))
			.collect(Collectors.toList());
	}

	private LedgerType parseRequiredType(String type) {
		LedgerType parsed = parseType(type);
		if (parsed == null) {
			throw new BusinessException("收支类型不合法");
		}
		return parsed;
	}

	private LedgerType parseType(String type) {
		if (type == null || type.isBlank()) {
			return null;
		}
		try {
			return LedgerType.valueOf(type.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("收支类型不合法");
		}
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

	private LedgerResponse toResponse(LedgerEntry entry) {
		return new LedgerResponse(entry.getId(), entry.getType() == null ? null : entry.getType().name(),
				entry.getAmount(), entry.getEntryDate(), new ArrayList<>(entry.getTags()), entry.getRemark());
	}

}

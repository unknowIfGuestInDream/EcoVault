package com.tlcsdm.ecovault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 启动时重置旧版工资表结构。
 *
 * <p>
 * 当前项目仅支持最新的分类工资表结构。若本地 SQLite 仍残留旧版 {@code allowance}/{@code deduction} 列， SQLite 在
 * {@code ddl-auto=update} 下不会自动删除旧列，后续录入工资或年终奖时会继续命中旧列的 {@code NOT NULL}
 * 约束。检测到该旧结构后，系统会直接按最新结构重建 {@code salary_records} 表。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Component
public class SalaryRecordSchemaReset implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SalaryRecordSchemaReset.class);

	private static final String TABLE_NAME = "salary_records";

	private static final RowMapper<String> COLUMN_NAME_MAPPER = (rs, rowNum) -> rs.getString("name");

	private final JdbcTemplate jdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	@Autowired
	public SalaryRecordSchemaReset(DataSource dataSource) {
		this(new JdbcTemplate(dataSource), new TransactionTemplate(new JdbcTransactionManager(dataSource)));
	}

	SalaryRecordSchemaReset(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		resetLegacySchemaIfNeeded();
	}

	void resetLegacySchemaIfNeeded() {
		Set<String> columns = loadColumns();
		if (!needsReset(columns)) {
			return;
		}
		transactionTemplate.executeWithoutResult(status -> recreateLatestTable());
	}

	private Set<String> loadColumns() {
		List<String> columns = jdbcTemplate.query("PRAGMA table_info(salary_records)", COLUMN_NAME_MAPPER);
		return new LinkedHashSet<>(columns);
	}

	private boolean needsReset(Set<String> columns) {
		return !columns.isEmpty() && (columns.contains("allowance") || columns.contains("deduction"));
	}

	private void recreateLatestTable() {
		Set<String> currentColumns = loadColumns();
		if (!needsReset(currentColumns)) {
			return;
		}
		jdbcTemplate.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
		jdbcTemplate.execute("""
				CREATE TABLE salary_records (
					id INTEGER PRIMARY KEY AUTOINCREMENT,
					user_id INTEGER NOT NULL,
					year INTEGER NOT NULL,
					month INTEGER NOT NULL,
					base_salary NUMERIC(12,2) NOT NULL,
					performance_salary NUMERIC(12,2) NOT NULL,
					housing_allowance NUMERIC(12,2) NOT NULL,
					meal_allowance NUMERIC(12,2) NOT NULL,
					transport_allowance NUMERIC(12,2) NOT NULL,
					overtime_pay NUMERIC(12,2) NOT NULL,
					overtime_allowance NUMERIC(12,2) NOT NULL,
					bonus NUMERIC(12,2) NOT NULL,
					medical_base NUMERIC(12,2) NOT NULL,
					pension_unemployment_base NUMERIC(12,2) NOT NULL,
					housing_fund_base NUMERIC(12,2) NOT NULL,
					medical_deduction NUMERIC(12,2) NOT NULL,
					pension_deduction NUMERIC(12,2) NOT NULL,
					unemployment_deduction NUMERIC(12,2) NOT NULL,
					housing_fund_deduction NUMERIC(12,2) NOT NULL,
					income_tax NUMERIC(12,2) NOT NULL,
					serious_illness_medical NUMERIC(12,2) NOT NULL,
					heating_allowance NUMERIC(12,2) NOT NULL,
					remark VARCHAR(256),
					created_at DATETIME NOT NULL,
					updated_at DATETIME NOT NULL,
					CONSTRAINT uk_salary_user_ym UNIQUE (user_id, year, month)
				)
				""");
		jdbcTemplate.execute("CREATE INDEX idx_salary_user ON salary_records (user_id)");
		jdbcTemplate.execute("CREATE INDEX idx_salary_ym ON salary_records (year, month)");
		log.warn("检测到旧版 salary_records 表结构，已重建为最新分类工资表；旧工资数据已清空");
	}

}

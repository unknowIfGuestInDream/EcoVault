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
import java.util.stream.Collectors;

/**
 * 兼容旧版工资表结构的启动迁移。
 *
 * <p>
 * 旧版 {@code salary_records} 仅包含 {@code allowance}/{@code deduction} 两个聚合字段，升级到分类工资模型后，
 * SQLite 在 {@code ddl-auto=update} 下不会删除旧列，导致插入新记录时命中旧列的 {@code NOT NULL} 约束。此迁移会在启动时
 * 检测旧列并重建表结构，将旧数据映射到新版字段，确保历史数据可继续使用。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Component
public class SalaryRecordSchemaMigration implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SalaryRecordSchemaMigration.class);

	private static final String TABLE_NAME = "salary_records";

	private static final String LEGACY_TABLE_NAME = "salary_records_legacy";

	private static final RowMapper<String> COLUMN_NAME_MAPPER = (rs, rowNum) -> rs.getString("name");

	private final JdbcTemplate jdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	@Autowired
	public SalaryRecordSchemaMigration(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
	}

	SalaryRecordSchemaMigration(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		migrateLegacySchemaIfNeeded();
	}

	void migrateLegacySchemaIfNeeded() {
		Set<String> columns = loadColumns(TABLE_NAME);
		if (!needsMigration(columns)) {
			return;
		}
		transactionTemplate.executeWithoutResult(status -> migrate(columns));
	}

	private void migrate(Set<String> originalColumns) {
		Set<String> currentColumns = loadColumns(TABLE_NAME);
		if (!needsMigration(currentColumns)) {
			return;
		}
		jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " RENAME TO " + LEGACY_TABLE_NAME);
		createCurrentTable();
		copyLegacyData(originalColumns);
		jdbcTemplate.execute("DROP TABLE " + LEGACY_TABLE_NAME);
		log.info("已完成 salary_records 旧版结构迁移，兼容分类工资录入与年终奖录入");
	}

	private boolean needsMigration(Set<String> columns) {
		return !columns.isEmpty() && (columns.contains("allowance") || columns.contains("deduction"));
	}

	private Set<String> loadColumns(String tableName) {
		String sql = switch (tableName) {
			case TABLE_NAME -> "PRAGMA table_info(salary_records)";
			case LEGACY_TABLE_NAME -> "PRAGMA table_info(salary_records_legacy)";
			default -> throw new IllegalArgumentException("不支持读取该表结构: " + tableName);
		};
		List<String> columns = jdbcTemplate.query(sql, COLUMN_NAME_MAPPER);
		return new LinkedHashSet<>(columns);
	}

	private void createCurrentTable() {
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
	}

	private void copyLegacyData(Set<String> originalColumns) {
		List<String> selectColumns = List.of(columnExpr(originalColumns, "rowid", "id", "rowid"),
				columnExpr(originalColumns, "0", "user_id"), columnExpr(originalColumns, "0", "year"),
				columnExpr(originalColumns, "0", "month"), columnExpr(originalColumns, "0", "base_salary"),
				columnExpr(originalColumns, "0", "performance_salary"),
				columnExpr(originalColumns, "0", "housing_allowance", "allowance"),
				columnExpr(originalColumns, "0", "meal_allowance"),
				columnExpr(originalColumns, "0", "transport_allowance"),
				columnExpr(originalColumns, "0", "overtime_pay"),
				columnExpr(originalColumns, "0", "overtime_allowance"), columnExpr(originalColumns, "0", "bonus"),
				columnExpr(originalColumns, "0", "medical_base"),
				columnExpr(originalColumns, "0", "pension_unemployment_base"),
				columnExpr(originalColumns, "0", "housing_fund_base"),
				columnExpr(originalColumns, "0", "medical_deduction", "deduction"),
				columnExpr(originalColumns, "0", "pension_deduction"),
				columnExpr(originalColumns, "0", "unemployment_deduction"),
				columnExpr(originalColumns, "0", "housing_fund_deduction"),
				columnExpr(originalColumns, "0", "income_tax"),
				columnExpr(originalColumns, "0", "serious_illness_medical"),
				columnExpr(originalColumns, "0", "heating_allowance"), columnExpr(originalColumns, "NULL", "remark"),
				columnExpr(originalColumns, "CURRENT_TIMESTAMP", "created_at"),
				columnExpr(originalColumns, "CURRENT_TIMESTAMP", "updated_at"));
		jdbcTemplate.execute("""
				INSERT INTO salary_records (
					id, user_id, year, month, base_salary, performance_salary, housing_allowance,
					meal_allowance, transport_allowance, overtime_pay, overtime_allowance, bonus,
					medical_base, pension_unemployment_base, housing_fund_base, medical_deduction,
					pension_deduction, unemployment_deduction, housing_fund_deduction, income_tax,
					serious_illness_medical, heating_allowance, remark, created_at, updated_at
				)
				SELECT %s
				FROM %s
				""".formatted(String.join(", ", selectColumns), LEGACY_TABLE_NAME));
	}

	/**
	 * 旧表经 Hibernate 自动升级后，部分新列可能"存在但值全为 NULL"；此时仍需回退到旧列或默认值， 避免迁移后命中新表的 NOT NULL 约束。
	 */
	private String columnExpr(Set<String> columns, String defaultValue, String... candidates) {
		List<String> existingColumns = List.of(candidates)
			.stream()
			.filter(columns::contains)
			.collect(Collectors.toList());
		if (existingColumns.isEmpty()) {
			return defaultValue;
		}
		return "COALESCE(" + String.join(", ", existingColumns) + ", " + defaultValue + ")";
	}

}

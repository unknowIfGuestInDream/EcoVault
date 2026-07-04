package com.tlcsdm.ecovault.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SalaryRecordSchemaMigration} 测试。
 *
 * @author unknowIfGuestInDream
 */
class SalaryRecordSchemaMigrationTest {

	@Test
	@DisplayName("旧版 salary_records 表启动后迁移为分类工资结构")
	void migrateLegacySalarySchema() throws Exception {
		Path dbPath = Files.createTempFile("salary-migration-", ".db");
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:" + dbPath);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
				CREATE TABLE salary_records (
					id INTEGER PRIMARY KEY AUTOINCREMENT,
					user_id INTEGER NOT NULL,
					year INTEGER NOT NULL,
					month INTEGER NOT NULL,
					base_salary NUMERIC(12,2) NOT NULL,
					bonus NUMERIC(12,2) NOT NULL,
					allowance NUMERIC(12,2) NOT NULL,
					deduction NUMERIC(12,2) NOT NULL,
					remark VARCHAR(256),
					created_at DATETIME NOT NULL,
					updated_at DATETIME NOT NULL,
					CONSTRAINT uk_salary_user_ym UNIQUE (user_id, year, month)
				)
				""");
		jdbcTemplate.update("""
				INSERT INTO salary_records
					(id, user_id, year, month, base_salary, bonus, allowance, deduction, remark, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", 1L, 7L, 2025, 12, "12000.00", "800.00", "500.00", "1500.00", "旧备注", "2026-01-01 00:00:00",
				"2026-01-02 00:00:00");

		SalaryRecordSchemaMigration migration = new SalaryRecordSchemaMigration(jdbcTemplate,
				new TransactionTemplate(new JdbcTransactionManager(dataSource)));

		migration.migrateLegacySchemaIfNeeded();

		List<String> columns = jdbcTemplate.query("PRAGMA table_info(salary_records)",
				(rs, rowNum) -> rs.getString("name"));
		assertThat(columns).contains("housing_allowance", "medical_deduction", "income_tax");
		assertThat(columns).doesNotContain("allowance", "deduction");

		var row = jdbcTemplate.queryForMap("""
				SELECT user_id, year, month, base_salary, bonus, housing_allowance, medical_deduction, remark
				FROM salary_records WHERE id = 1
				""");
		assertThat(((Number) row.get("user_id")).longValue()).isEqualTo(7L);
		assertThat(((Number) row.get("year")).intValue()).isEqualTo(2025);
		assertThat(((Number) row.get("month")).intValue()).isEqualTo(12);
		assertThat(new BigDecimal(row.get("base_salary").toString())).isEqualByComparingTo("12000");
		assertThat(new BigDecimal(row.get("bonus").toString())).isEqualByComparingTo("800");
		assertThat(new BigDecimal(row.get("housing_allowance").toString())).isEqualByComparingTo("500");
		assertThat(new BigDecimal(row.get("medical_deduction").toString())).isEqualByComparingTo("1500");
		assertThat(row.get("remark")).isEqualTo("旧备注");
	}

	@Test
	@DisplayName("新版 salary_records 表无需重复迁移")
	void skipWhenSchemaAlreadyCurrent() throws Exception {
		Path dbPath = Files.createTempFile("salary-current-", ".db");
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:" + dbPath);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
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
					updated_at DATETIME NOT NULL
				)
				""");

		SalaryRecordSchemaMigration migration = new SalaryRecordSchemaMigration(jdbcTemplate,
				new TransactionTemplate(new JdbcTransactionManager(dataSource)));

		migration.migrateLegacySchemaIfNeeded();

		List<String> tables = jdbcTemplate.query("SELECT name FROM sqlite_master WHERE type='table'",
				(rs, rowNum) -> rs.getString(1));
		assertThat(tables).contains("salary_records").doesNotContain("salary_records_legacy");
	}

}

package com.tlcsdm.ecovault.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link SalaryRecordSchemaReset} 测试。
 *
 * @author unknowIfGuestInDream
 */
class SalaryRecordSchemaResetTest {

	@Test
	@DisplayName("旧版 salary_records 表会被重建为最新分类工资结构并恢复录入能力")
	void resetLegacySalarySchema() throws Exception {
		JdbcTemplate jdbcTemplate = jdbcTemplate("salary-reset-");
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
					updated_at DATETIME NOT NULL
				)
				""");
		jdbcTemplate.update("""
				INSERT INTO salary_records
					(user_id, year, month, base_salary, bonus, allowance, deduction, remark, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", 7L, 2025, 12, "12000.00", "800.00", "500.00", "1500.00", "旧备注", "2026-01-01 00:00:00",
				"2026-01-02 00:00:00");

		new SalaryRecordSchemaReset(jdbcTemplate, transactionTemplate(jdbcTemplate)).resetLegacySchemaIfNeeded();

		List<String> columns = jdbcTemplate.query("PRAGMA table_info(salary_records)",
				(rs, rowNum) -> rs.getString("name"));
		assertThat(columns).contains("housing_allowance", "medical_deduction", "income_tax");
		assertThat(columns).doesNotContain("allowance", "deduction");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_records", Integer.class)).isZero();

		assertThatCode(() -> jdbcTemplate.update(
				"""
						INSERT INTO salary_records (
							user_id, year, month, base_salary, performance_salary, housing_allowance,
							meal_allowance, transport_allowance, overtime_pay, overtime_allowance, bonus,
							medical_base, pension_unemployment_base, housing_fund_base, medical_deduction,
							pension_deduction, unemployment_deduction, housing_fund_deduction, income_tax,
							serious_illness_medical, heating_allowance, remark, created_at, updated_at
						) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
						""",
				7L, 2025, 0, "0", "0", "0", "0", "0", "0", "50000", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"年终奖"))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("新版 salary_records 表无需重建并保留现有数据")
	void skipWhenSchemaAlreadyCurrent() throws Exception {
		JdbcTemplate jdbcTemplate = jdbcTemplate("salary-current-");
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
		jdbcTemplate.execute("""
				INSERT INTO salary_records (
					user_id, year, month, base_salary, performance_salary, housing_allowance,
					meal_allowance, transport_allowance, overtime_pay, overtime_allowance, bonus,
					medical_base, pension_unemployment_base, housing_fund_base, medical_deduction,
					pension_deduction, unemployment_deduction, housing_fund_deduction, income_tax,
					serious_illness_medical, heating_allowance, remark, created_at, updated_at
				) VALUES (
					9, 2025, 1, 12000, 0, 500,
					0, 0, 0, 0, 800,
					0, 0, 0, 300,
					0, 0, 0, 0,
					0, 0, '当前数据', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
				)
				""");

		new SalaryRecordSchemaReset(jdbcTemplate, transactionTemplate(jdbcTemplate)).resetLegacySchemaIfNeeded();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_records", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT remark FROM salary_records WHERE user_id = 9", String.class))
			.isEqualTo("当前数据");
	}

	@Test
	@DisplayName("salary_records 表不存在时跳过重建")
	void skipWhenTableMissing() throws Exception {
		JdbcTemplate jdbcTemplate = jdbcTemplate("salary-missing-");

		assertThatCode(() -> new SalaryRecordSchemaReset(jdbcTemplate, transactionTemplate(jdbcTemplate))
			.resetLegacySchemaIfNeeded()).doesNotThrowAnyException();
		assertThat(jdbcTemplate.queryForList("SELECT name FROM sqlite_master WHERE type='table'", String.class))
			.isEmpty();
	}

	private JdbcTemplate jdbcTemplate(String prefix) throws Exception {
		Path dbPath = Files.createTempFile(prefix, ".db");
		dbPath.toFile().deleteOnExit();
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:" + dbPath);
		return new JdbcTemplate(dataSource);
	}

	private TransactionTemplate transactionTemplate(JdbcTemplate jdbcTemplate) {
		return new TransactionTemplate(new JdbcTransactionManager(jdbcTemplate.getDataSource()));
	}

}

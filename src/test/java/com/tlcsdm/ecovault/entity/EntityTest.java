package com.tlcsdm.ecovault.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实体类单元测试。
 *
 * <p>
 * 校验各实体的读写访问器、生命周期回调 (prePersist/preUpdate) 与派生计算 (税前/实发)。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class EntityTest {

	@Test
	@DisplayName("User 读写与生命周期回调正确")
	void userAccessorsAndLifecycle() {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");
		user.setPassword("secret");
		user.setNickname("Alice");
		user.setEmail("a@b.com");
		user.setRole(Role.ADMIN);
		user.setEnabled(false);
		LocalDateTime created = LocalDateTime.now().minusDays(1);
		user.setCreatedAt(created);
		user.setUpdatedAt(created);

		assertThat(user.getId()).isEqualTo(1L);
		assertThat(user.getUsername()).isEqualTo("alice");
		assertThat(user.getPassword()).isEqualTo("secret");
		assertThat(user.getNickname()).isEqualTo("Alice");
		assertThat(user.getEmail()).isEqualTo("a@b.com");
		assertThat(user.getRole()).isEqualTo(Role.ADMIN);
		assertThat(user.isEnabled()).isFalse();
		assertThat(user.getCreatedAt()).isEqualTo(created);
		assertThat(user.getUpdatedAt()).isEqualTo(created);

		user.prePersist();
		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();
		LocalDateTime afterPersist = user.getUpdatedAt();
		user.preUpdate();
		assertThat(user.getUpdatedAt()).isAfterOrEqualTo(afterPersist);
	}

	@Test
	@DisplayName("Role 枚举包含 USER 与 ADMIN")
	void roleEnum() {
		assertThat(Role.valueOf("USER")).isEqualTo(Role.USER);
		assertThat(Role.values()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
	}

	@Test
	@DisplayName("OperationLog 读写与 prePersist 正确")
	void operationLogAccessors() {
		OperationLog log = new OperationLog();
		log.setId(2L);
		log.setUserId(3L);
		log.setUsername("bob");
		log.setModule("用户管理");
		log.setOperation("登录");
		log.setMethod("AuthController.login");
		log.setParams("LoginRequest");
		log.setIp("127.0.0.1");
		log.setStatus("SUCCESS");
		log.setErrorMsg(null);
		log.setDurationMs(120L);
		LocalDateTime t = LocalDateTime.now();
		log.setCreatedAt(t);

		assertThat(log.getId()).isEqualTo(2L);
		assertThat(log.getUserId()).isEqualTo(3L);
		assertThat(log.getUsername()).isEqualTo("bob");
		assertThat(log.getModule()).isEqualTo("用户管理");
		assertThat(log.getOperation()).isEqualTo("登录");
		assertThat(log.getMethod()).isEqualTo("AuthController.login");
		assertThat(log.getParams()).isEqualTo("LoginRequest");
		assertThat(log.getIp()).isEqualTo("127.0.0.1");
		assertThat(log.getStatus()).isEqualTo("SUCCESS");
		assertThat(log.getErrorMsg()).isNull();
		assertThat(log.getDurationMs()).isEqualTo(120L);
		assertThat(log.getCreatedAt()).isEqualTo(t);

		log.prePersist();
		assertThat(log.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("UserSession 读写与 prePersist 初始化时间戳")
	void userSessionAccessors() {
		UserSession session = new UserSession();
		session.setId(4L);
		session.setUserId(5L);
		session.setJti("jti-123");
		session.setDeviceInfo("Chrome");
		session.setIp("10.0.0.1");
		session.setActive(true);
		LocalDateTime t = LocalDateTime.now();
		session.setCreatedAt(t);
		session.setLastActiveAt(t);

		assertThat(session.getId()).isEqualTo(4L);
		assertThat(session.getUserId()).isEqualTo(5L);
		assertThat(session.getJti()).isEqualTo("jti-123");
		assertThat(session.getDeviceInfo()).isEqualTo("Chrome");
		assertThat(session.getIp()).isEqualTo("10.0.0.1");
		assertThat(session.isActive()).isTrue();
		assertThat(session.getCreatedAt()).isEqualTo(t);
		assertThat(session.getLastActiveAt()).isEqualTo(t);

		UserSession fresh = new UserSession();
		fresh.prePersist();
		assertThat(fresh.getCreatedAt()).isNotNull();
		assertThat(fresh.getLastActiveAt()).isNotNull();
	}

	@Test
	@DisplayName("PasswordEntry 读写与生命周期回调正确")
	void passwordEntryAccessors() {
		PasswordEntry entry = new PasswordEntry();
		entry.setId(6L);
		entry.setUserId(7L);
		entry.setTitle("GitHub");
		entry.setAccount("alice");
		entry.setSecret("cipher");
		entry.setUrl("https://github.com");
		entry.setNotes("notes-cipher");
		entry.setCategory("开发");
		entry.setTags("tag-cipher");
		entry.setStrengthScore(80);
		entry.setStrengthLevel("STRONG");
		LocalDateTime t = LocalDateTime.now();
		entry.setCreatedAt(t);
		entry.setUpdatedAt(t);

		assertThat(entry.getId()).isEqualTo(6L);
		assertThat(entry.getUserId()).isEqualTo(7L);
		assertThat(entry.getTitle()).isEqualTo("GitHub");
		assertThat(entry.getAccount()).isEqualTo("alice");
		assertThat(entry.getSecret()).isEqualTo("cipher");
		assertThat(entry.getUrl()).isEqualTo("https://github.com");
		assertThat(entry.getNotes()).isEqualTo("notes-cipher");
		assertThat(entry.getCategory()).isEqualTo("开发");
		assertThat(entry.getTags()).isEqualTo("tag-cipher");
		assertThat(entry.getStrengthScore()).isEqualTo(80);
		assertThat(entry.getStrengthLevel()).isEqualTo("STRONG");
		assertThat(entry.getCreatedAt()).isEqualTo(t);
		assertThat(entry.getUpdatedAt()).isEqualTo(t);

		PasswordEntry fresh = new PasswordEntry();
		fresh.prePersist();
		assertThat(fresh.getCreatedAt()).isNotNull();
		LocalDateTime afterPersist = fresh.getUpdatedAt();
		fresh.preUpdate();
		assertThat(fresh.getUpdatedAt()).isAfterOrEqualTo(afterPersist);
	}

	@Test
	@DisplayName("SalaryRecord 读写、生命周期与各派生金额计算正确")
	void salaryRecordAccessorsAndDerived() {
		SalaryRecord record = new SalaryRecord();
		record.setId(8L);
		record.setUserId(9L);
		record.setYear(2025);
		record.setMonth(6);
		// 发放项
		record.setBaseSalary(new BigDecimal("10000"));
		record.setPerformanceSalary(new BigDecimal("2000"));
		record.setHousingAllowance(new BigDecimal("800"));
		record.setMealAllowance(new BigDecimal("300"));
		record.setTransportAllowance(new BigDecimal("200"));
		record.setOvertimePay(new BigDecimal("400"));
		record.setOvertimeAllowance(new BigDecimal("100"));
		record.setBonus(new BigDecimal("1000"));
		// 缴费基数
		record.setMedicalBase(new BigDecimal("10000"));
		record.setPensionUnemploymentBase(new BigDecimal("10000"));
		record.setHousingFundBase(new BigDecimal("10000"));
		// 扣除项
		record.setMedicalDeduction(new BigDecimal("200"));
		record.setPensionDeduction(new BigDecimal("800"));
		record.setUnemploymentDeduction(new BigDecimal("50"));
		record.setHousingFundDeduction(new BigDecimal("1200"));
		record.setIncomeTax(new BigDecimal("500"));
		// 税后附加项
		record.setSeriousIllnessMedical(new BigDecimal("150"));
		record.setHeatingAllowance(new BigDecimal("100"));
		record.setNetPay(new BigDecimal("12345.67"));
		record.setRemark("6 月工资");
		LocalDateTime t = LocalDateTime.now();
		record.setCreatedAt(t);
		record.setUpdatedAt(t);

		assertThat(record.getId()).isEqualTo(8L);
		assertThat(record.getUserId()).isEqualTo(9L);
		assertThat(record.getYear()).isEqualTo(2025);
		assertThat(record.getMonth()).isEqualTo(6);
		assertThat(record.isAnnualBonus()).isFalse();
		assertThat(record.getBaseSalary()).isEqualByComparingTo("10000");
		assertThat(record.getPerformanceSalary()).isEqualByComparingTo("2000");
		assertThat(record.getHousingAllowance()).isEqualByComparingTo("800");
		assertThat(record.getMealAllowance()).isEqualByComparingTo("300");
		assertThat(record.getTransportAllowance()).isEqualByComparingTo("200");
		assertThat(record.getOvertimePay()).isEqualByComparingTo("400");
		assertThat(record.getOvertimeAllowance()).isEqualByComparingTo("100");
		assertThat(record.getBonus()).isEqualByComparingTo("1000");
		assertThat(record.getMedicalBase()).isEqualByComparingTo("10000");
		assertThat(record.getPensionUnemploymentBase()).isEqualByComparingTo("10000");
		assertThat(record.getHousingFundBase()).isEqualByComparingTo("10000");
		assertThat(record.getMedicalDeduction()).isEqualByComparingTo("200");
		assertThat(record.getPensionDeduction()).isEqualByComparingTo("800");
		assertThat(record.getUnemploymentDeduction()).isEqualByComparingTo("50");
		assertThat(record.getHousingFundDeduction()).isEqualByComparingTo("1200");
		assertThat(record.getIncomeTax()).isEqualByComparingTo("500");
		assertThat(record.getSeriousIllnessMedical()).isEqualByComparingTo("150");
		assertThat(record.getHeatingAllowance()).isEqualByComparingTo("100");
		assertThat(record.getNetPay()).isEqualByComparingTo("12345.67");
		assertThat(record.getRemark()).isEqualTo("6 月工资");
		assertThat(record.getCreatedAt()).isEqualTo(t);
		assertThat(record.getUpdatedAt()).isEqualTo(t);

		// 应发 = 10000+2000+800+300+200+400+100+1000 = 14800
		assertThat(record.getGrossPay()).isEqualByComparingTo("14800");
		// 扣除项合计 = 200+800+50+1200 = 2250
		assertThat(record.getTotalDeduction()).isEqualByComparingTo("2250");
		// 税前 = 14800 - 2250 = 12550
		assertThat(record.getPreTaxSalary()).isEqualByComparingTo("12550");
		// 税后 = 12550 - 500 = 12050
		assertThat(record.getAfterTaxSalary()).isEqualByComparingTo("12050");

		SalaryRecord fresh = new SalaryRecord();
		fresh.prePersist();
		assertThat(fresh.getCreatedAt()).isNotNull();
		LocalDateTime afterPersist = fresh.getUpdatedAt();
		fresh.preUpdate();
		assertThat(fresh.getUpdatedAt()).isAfterOrEqualTo(afterPersist);
	}

	@Test
	@DisplayName("SalaryRecord 各金额为 null 时各派生金额按零计算")
	void salaryRecordNullAmounts() {
		SalaryRecord record = new SalaryRecord();
		record.setBaseSalary(null);
		record.setPerformanceSalary(null);
		record.setHousingAllowance(null);
		record.setMealAllowance(null);
		record.setTransportAllowance(null);
		record.setOvertimePay(null);
		record.setOvertimeAllowance(null);
		record.setBonus(null);
		record.setMedicalDeduction(null);
		record.setPensionDeduction(null);
		record.setUnemploymentDeduction(null);
		record.setHousingFundDeduction(null);
		record.setIncomeTax(null);
		record.setSeriousIllnessMedical(null);
		record.setHeatingAllowance(null);
		record.setNetPay(null);

		assertThat(record.getGrossPay()).isEqualByComparingTo("0");
		assertThat(record.getTotalDeduction()).isEqualByComparingTo("0");
		assertThat(record.getPreTaxSalary()).isEqualByComparingTo("0");
		assertThat(record.getAfterTaxSalary()).isEqualByComparingTo("0");
		assertThat(record.getNetPay()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("SalaryRecord 月份为 0 时识别为年终奖记录")
	void salaryRecordAnnualBonus() {
		SalaryRecord record = new SalaryRecord();
		record.setMonth(SalaryRecord.ANNUAL_BONUS_MONTH);
		assertThat(record.isAnnualBonus()).isTrue();
	}

}

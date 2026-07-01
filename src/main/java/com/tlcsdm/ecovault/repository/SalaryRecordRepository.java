package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 工资数据数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

	/**
	 * 查询用户全部工资记录，按年月升序。
	 * @param userId 用户 ID
	 * @return 工资记录列表
	 */
	List<SalaryRecord> findByUserIdOrderByYearAscMonthAsc(Long userId);

	/**
	 * 查询用户指定年份的工资记录。
	 * @param userId 用户 ID
	 * @param year 年份
	 * @return 工资记录列表
	 */
	List<SalaryRecord> findByUserIdAndYearOrderByMonthAsc(Long userId, int year);

	/**
	 * 按归属查询单条记录。
	 * @param id 记录 ID
	 * @param userId 用户 ID
	 * @return 记录 (可能为空)
	 */
	Optional<SalaryRecord> findByIdAndUserId(Long id, Long userId);

	/**
	 * 查询用户指定年月是否已存在记录。
	 * @param userId 用户 ID
	 * @param year 年份
	 * @param month 月份
	 * @return 记录 (可能为空)
	 */
	Optional<SalaryRecord> findByUserIdAndYearAndMonth(Long userId, int year, int month);

}

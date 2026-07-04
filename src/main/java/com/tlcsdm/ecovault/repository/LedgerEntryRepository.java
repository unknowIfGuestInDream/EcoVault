package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.LedgerEntry;
import com.tlcsdm.ecovault.entity.LedgerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 收入支出记录数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	/**
	 * 按归属查询单条记录。
	 * @param id 记录 ID
	 * @param userId 用户 ID
	 * @return 记录 (可能为空)
	 */
	Optional<LedgerEntry> findByIdAndUserId(Long id, Long userId);

	/**
	 * 多条件查询用户的收支记录。
	 *
	 * <p>
	 * 各条件均可为空，为空时忽略该过滤条件；支持按类型、时间区间与标签过滤，按日期倒序返回。
	 * </p>
	 * @param userId 用户 ID
	 * @param type 收支类型 (可空)
	 * @param start 起始日期 (含，可空)
	 * @param end 结束日期 (含，可空)
	 * @param tag 标签 (可空，精确匹配其中一个标签)
	 * @return 记录列表
	 */
	@Query("SELECT DISTINCT e FROM LedgerEntry e LEFT JOIN e.tags t WHERE e.userId = :userId "
			+ "AND (:type IS NULL OR e.type = :type) " + "AND (:start IS NULL OR e.entryDate >= :start) "
			+ "AND (:end IS NULL OR e.entryDate <= :end) " + "AND (:tag IS NULL OR t = :tag) "
			+ "ORDER BY e.entryDate DESC, e.id DESC")
	List<LedgerEntry> search(@Param("userId") Long userId, @Param("type") LedgerType type,
			@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("tag") String tag);

}

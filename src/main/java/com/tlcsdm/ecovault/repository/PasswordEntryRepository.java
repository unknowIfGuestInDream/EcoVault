package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.PasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 密码条目数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {

	/**
	 * 查询用户的全部密码条目。
	 * @param userId 用户 ID
	 * @return 条目列表
	 */
	List<PasswordEntry> findByUserIdOrderByUpdatedAtDesc(Long userId);

	/**
	 * 根据 ID 与用户 ID 查询条目 (确保数据归属)。
	 * @param id 条目 ID
	 * @param userId 用户 ID
	 * @return 条目 (可能为空)
	 */
	Optional<PasswordEntry> findByIdAndUserId(Long id, Long userId);

	/**
	 * 按标题模糊搜索用户的条目。
	 * @param userId 用户 ID
	 * @param keyword 关键字
	 * @return 条目列表
	 */
	List<PasswordEntry> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String keyword);

}

package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 根据用户名查询用户。
	 * @param username 用户名
	 * @return 用户 (可能为空)
	 */
	Optional<User> findByUsername(String username);

	/**
	 * 判断用户名是否已存在。
	 * @param username 用户名
	 * @return 是否存在
	 */
	boolean existsByUsername(String username);

}

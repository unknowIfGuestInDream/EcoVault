package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户会话数据访问接口。
 *
 * @author 梦里不知身是客
 */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * 根据 JWT 唯一标识查询会话。
     *
     * @param jti JWT ID
     * @return 会话 (可能为空)
     */
    Optional<UserSession> findByJti(String jti);

    /**
     * 查询用户所有活跃会话，按创建时间升序 (最早的在前)。
     *
     * @param userId 用户 ID
     * @return 活跃会话列表
     */
    List<UserSession> findByUserIdAndActiveTrueOrderByCreatedAtAsc(Long userId);
}

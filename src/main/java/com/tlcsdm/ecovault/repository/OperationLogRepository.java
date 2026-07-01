package com.tlcsdm.ecovault.repository;

import com.tlcsdm.ecovault.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 操作日志数据访问接口。
 *
 * @author unknowIfGuestInDream
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    /**
     * 分页条件查询日志。
     *
     * <p>各条件均可为空，为空时忽略该过滤条件。管理员可传入任意 userId，
     * 普通用户由服务层强制限定为自身 userId。</p>
     *
     * @param userId   用户 ID (可空)
     * @param module   模块 (可空)
     * @param keyword  关键字 (可空，匹配操作描述)
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM OperationLog l WHERE "
            + "(:userId IS NULL OR l.userId = :userId) AND "
            + "(:module IS NULL OR l.module = :module) AND "
            + "(:keyword IS NULL OR l.operation LIKE %:keyword%) "
            + "ORDER BY l.createdAt DESC")
    Page<OperationLog> search(@Param("userId") Long userId,
                              @Param("module") String module,
                              @Param("keyword") String keyword,
                              Pageable pageable);
}

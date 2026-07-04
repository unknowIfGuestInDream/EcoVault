package com.tlcsdm.ecovault.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实体与枚举模型基础行为测试。
 *
 * @author unknowIfGuestInDream
 */
class EntityModelTest {

	@Test
	@DisplayName("RolePermission Getter/Setter 可正确读写字段")
	void rolePermissionAccessors() {
		RolePermission permission = new RolePermission();
		permission.setId(10L);
		permission.setRole(Role.ADMIN);
		permission.setPageKey("ledger");

		assertThat(permission.getId()).isEqualTo(10L);
		assertThat(permission.getRole()).isEqualTo(Role.ADMIN);
		assertThat(permission.getPageKey()).isEqualTo("ledger");
	}

	@Test
	@DisplayName("LedgerEntry Getter/Setter 可正确读写时间与用户字段")
	void ledgerEntryAccessors() {
		LedgerEntry entry = new LedgerEntry();
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 4, 12, 0);
		LocalDateTime updatedAt = createdAt.plusMinutes(5);
		entry.setUserId(7L);
		entry.setCreatedAt(createdAt);
		entry.setUpdatedAt(updatedAt);

		assertThat(entry.getUserId()).isEqualTo(7L);
		assertThat(entry.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entry.getUpdatedAt()).isEqualTo(updatedAt);
	}

	@Test
	@DisplayName("MenuPage.fromKey 可按 key 查找页面")
	void menuPageFromKey() {
		assertThat(MenuPage.fromKey("ledger")).contains(MenuPage.LEDGER);
		assertThat(MenuPage.fromKey("missing")).isEmpty();
	}

}

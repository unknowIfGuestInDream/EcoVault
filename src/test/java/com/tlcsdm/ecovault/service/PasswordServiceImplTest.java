package com.tlcsdm.ecovault.service;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.PasswordEntryRequest;
import com.tlcsdm.ecovault.dto.PasswordEntryResponse;
import com.tlcsdm.ecovault.entity.PasswordEntry;
import com.tlcsdm.ecovault.repository.PasswordEntryRepository;
import com.tlcsdm.ecovault.service.impl.PasswordServiceImpl;
import com.tlcsdm.ecovault.utils.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 密码管理服务单元测试。
 *
 * <p>
 * 使用真实 {@link AesUtil} 校验敏感字段加密落库与解密还原， 仓储层通过 Mockito 模拟。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

	@Mock
	private PasswordEntryRepository repository;

	private PasswordServiceImpl service;

	private final AesUtil aesUtil = new AesUtil("ecovault-test-aes-secret-32bytes!");

	@BeforeEach
	void setUp() {
		service = new PasswordServiceImpl(repository, aesUtil);
	}

	@Test
	@DisplayName("创建时敏感字段加密落库，返回解密后的明文与强度")
	void createEncryptsAndReturnsPlain() {
		PasswordEntryRequest request = new PasswordEntryRequest("GitHub", "alice", "Abcdef123!@#", "https://github.com",
				"工作账号", "开发", List.of("工作", "代码"));
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> {
			PasswordEntry entry = inv.getArgument(0);
			// 落库的敏感字段必须是密文，不能是明文
			assertThat(entry.getSecret()).isNotEqualTo("Abcdef123!@#");
			assertThat(entry.getNotes()).isNotEqualTo("工作账号");
			assertThat(entry.getTags()).doesNotContain("工作");
			entry.setId(10L);
			return entry;
		});

		PasswordEntryResponse response = service.create(1L, request);

		assertThat(response.secret()).isEqualTo("Abcdef123!@#");
		assertThat(response.notes()).isEqualTo("工作账号");
		assertThat(response.tags()).containsExactly("工作", "代码");
		assertThat(response.strengthLevel()).isEqualTo("STRONG");
	}

	@Test
	@DisplayName("按标签筛选仅返回包含该标签的条目")
	void listFilterByTag() {
		PasswordEntry a = buildEntry(1L, "A", "p1", List.of("工作"));
		PasswordEntry b = buildEntry(2L, "B", "p2", List.of("个人"));
		when(repository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(a, b));

		List<PasswordEntryResponse> result = service.list(1L, null, "工作");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).title()).isEqualTo("A");
	}

	@Test
	@DisplayName("更新不存在的条目抛出业务异常")
	void updateMissingThrows() {
		PasswordEntryRequest request = new PasswordEntryRequest("X", null, "secret", null, null, null, null);
		when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(1L, 99L, request)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("关键字非空时按标题模糊查询，且无标签筛选返回全部")
	void listWithKeywordNoTag() {
		PasswordEntry a = buildEntry(1L, "GitHub", "p1", List.of("工作"));
		when(repository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(1L, "Git")).thenReturn(List.of(a));

		List<PasswordEntryResponse> result = service.list(1L, "  Git  ", "  ");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).title()).isEqualTo("GitHub");
	}

	@Test
	@DisplayName("标签筛选时忽略标签为空的条目")
	void listFilterSkipsEntriesWithoutTags() {
		PasswordEntry a = buildEntry(1L, "A", "p1", List.of("工作"));
		PasswordEntry noTag = new PasswordEntry();
		noTag.setId(2L);
		noTag.setUserId(1L);
		noTag.setTitle("B");
		noTag.setSecret(aesUtil.encrypt("p2"));
		noTag.setTags(null);
		when(repository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(a, noTag));

		List<PasswordEntryResponse> result = service.list(1L, "", "工作");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).title()).isEqualTo("A");
	}

	@Test
	@DisplayName("空白或空标签在加密时被过滤为无标签")
	void createWithBlankTagsStoresNull() {
		PasswordEntryRequest request = new PasswordEntryRequest("X", null, "Abcdef123!@#", null, null, null,
				List.of(" ", ""));
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> {
			PasswordEntry entry = inv.getArgument(0);
			assertThat(entry.getTags()).isNull();
			entry.setId(5L);
			return entry;
		});

		PasswordEntryResponse response = service.create(1L, request);

		assertThat(response.tags()).isEmpty();
	}

	@Test
	@DisplayName("更新存在的条目成功写入新内容")
	void updateSuccess() {
		PasswordEntry entry = buildEntry(3L, "旧标题", "old", List.of("工作"));
		when(repository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(entry));
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> inv.getArgument(0));

		PasswordEntryRequest request = new PasswordEntryRequest("新标题", "acc", "NewSecret1!", "https://x.com", "备注",
				"分类", List.of("个人"));
		PasswordEntryResponse response = service.update(1L, 3L, request);

		assertThat(response.title()).isEqualTo("新标题");
		assertThat(response.secret()).isEqualTo("NewSecret1!");
		assertThat(response.tags()).containsExactly("个人");
	}

	@Test
	@DisplayName("获取存在的条目返回解密内容")
	void getSuccess() {
		PasswordEntry entry = buildEntry(4L, "标题", "secret1", List.of("工作", "代码"));
		when(repository.findByIdAndUserId(4L, 1L)).thenReturn(Optional.of(entry));

		PasswordEntryResponse response = service.get(1L, 4L);

		assertThat(response.title()).isEqualTo("标题");
		assertThat(response.secret()).isEqualTo("secret1");
		assertThat(response.tags()).containsExactly("工作", "代码");
	}

	@Test
	@DisplayName("删除存在的条目调用仓储删除")
	void deleteSuccess() {
		PasswordEntry entry = buildEntry(6L, "标题", "secret", List.of());
		when(repository.findByIdAndUserId(6L, 1L)).thenReturn(Optional.of(entry));

		service.delete(1L, 6L);

		org.mockito.Mockito.verify(repository).delete(entry);
	}

	@Test
	@DisplayName("删除不存在的条目抛出业务异常")
	void deleteMissingThrows() {
		when(repository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(1L, 7L)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("获取不存在的条目抛出业务异常")
	void getMissingThrows() {
		when(repository.findByIdAndUserId(8L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(1L, 8L)).isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("标签含 null/空白/有效项时仅保留有效标签")
	void createWithMixedTags() {
		java.util.List<String> tags = java.util.Arrays.asList("工作", null, "  ", "代码");
		PasswordEntryRequest request = new PasswordEntryRequest("X", null, "Abcdef123!@#", null, null, null, tags);
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> {
			PasswordEntry entry = inv.getArgument(0);
			entry.setId(11L);
			return entry;
		});

		PasswordEntryResponse response = service.create(1L, request);

		assertThat(response.tags()).containsExactly("工作", "代码");
	}

	@Test
	@DisplayName("同时按关键字与标签筛选")
	void listWithKeywordAndTag() {
		PasswordEntry a = buildEntry(1L, "GitHub", "p1", List.of("工作"));
		PasswordEntry b = buildEntry(2L, "GitLab", "p2", List.of("个人"));
		when(repository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(1L, "Git"))
			.thenReturn(List.of(a, b));

		List<PasswordEntryResponse> result = service.list(1L, "Git", "工作");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).title()).isEqualTo("GitHub");
	}

	@Test
	@DisplayName("关键字与标签均为 null 时返回全部并解密标签")
	void listWithoutKeywordAndTag() {
		PasswordEntry a = buildEntry(1L, "A", "p1", List.of("工作"));
		when(repository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(a));

		List<PasswordEntryResponse> result = service.list(1L, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).tags()).containsExactly("工作");
	}

	@Test
	@DisplayName("标签列表为 null 时加密结果为空")
	void createWithNullTagsStoresNull() {
		PasswordEntryRequest request = new PasswordEntryRequest("X", null, "Abcdef123!@#", null, null, null, null);
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> {
			PasswordEntry entry = inv.getArgument(0);
			assertThat(entry.getTags()).isNull();
			entry.setId(20L);
			return entry;
		});

		PasswordEntryResponse response = service.create(1L, request);

		assertThat(response.tags()).isEmpty();
	}

	@Test
	@DisplayName("标签列表为空集合时加密结果为空")
	void createWithEmptyTagListStoresNull() {
		PasswordEntryRequest request = new PasswordEntryRequest("X", null, "Abcdef123!@#", null, null, null, List.of());
		when(repository.save(any(PasswordEntry.class))).thenAnswer(inv -> {
			PasswordEntry entry = inv.getArgument(0);
			assertThat(entry.getTags()).isNull();
			entry.setId(21L);
			return entry;
		});

		PasswordEntryResponse response = service.create(1L, request);

		assertThat(response.tags()).isEmpty();
	}

	@Test
	@DisplayName("标签密文为空白时解密为空列表")
	void getWithBlankTagCipher() {
		PasswordEntry entry = new PasswordEntry();
		entry.setId(30L);
		entry.setUserId(1L);
		entry.setTitle("标题");
		entry.setSecret(aesUtil.encrypt("secret"));
		// 直接存入空白 (非加密) 标签串，覆盖 cipher.isBlank() 分支
		entry.setTags("   ");
		when(repository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(entry));

		PasswordEntryResponse response = service.get(1L, 30L);

		assertThat(response.tags()).isEmpty();
	}

	@Test
	@DisplayName("标签密文解密为空串时返回空列表")
	void getWithEmptyDecryptedTags() {
		PasswordEntry entry = new PasswordEntry();
		entry.setId(31L);
		entry.setUserId(1L);
		entry.setTitle("标题");
		entry.setSecret(aesUtil.encrypt("secret"));
		// 加密空串，解密后 joined 为空串，覆盖 joined.isBlank() 分支
		entry.setTags(aesUtil.encrypt(""));
		when(repository.findByIdAndUserId(31L, 1L)).thenReturn(Optional.of(entry));

		PasswordEntryResponse response = service.get(1L, 31L);

		assertThat(response.tags()).isEmpty();
	}

	@Test
	@DisplayName("标签串含连续分隔符时跳过空白项")
	void getSkipsBlankTagSegments() {
		PasswordEntry entry = new PasswordEntry();
		entry.setId(32L);
		entry.setUserId(1L);
		entry.setTitle("标题");
		entry.setSecret(aesUtil.encrypt("secret"));
		// 含空白段，覆盖 decryptTags 过滤空白项的 false 分支
		entry.setTags(aesUtil.encrypt("工作,,代码"));
		when(repository.findByIdAndUserId(32L, 1L)).thenReturn(Optional.of(entry));

		PasswordEntryResponse response = service.get(1L, 32L);

		assertThat(response.tags()).containsExactly("工作", "代码");
	}

	private PasswordEntry buildEntry(Long id, String title, String secret, List<String> tags) {
		PasswordEntry entry = new PasswordEntry();
		entry.setId(id);
		entry.setUserId(1L);
		entry.setTitle(title);
		entry.setSecret(aesUtil.encrypt(secret));
		entry.setTags(aesUtil.encrypt(String.join(",", tags)));
		entry.setStrengthScore(50);
		entry.setStrengthLevel("MEDIUM");
		return entry;
	}

}

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

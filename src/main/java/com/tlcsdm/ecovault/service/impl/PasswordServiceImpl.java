package com.tlcsdm.ecovault.service.impl;

import com.tlcsdm.ecovault.common.BusinessException;
import com.tlcsdm.ecovault.dto.PasswordEntryRequest;
import com.tlcsdm.ecovault.dto.PasswordEntryResponse;
import com.tlcsdm.ecovault.entity.PasswordEntry;
import com.tlcsdm.ecovault.repository.PasswordEntryRepository;
import com.tlcsdm.ecovault.service.PasswordService;
import com.tlcsdm.ecovault.utils.AesUtil;
import com.tlcsdm.ecovault.utils.PasswordStrengthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 密码管理服务实现。
 *
 * <p>
 * 密码、备注、标签等敏感字段使用 AES 加密后落库，读取时解密返回。 标签加密存储可避免包含敏感关键字时被 Web 防火墙误拦截。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Service
public class PasswordServiceImpl implements PasswordService {

	private static final String MASKED_SECRET = "******";

	private final PasswordEntryRepository repository;

	private final AesUtil aesUtil;

	public PasswordServiceImpl(PasswordEntryRepository repository, AesUtil aesUtil) {
		this.repository = repository;
		this.aesUtil = aesUtil;
	}

	@Override
	@Transactional
	public PasswordEntryResponse create(Long userId, PasswordEntryRequest request) {
		PasswordEntry entry = new PasswordEntry();
		entry.setUserId(userId);
		applyRequest(entry, request);
		return toResponse(repository.save(entry));
	}

	@Override
	@Transactional
	public PasswordEntryResponse update(Long userId, Long id, PasswordEntryRequest request) {
		PasswordEntry entry = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("密码条目不存在"));
		applyRequest(entry, request);
		return toResponse(repository.save(entry));
	}

	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		PasswordEntry entry = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("密码条目不存在"));
		repository.delete(entry);
	}

	@Override
	@Transactional(readOnly = true)
	public PasswordEntryResponse get(Long userId, Long id) {
		PasswordEntry entry = repository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new BusinessException("密码条目不存在"));
		return toResponse(entry);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PasswordEntryResponse> list(Long userId, String keyword, String tag) {
		List<PasswordEntry> entries = (keyword == null || keyword.isBlank())
				? repository.findByUserIdOrderByUpdatedAtDesc(userId)
				: repository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, keyword.trim());

		List<PasswordEntryResponse> responses = entries.stream().map(this::toListResponse).collect(Collectors.toList());

		// 标签筛选在解密后进行 (标签以密文存储)
		if (tag != null && !tag.isBlank()) {
			String target = tag.trim();
			responses = responses.stream()
				.filter(r -> r.tags().stream().anyMatch(t -> t.equalsIgnoreCase(target)))
				.collect(Collectors.toList());
		}
		return responses;
	}

	/**
	 * 将请求内容写入实体，并加密敏感字段、计算密码强度。
	 * @param entry 实体
	 * @param request 请求
	 */
	private void applyRequest(PasswordEntry entry, PasswordEntryRequest request) {
		entry.setTitle(request.title());
		entry.setAccount(request.account());
		entry.setUrl(request.url());
		entry.setCategory(request.category());
		entry.setSecret(aesUtil.encrypt(request.secret()));
		entry.setNotes(aesUtil.encrypt(request.notes()));
		entry.setTags(encryptTags(request.tags()));

		PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.evaluate(request.secret());
		entry.setStrengthScore(strength.score());
		entry.setStrengthLevel(strength.level());
	}

	/**
	 * 将标签列表拼接为逗号分隔字符串后加密。
	 * @param tags 标签列表
	 * @return 加密后的标签串
	 */
	private String encryptTags(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}
		String joined = tags.stream()
			.filter(t -> t != null && !t.isBlank())
			.map(String::trim)
			.collect(Collectors.joining(","));
		return joined.isEmpty() ? null : aesUtil.encrypt(joined);
	}

	/**
	 * 解密标签串为列表。
	 * @param cipher 密文
	 * @return 标签列表
	 */
	private List<String> decryptTags(String cipher) {
		if (cipher == null || cipher.isBlank()) {
			return new ArrayList<>();
		}
		String joined = aesUtil.decrypt(cipher);
		if (joined.isBlank()) {
			return new ArrayList<>();
		}
		return Arrays.stream(joined.split(",")).filter(t -> !t.isBlank()).collect(Collectors.toList());
	}

	/**
	 * 实体转响应 (解密敏感字段)。
	 * @param entry 实体
	 * @return 响应
	 */
	private PasswordEntryResponse toResponse(PasswordEntry entry) {
		return buildResponse(entry, aesUtil.decrypt(entry.getSecret()), aesUtil.decrypt(entry.getNotes()),
				entry.getCategory(), entry.getStrengthScore(), entry.getStrengthLevel());
	}

	/**
	 * 实体转列表响应（默认脱敏密码，避免列表直接暴露明文）。
	 * @param entry 实体
	 * @return 列表响应
	 */
	private PasswordEntryResponse toListResponse(PasswordEntry entry) {
		return buildResponse(entry, MASKED_SECRET, null, null, 0, null);
	}

	private PasswordEntryResponse buildResponse(PasswordEntry entry, String secret, String notes, String category,
			int strengthScore, String strengthLevel) {
		return new PasswordEntryResponse(entry.getId(), entry.getTitle(), entry.getAccount(), secret, entry.getUrl(),
				notes, category, decryptTags(entry.getTags()), strengthScore, strengthLevel, entry.getCreatedAt(),
				entry.getUpdatedAt());
	}

}

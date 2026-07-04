package com.tlcsdm.ecovault.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 稳定的分页响应结构，避免直接序列化 Spring Data {@link Page} 实现类。
 *
 * @param content 当前页数据
 * @param number 当前页码（从 0 开始）
 * @param size 每页大小
 * @param totalElements 总记录数
 * @param totalPages 总页数
 * @param first 是否首页
 * @param last 是否尾页
 * @author unknowIfGuestInDream
 */
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages, boolean first,
		boolean last) {

	/**
	 * 从 Spring Data 分页对象转换为稳定 DTO。
	 * @param page 分页对象
	 * @param <T> 数据类型
	 * @return 分页响应
	 */
	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages(), page.isFirst(), page.isLast());
	}

}

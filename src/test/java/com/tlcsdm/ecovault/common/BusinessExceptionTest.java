package com.tlcsdm.ecovault.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务异常单元测试。
 *
 * @author unknowIfGuestInDream
 */
class BusinessExceptionTest {

	@Test
	@DisplayName("单参构造默认状态码为 400")
	void defaultCode() {
		BusinessException ex = new BusinessException("参数错误");
		assertThat(ex.getCode()).isEqualTo(400);
		assertThat(ex.getMessage()).isEqualTo("参数错误");
	}

	@Test
	@DisplayName("双参构造使用自定义状态码")
	void customCode() {
		BusinessException ex = new BusinessException(401, "未登录");
		assertThat(ex.getCode()).isEqualTo(401);
		assertThat(ex.getMessage()).isEqualTo("未登录");
	}

}

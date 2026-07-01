package com.tlcsdm.ecovault.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应封装单元测试。
 *
 * @author unknowIfGuestInDream
 */
class ApiResponseTest {

	@Test
	@DisplayName("success(data) 返回 code=0 并携带数据")
	void successWithData() {
		ApiResponse<String> response = ApiResponse.success("hello");
		assertThat(response.getCode()).isZero();
		assertThat(response.getMessage()).isEqualTo("成功");
		assertThat(response.getData()).isEqualTo("hello");
	}

	@Test
	@DisplayName("success() 返回 code=0 且数据为空")
	void successNoData() {
		ApiResponse<Void> response = ApiResponse.success();
		assertThat(response.getCode()).isZero();
		assertThat(response.getMessage()).isEqualTo("成功");
		assertThat(response.getData()).isNull();
	}

	@Test
	@DisplayName("error 返回自定义状态码与信息")
	void error() {
		ApiResponse<Void> response = ApiResponse.error(400, "参数错误");
		assertThat(response.getCode()).isEqualTo(400);
		assertThat(response.getMessage()).isEqualTo("参数错误");
		assertThat(response.getData()).isNull();
	}

	@Test
	@DisplayName("无参构造与 setter 可正常读写")
	void settersAndGetters() {
		ApiResponse<Integer> response = new ApiResponse<>();
		response.setCode(7);
		response.setMessage("m");
		response.setData(99);
		assertThat(response.getCode()).isEqualTo(7);
		assertThat(response.getMessage()).isEqualTo("m");
		assertThat(response.getData()).isEqualTo(99);
	}

	@Test
	@DisplayName("全参构造正确赋值")
	void allArgsConstructor() {
		ApiResponse<String> response = new ApiResponse<>(1, "msg", "d");
		assertThat(response.getCode()).isEqualTo(1);
		assertThat(response.getMessage()).isEqualTo("msg");
		assertThat(response.getData()).isEqualTo("d");
	}

}

package com.tlcsdm.ecovault.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理器单元测试。
 *
 * @author unknowIfGuestInDream
 */
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("业务异常转换为 400 与业务状态码")
	void handleBusiness() {
		ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(new BusinessException(409, "冲突"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(409);
		assertThat(response.getBody().getMessage()).isEqualTo("冲突");
	}

	@Test
	@DisplayName("参数校验异常拼接字段错误信息")
	void handleValidation() throws Exception {
		Method method = Sample.class.getDeclaredMethod("sample", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
		bindingResult.addError(new FieldError("target", "username", "用户名不能为空"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

		ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(400);
		assertThat(response.getBody().getMessage()).contains("用户名不能为空");
	}

	@Test
	@DisplayName("权限不足异常转换为 403")
	void handleAccessDenied() {
		ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("未知系统异常转换为 500 且不泄露堆栈")
	void handleUnknown() {
		ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("boom"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(500);
		assertThat(response.getBody().getMessage()).isEqualTo("系统内部错误，请稍后重试");
	}

	private static final class Sample {

		@SuppressWarnings("unused")
		void sample(String name) {
		}

	}

}

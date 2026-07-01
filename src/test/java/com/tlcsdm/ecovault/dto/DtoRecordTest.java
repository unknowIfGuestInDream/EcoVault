package com.tlcsdm.ecovault.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO record 单元测试，校验访问器返回构造时传入的值。
 *
 * @author unknowIfGuestInDream
 */
class DtoRecordTest {

	@Test
	@DisplayName("UpdateProfileRequest 保存昵称与邮箱")
	void updateProfileRequest() {
		UpdateProfileRequest request = new UpdateProfileRequest("新昵称", "new@ecovault.com");
		assertThat(request.nickname()).isEqualTo("新昵称");
		assertThat(request.email()).isEqualTo("new@ecovault.com");
	}

	@Test
	@DisplayName("RegisterRequest 保存注册字段")
	void registerRequest() {
		RegisterRequest request = new RegisterRequest("alice", "Passw0rd!", "Alice", "a@b.com");
		assertThat(request.username()).isEqualTo("alice");
		assertThat(request.password()).isEqualTo("Passw0rd!");
		assertThat(request.nickname()).isEqualTo("Alice");
		assertThat(request.email()).isEqualTo("a@b.com");
	}

	@Test
	@DisplayName("LoginResponse 保存令牌与用户信息")
	void loginResponse() {
		LoginResponse response = new LoginResponse("token", "alice", "Alice", "USER");
		assertThat(response.token()).isEqualTo("token");
		assertThat(response.username()).isEqualTo("alice");
		assertThat(response.nickname()).isEqualTo("Alice");
		assertThat(response.role()).isEqualTo("USER");
	}

}

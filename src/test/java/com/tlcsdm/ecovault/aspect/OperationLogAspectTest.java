package com.tlcsdm.ecovault.aspect;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.entity.Role;
import com.tlcsdm.ecovault.entity.User;
import com.tlcsdm.ecovault.security.SecurityUser;
import com.tlcsdm.ecovault.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 操作日志切面单元测试。
 *
 * <p>
 * 校验成功/失败路径的状态记录、参数脱敏 (仅记录类型且过滤 HttpServletRequest)、 当前登录用户与 IP 填充，以及日志保存失败不影响主流程。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class OperationLogAspectTest {

	private OperationLogService logService;

	private OperationLogAspect aspect;

	@BeforeEach
	void setUp() {
		logService = mock(OperationLogService.class);
		aspect = new OperationLogAspect(logService, new tools.jackson.databind.ObjectMapper());
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
		RequestContextHolder.resetRequestAttributes();
	}

	@OperationLogRecord(module = "测试模块", operation = "测试操作")
	public void annotated(String arg, HttpServletRequest request, Object nullable) {
	}

	@OperationLogRecord(module = "测试模块", operation = "测试响应过滤")
	public void withResponse(String arg, HttpServletResponse response) {
	}

	@OperationLogRecord(module = "安全", operation = "登录")
	public void sensitive(com.tlcsdm.ecovault.dto.LoginRequest request) {
	}

	public void noAnnotation() {
	}

	private MethodSignature signatureFor(String methodName, Class<?>... params) throws Exception {
		Method method = OperationLogAspectTest.class.getMethod(methodName, params);
		MethodSignature signature = mock(MethodSignature.class);
		when(signature.getMethod()).thenReturn(method);
		when(signature.getDeclaringType()).thenReturn(OperationLogAspectTest.class);
		return signature;
	}

	private void authenticate() {
		User user = new User();
		user.setId(1L);
		user.setUsername("alice");
		user.setRole(Role.USER);
		user.setEnabled(true);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(new SecurityUser(user), null,
					new SecurityUser(user).getAuthorities()));
	}

	private void bindRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Real-IP", "10.1.2.3");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@Test
	@DisplayName("成功执行记录 SUCCESS 状态、模块、操作、参数与用户信息")
	void successPath() throws Throwable {
		authenticate();
		bindRequest();
		MethodSignature signature = signatureFor("annotated", String.class, HttpServletRequest.class, Object.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { "hello", new MockHttpServletRequest(), null });
		when(joinPoint.proceed()).thenReturn("ok");

		Object result = aspect.around(joinPoint);

		assertThat(result).isEqualTo("ok");
		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		OperationLog saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo("SUCCESS");
		assertThat(saved.getModule()).isEqualTo("测试模块");
		assertThat(saved.getOperation()).isEqualTo("测试操作");
		assertThat(saved.getMethod()).isEqualTo("OperationLogAspectTest.annotated");
		// HttpServletRequest 参数被过滤，其余参数按实际值序列化为 JSON，null 记录为 null
		assertThat(saved.getParams()).isEqualTo("[\"hello\",null]");
		assertThat(saved.getUserId()).isEqualTo(1L);
		assertThat(saved.getUsername()).isEqualTo("alice");
		assertThat(saved.getIp()).isEqualTo("10.1.2.3");
		assertThat(saved.getDurationMs()).isGreaterThanOrEqualTo(0L);
	}

	@Test
	@DisplayName("目标方法抛出异常时记录 FAILURE 与错误信息并向上抛出")
	void failurePath() throws Throwable {
		MethodSignature signature = signatureFor("annotated", String.class, HttpServletRequest.class, Object.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[0]);
		when(joinPoint.proceed()).thenThrow(new IllegalStateException("业务失败"));

		assertThatThrownBy(() -> aspect.around(joinPoint)).isInstanceOf(IllegalStateException.class);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		OperationLog saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo("FAILURE");
		assertThat(saved.getErrorMsg()).isEqualTo("业务失败");
		// 无参数时参数摘要为空字符串
		assertThat(saved.getParams()).isEmpty();
		// 无认证与请求上下文时用户与 IP 均为空
		assertThat(saved.getUserId()).isNull();
		assertThat(saved.getIp()).isNull();
	}

	@Test
	@DisplayName("日志保存失败不影响主流程返回")
	void saveFailureSwallowed() throws Throwable {
		MethodSignature signature = signatureFor("annotated", String.class, HttpServletRequest.class, Object.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { "x", null, "y" });
		when(joinPoint.proceed()).thenReturn("done");
		doThrow(new RuntimeException("db down")).when(logService).save(any(OperationLog.class));

		Object result = aspect.around(joinPoint);

		assertThat(result).isEqualTo("done");
		verify(logService).save(any(OperationLog.class));
	}

	@Test
	@DisplayName("参数中的敏感字段 (如密码) 在记录时被脱敏")
	void masksSensitiveFields() throws Throwable {
		MethodSignature signature = signatureFor("sensitive", com.tlcsdm.ecovault.dto.LoginRequest.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs())
			.thenReturn(new Object[] { new com.tlcsdm.ecovault.dto.LoginRequest("alice", "Secret123!") });
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		String params = captor.getValue().getParams();
		assertThat(params).contains("alice").contains("******").doesNotContain("Secret123!");
	}

	@Test
	@DisplayName("HttpServletResponse 参数会被过滤，不写入日志摘要")
	void filtersHttpServletResponse() throws Throwable {
		MethodSignature signature = signatureFor("withResponse", String.class, HttpServletResponse.class);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { "hello", new MockHttpServletResponse() });
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		assertThat(captor.getValue().getParams()).isEqualTo("[\"hello\"]");
	}

	@Test
	@DisplayName("方法无注解时模块与操作为空，仍记录方法与状态")
	void withoutAnnotation() throws Throwable {
		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[0]);
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		OperationLog saved = captor.getValue();
		assertThat(saved.getModule()).isNull();
		assertThat(saved.getOperation()).isNull();
		assertThat(saved.getMethod()).isEqualTo("OperationLogAspectTest.noAnnotation");
		assertThat(saved.getStatus()).isEqualTo("SUCCESS");
	}

	@Test
	@DisplayName("方法参数为 null 时参数摘要为空字符串")
	void nullArgsProducesEmptyParams() throws Throwable {
		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(null);
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		assertThat(captor.getValue().getParams()).isEmpty();
	}

	@Test
	@DisplayName("异常信息为 null 时错误信息记录为 null")
	void nullExceptionMessage() throws Throwable {
		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[0]);
		when(joinPoint.proceed()).thenThrow(new IllegalStateException());

		assertThatThrownBy(() -> aspect.around(joinPoint)).isInstanceOf(IllegalStateException.class);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		OperationLog saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo("FAILURE");
		assertThat(saved.getErrorMsg()).isNull();
	}

	@Test
	@DisplayName("超长异常信息按上限截断")
	void longExceptionMessageTruncated() throws Throwable {
		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[0]);
		when(joinPoint.proceed()).thenThrow(new IllegalStateException("e".repeat(1500)));

		assertThatThrownBy(() -> aspect.around(joinPoint)).isInstanceOf(IllegalStateException.class);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		assertThat(captor.getValue().getErrorMsg()).hasSize(1000);
	}

	@Test
	@DisplayName("参数无法序列化时退化为类型名")
	void fallsBackToSimpleTypeNameWhenSerializationFails() throws Throwable {
		tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
		tools.jackson.databind.ObjectMapper failingMapper = mock(tools.jackson.databind.ObjectMapper.class);
		when(failingMapper.createArrayNode()).thenReturn(realMapper.createArrayNode());
		when(failingMapper.valueToTree(any())).thenThrow(new RuntimeException("serialize fail"));
		when(failingMapper.writeValueAsString(any()))
			.thenAnswer(inv -> realMapper.writeValueAsString(inv.getArgument(0)));
		aspect = new OperationLogAspect(logService, failingMapper);

		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { new java.io.ByteArrayInputStream(new byte[0]) });
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		assertThat(captor.getValue().getParams()).contains("ByteArrayInputStream");
	}

	@Test
	@DisplayName("参数摘要 JSON 序列化失败时返回空字符串")
	void returnsEmptyParamsWhenJsonSerializationFails() throws Throwable {
		tools.jackson.databind.ObjectMapper realMapper = new tools.jackson.databind.ObjectMapper();
		tools.jackson.databind.ObjectMapper failingMapper = mock(tools.jackson.databind.ObjectMapper.class);
		when(failingMapper.createArrayNode()).thenReturn(realMapper.createArrayNode());
		when(failingMapper.valueToTree(any())).thenAnswer(inv -> realMapper.valueToTree(inv.getArgument(0)));
		when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json fail"));
		aspect = new OperationLogAspect(logService, failingMapper);

		MethodSignature signature = signatureFor("noAnnotation");
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[] { "hello" });
		when(joinPoint.proceed()).thenReturn("ok");

		aspect.around(joinPoint);

		ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
		verify(logService).save(captor.capture());
		assertThat(captor.getValue().getParams()).isEmpty();
	}

}

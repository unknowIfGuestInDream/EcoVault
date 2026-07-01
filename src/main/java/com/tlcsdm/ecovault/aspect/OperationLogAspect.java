package com.tlcsdm.ecovault.aspect;

import com.tlcsdm.ecovault.annotation.OperationLogRecord;
import com.tlcsdm.ecovault.entity.OperationLog;
import com.tlcsdm.ecovault.security.SecurityUser;
import com.tlcsdm.ecovault.service.OperationLogService;
import com.tlcsdm.ecovault.utils.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面。
 *
 * <p>
 * 拦截标注了 {@link OperationLogRecord} 的方法，自动记录操作人、模块、 操作描述、参数、IP、执行结果与耗时到数据库，实现操作审计。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@Aspect
@Component
public class OperationLogAspect {

	private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

	/** 参数摘要最大长度，避免超长内容 */
	private static final int MAX_PARAM_LENGTH = 1000;

	private final OperationLogService operationLogService;

	public OperationLogAspect(OperationLogService operationLogService) {
		this.operationLogService = operationLogService;
	}

	/**
	 * 环绕通知，记录操作日志。
	 * @param joinPoint 连接点
	 * @return 目标方法返回值
	 * @throws Throwable 目标方法抛出的异常
	 */
	@Around("@annotation(com.tlcsdm.ecovault.annotation.OperationLogRecord)")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		OperationLog entity = new OperationLog();
		fillContext(entity, joinPoint);

		try {
			Object result = joinPoint.proceed();
			entity.setStatus("SUCCESS");
			return result;
		}
		catch (Throwable ex) {
			entity.setStatus("FAILURE");
			entity.setErrorMsg(truncate(ex.getMessage(), MAX_PARAM_LENGTH));
			throw ex;
		}
		finally {
			entity.setDurationMs(System.currentTimeMillis() - start);
			try {
				operationLogService.save(entity);
			}
			catch (Exception e) {
				// 日志记录失败不应影响主流程
				log.warn("保存操作日志失败: {}", e.getMessage());
			}
		}
	}

	/**
	 * 填充日志上下文信息。
	 * @param entity 日志实体
	 * @param joinPoint 连接点
	 */
	private void fillContext(OperationLog entity, ProceedingJoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		OperationLogRecord annotation = method.getAnnotation(OperationLogRecord.class);
		if (annotation != null) {
			entity.setModule(annotation.module());
			entity.setOperation(annotation.operation());
		}
		entity.setMethod(signature.getDeclaringType().getSimpleName() + "." + method.getName());
		entity.setParams(buildParams(joinPoint.getArgs()));

		// 当前登录用户
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
			entity.setUserId(securityUser.getId());
			entity.setUsername(securityUser.getUsername());
		}

		// 请求 IP
		HttpServletRequest request = currentRequest();
		if (request != null) {
			entity.setIp(WebUtil.getClientIp(request));
		}
	}

	/**
	 * 构建参数摘要，过滤敏感对象并限制长度。
	 * @param args 方法参数
	 * @return 参数摘要
	 */
	private String buildParams(Object[] args) {
		if (args == null || args.length == 0) {
			return "";
		}
		String params = Arrays.stream(args)
			.filter(arg -> !(arg instanceof HttpServletRequest))
			.map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
			.collect(Collectors.joining(", "));
		return truncate(params, MAX_PARAM_LENGTH);
	}

	private HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest();
		}
		return null;
	}

	private String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}

}

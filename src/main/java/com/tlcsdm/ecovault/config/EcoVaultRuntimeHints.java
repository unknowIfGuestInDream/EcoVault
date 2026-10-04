package com.tlcsdm.ecovault.config;

import org.apache.coyote.AbstractProtocol;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.lang.reflect.Method;

/**
 * EcoVault Native Image 运行时提示配置。
 *
 * <p>
 * GraalVM Native Image 下，Tomcat 在启动阶段会反射调用 {@link AbstractProtocol#getProperty(String)}
 * 读取协议配置。 若未显式注册该方法的反射调用权限，原生可执行文件启动时会抛出 {@code MissingReflectionRegistrationError}，导致
 * Web 服务器初始化失败。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
public class EcoVaultRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		try {
			Method getPropertyMethod = AbstractProtocol.class.getMethod("getProperty", String.class);
			hints.reflection().registerMethod(getPropertyMethod, ExecutableMode.INVOKE);
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException("Tomcat AbstractProtocol#getProperty(String) 方法不存在", e);
		}
	}

}

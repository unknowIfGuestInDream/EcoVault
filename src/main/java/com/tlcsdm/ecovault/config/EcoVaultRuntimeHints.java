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
 * GraalVM Native Image 下，Tomcat 在启动阶段会反射调用 {@link AbstractProtocol#getProperty(String)}、
 * {@link AbstractProtocol#setProperty(String, String)} 与
 * {@link AbstractProtocol#getName()} 读写并输出协议配置。若未显式注册这些方法的反射调用权限，原生可执行文件启动时会抛出
 * {@code MissingReflectionRegistrationError}，导致 Web 服务器初始化失败。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
public class EcoVaultRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		try {
			Method getPropertyMethod = AbstractProtocol.class.getMethod("getProperty", String.class);
			Method setPropertyMethod = AbstractProtocol.class.getMethod("setProperty", String.class, String.class);
			Method getNameMethod = AbstractProtocol.class.getMethod("getName");
			hints.reflection().registerMethod(getPropertyMethod, ExecutableMode.INVOKE);
			hints.reflection().registerMethod(setPropertyMethod, ExecutableMode.INVOKE);
			hints.reflection().registerMethod(getNameMethod, ExecutableMode.INVOKE);
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException("Tomcat AbstractProtocol 属性访问方法不存在", e);
		}
	}

}

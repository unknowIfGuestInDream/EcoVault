package com.tlcsdm.ecovault.config;

import org.apache.coyote.AbstractProtocol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Native 运行时提示配置测试。
 *
 * <p>
 * 验证 GraalVM Native Image 运行时已显式注册 Tomcat 启动所需的反射方法， 避免原生可执行文件启动时因缺少反射元数据而失败。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class EcoVaultRuntimeHintsTest {

	@Test
	@DisplayName("应注册 Tomcat AbstractProtocol 属性访问相关的反射调用提示")
	void shouldRegisterTomcatProtocolPropertyAccessHints() throws NoSuchMethodException {
		RuntimeHints hints = new RuntimeHints();

		new EcoVaultRuntimeHints().registerHints(hints, getClass().getClassLoader());

		Method getPropertyMethod = AbstractProtocol.class.getMethod("getProperty", String.class);
		Method setPropertyMethod = AbstractProtocol.class.getMethod("setProperty", String.class, String.class);
		Method getNameMethod = AbstractProtocol.class.getMethod("getName");
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(getPropertyMethod).test(hints)).isTrue();
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(setPropertyMethod).test(hints)).isTrue();
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(getNameMethod).test(hints)).isTrue();
	}

}

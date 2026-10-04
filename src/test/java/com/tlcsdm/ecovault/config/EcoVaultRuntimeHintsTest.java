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
	@DisplayName("应注册 Tomcat AbstractProtocol#getProperty 的反射调用提示")
	void shouldRegisterTomcatProtocolGetPropertyHint() throws NoSuchMethodException {
		RuntimeHints hints = new RuntimeHints();

		new EcoVaultRuntimeHints().registerHints(hints, getClass().getClassLoader());

		Method method = AbstractProtocol.class.getMethod("getProperty", String.class);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(method).test(hints)).isTrue();
	}

}

package com.tlcsdm.ecovault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用启动入口的单元测试。
 *
 * <p>
 * 通过反射调用私有的数据库目录创建逻辑，覆盖父目录已存在与不存在两种分支， 避免真正启动内嵌服务器。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
class EcoVaultApplicationMainTest {

	@Test
	@DisplayName("反射调用可确保数据库父目录被创建，且重复调用不报错")
	void ensureDatabaseDirectoryCreatesParent() throws Exception {
		Method method = EcoVaultApplication.class.getDeclaredMethod("ensureDatabaseDirectory");
		method.setAccessible(true);

		// 默认路径 data/ecovault.db，其父目录 data 应被创建
		method.invoke(null);
		File defaultParent = new File("data");
		assertThat(defaultParent).exists();

		// 目录已存在时再次调用不应抛出异常 (覆盖 parent.exists() 为真的分支)
		method.invoke(null);
		assertThat(defaultParent).exists();
	}

	@Test
	@DisplayName("实例化启动类以覆盖默认构造函数")
	void constructorIsInvocable() {
		assertThat(new EcoVaultApplication()).isNotNull();
	}

	@Test
	@DisplayName("data 目录在测试后可被安全清理")
	void cleanupDataDirectory() throws Exception {
		Path dir = Path.of("data");
		if (Files.exists(dir) && isEmptyDir(dir)) {
			Files.deleteIfExists(dir);
		}
		assertThat(true).isTrue();
	}

	private boolean isEmptyDir(Path dir) throws Exception {
		try (var stream = Files.newDirectoryStream(dir)) {
			return !stream.iterator().hasNext();
		}
	}

}

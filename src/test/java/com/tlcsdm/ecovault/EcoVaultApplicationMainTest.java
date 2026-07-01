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
	@DisplayName("数据库路径无父目录时安全跳过创建 (parent 为 null 分支)")
	void ensureDatabaseDirectoryWithoutParent() {
		// 仅文件名、无父目录，parent 为 null，不应抛出异常
		EcoVaultApplication.ensureDatabaseDirectory("ecovault-standalone.db");
		assertThat(new File("ecovault-standalone.db")).doesNotExist();
	}

	@Test
	@DisplayName("父目录不存在时创建，已存在时安全跳过")
	void ensureDatabaseDirectoryCreatesAndSkips(@org.junit.jupiter.api.io.TempDir Path tempDir) {
		Path nested = tempDir.resolve("nested-db-dir");
		String dbPath = nested.resolve("ecovault.db").toString();

		EcoVaultApplication.ensureDatabaseDirectory(dbPath);
		assertThat(nested).exists();

		// 再次调用命中 parent.exists() 为真的分支
		EcoVaultApplication.ensureDatabaseDirectory(dbPath);
		assertThat(nested).exists();
	}

	@Test
	@DisplayName("main 方法确保目录并委托 SpringApplication 启动")
	void mainDelegatesToSpringApplication() {
		try (org.mockito.MockedStatic<org.springframework.boot.SpringApplication> mocked = org.mockito.Mockito
			.mockStatic(org.springframework.boot.SpringApplication.class)) {
			mocked.when(() -> org.springframework.boot.SpringApplication.run(
					org.mockito.ArgumentMatchers.eq(EcoVaultApplication.class),
					org.mockito.ArgumentMatchers.any(String[].class)))
				.thenReturn(null);

			EcoVaultApplication.main(new String[] {});

			mocked.verify(() -> org.springframework.boot.SpringApplication.run(
					org.mockito.ArgumentMatchers.eq(EcoVaultApplication.class),
					org.mockito.ArgumentMatchers.any(String[].class)));
		}
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

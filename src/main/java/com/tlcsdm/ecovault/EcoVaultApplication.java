package com.tlcsdm.ecovault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

/**
 * EcoVault 应用启动入口。
 *
 * <p>
 * EcoVault (生态保险箱) 是一个个人数据安全存储与智能管理平台， 提供用户管理、密码管理、财务(工资)数据管理、操作日志审计与管理后台等功能。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
@SpringBootApplication
public class EcoVaultApplication {

	/**
	 * 应用主入口。
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		ensureDatabaseDirectory();
		SpringApplication.run(EcoVaultApplication.class, args);
	}

	/**
	 * 确保 SQLite 数据库文件所在目录存在。
	 *
	 * <p>
	 * SQLite 不会自动创建父级目录，若目录缺失会导致启动失败，故在此提前创建。 数据库路径可通过环境变量 {@code ECOVAULT_DB_PATH}
	 * 覆盖，默认为 {@code data/ecovault.db}。
	 * </p>
	 */
	private static void ensureDatabaseDirectory() {
		String dbPath = System.getenv().getOrDefault("ECOVAULT_DB_PATH", "data/ecovault.db");
		ensureDatabaseDirectory(dbPath);
	}

	/**
	 * 根据数据库路径确保其父级目录存在。
	 *
	 * <p>
	 * 抽取为包级可见的重载方法，便于对「无父目录」「父目录已存在」「父目录待创建」等分支进行单元测试。
	 * </p>
	 * @param dbPath 数据库文件路径
	 */
	static void ensureDatabaseDirectory(String dbPath) {
		File dbFile = new File(dbPath);
		File parent = dbFile.getParentFile();
		if (parent != null && !parent.exists()) {
			// 目录创建失败不抛异常，交由数据源初始化时给出明确错误
			parent.mkdirs();
		}
	}

}

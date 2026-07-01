package com.tlcsdm.ecovault.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.community.dialect.identity.SQLiteIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;

/**
 * EcoVault 定制 SQLite 方言。
 *
 * <p>
 * Hibernate 7.x 社区版 {@link SQLiteDialect} 在导出建表语句时存在一个缺陷： {@code ColumnDefinitions}
 * 会在自增主键列后追加身份类型串 {@code "integer"}，但其判断条件是
 * "当前已累积的整段列定义中不包含该串"。由于该判断针对的是整张表已生成的列缓冲区（并非当前列本身）， 一旦实体中主键列之前存在其它 {@code integer}
 * 类型的列（例如 {@code year}、{@code month}、 {@code strength_score}），主键列就会被误判为"已包含
 * integer"，从而漏掉类型声明， 生成形如 {@code id,}（无类型）的定义。SQLite 只有在列类型精确为 {@code INTEGER} 且为主键时
 * 才会把该列作为 rowid 别名实现自增，缺失类型会导致主键无法自增、插入后主键为 {@code NULL}， 进而使查询结果被反序列化为
 * {@code null}。受影响的实体为 {@code password_entries} 与 {@code salary_records}。
 * </p>
 *
 * <p>
 * 缺陷判断使用 {@code buffer.toLowerCase(ROOT).contains(identityColumnString)}，缓冲区在比较前
 * 已被转为小写。因此本方言将身份类型串改为大写 {@code "INTEGER"}：小写化后的缓冲区永远不会包含大写串， 类型声明得以稳定追加；同时 SQLite
 * 对类型名大小写不敏感，{@code INTEGER
 * PRIMARY KEY} 仍会作为 rowid 别名 正常自增，从而修复所有实体的主键生成。
 * </p>
 *
 * @author unknowIfGuestInDream
 */
public class EcoVaultSQLiteDialect extends SQLiteDialect {

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return EcoVaultIdentityColumnSupport.INSTANCE;
	}

	/**
	 * 修复自增主键类型缺失的身份列支持实现。
	 */
	static final class EcoVaultIdentityColumnSupport extends SQLiteIdentityColumnSupport {

		static final EcoVaultIdentityColumnSupport INSTANCE = new EcoVaultIdentityColumnSupport();

		@Override
		public String getIdentityColumnString(int type) {
			// 使用大写 INTEGER 规避 Hibernate 建表时的小写 contains 误判，确保类型串稳定输出。
			return "INTEGER";
		}

	}

}

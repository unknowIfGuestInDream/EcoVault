/**
 * 工具类包。
 *
 * <p>
 * 本包提供 EcoVault 应用的通用工具类。
 * </p>
 *
 * <h2>主要工具类</h2>
 * <ul>
 * <li>{@link com.tlcsdm.ecovault.utils.EncryptionUtil} - 加密工具类，提供 AES-GCM 加解密</li>
 * <li>日期工具类 - 日期格式化、转换</li>
 * <li>字符串工具类 - 字符串处理、验证</li>
 * <li>IP 工具类 - IP 地址获取与解析</li>
 * </ul>
 *
 * <h2>加密工具</h2>
 * <p>
 * {@link com.tlcsdm.ecovault.utils.EncryptionUtil} 提供 AES-GCM 加密和解密功能， 用于保护密码条目中的敏感密码字段。
 * </p>
 *
 * <h2>使用示例</h2> <pre>{@code
 * // 加密密码
 * String encryptedPassword = EncryptionUtil.encrypt("myPassword123");
 *
 * // 解密密码
 * String decryptedPassword = EncryptionUtil.decrypt(encryptedPassword);
 * }</pre>
 *
 * @see com.tlcsdm.ecovault.security
 * @author unknowIfGuestInDream
 * @version 1.0.1
 * @since 1.0.0
 */
package com.tlcsdm.ecovault.utils;

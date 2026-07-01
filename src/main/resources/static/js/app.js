/* EcoVault 前端公共脚本：主题切换、CSRF、API 封装、提示 */
(function () {
    "use strict";

    // 初始化主题 (读取本地存储)
    const savedTheme = localStorage.getItem("ecovault-theme") || "light";
    document.documentElement.setAttribute("data-theme", savedTheme);

    /** 切换暗/亮主题 */
    window.toggleTheme = function () {
        const current = document.documentElement.getAttribute("data-theme");
        const next = current === "dark" ? "light" : "dark";
        document.documentElement.setAttribute("data-theme", next);
        localStorage.setItem("ecovault-theme", next);
        const btn = document.querySelector(".theme-toggle");
        if (btn) {
            btn.textContent = next === "dark" ? "☀️" : "🌙";
        }
    };

    /**
     * 从 Cookie 读取指定名称的值。
     * @param {string} name Cookie 名
     * @returns {string|null} 值
     */
    function getCookie(name) {
        const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
        return match ? decodeURIComponent(match[2]) : null;
    }

    /**
     * 封装的 API 请求，自动携带 CSRF 令牌。
     * @param {string} url 请求地址
     * @param {object} options fetch 选项
     * @returns {Promise<any>} 响应数据
     */
    window.api = async function (url, options = {}) {
        const opts = Object.assign({ headers: {}, credentials: "same-origin" }, options);
        opts.headers = Object.assign({ "Content-Type": "application/json" }, opts.headers);
        const csrf = getCookie("XSRF-TOKEN");
        if (csrf && !["GET", "HEAD", "OPTIONS"].includes((opts.method || "GET").toUpperCase())) {
            opts.headers["X-XSRF-TOKEN"] = csrf;
        }
        const resp = await fetch(url, opts);
        if (resp.status === 401) {
            window.location.href = "/login";
            throw new Error("未登录");
        }
        const contentType = resp.headers.get("content-type") || "";
        if (!contentType.includes("application/json")) {
            if (!resp.ok) {
                throw new Error("请求失败: " + resp.status);
            }
            return resp;
        }
        const body = await resp.json();
        if (body.code !== 0) {
            throw new Error(body.message || "请求失败");
        }
        return body.data;
    };

    /**
     * 弹出提示。
     * @param {string} message 消息
     * @param {string} type success | error
     */
    window.toast = function (message, type = "success") {
        let el = document.querySelector(".toast");
        if (!el) {
            el = document.createElement("div");
            el.className = "toast";
            document.body.appendChild(el);
        }
        el.textContent = message;
        el.className = "toast " + type;
        requestAnimationFrame(() => el.classList.add("show"));
        setTimeout(() => el.classList.remove("show"), 2800);
    };

    /** 退出登录 */
    window.logout = async function () {
        try {
            await window.api("/api/auth/logout", { method: "POST" });
        } catch (e) {
            // 忽略
        }
        window.location.href = "/login";
    };

    /** HTML 转义，防止 XSS */
    window.escapeHtml = function (value) {
        if (value === null || value === undefined) {
            return "";
        }
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    };
})();

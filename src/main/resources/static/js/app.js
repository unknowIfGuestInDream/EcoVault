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

    /**
     * 复制文本到剪贴板。
     * @param {string} text 待复制内容
     * @returns {Promise<void>} 复制结果
     */
    window.copyText = async function (text) {
        if (!text) {
            throw new Error("没有可复制的内容");
        }
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return;
        }
        const input = document.createElement("textarea");
        input.value = text;
        input.setAttribute("readonly", "readonly");
        input.style.position = "fixed";
        input.style.left = "-9999px";
        document.body.appendChild(input);
        input.select();
        document.execCommand("copy");
        document.body.removeChild(input);
    };

    /**
     * 站内确认弹窗。
     * @param {object} options 弹窗选项
     * @returns {Promise<boolean>} 是否确认
     */
    window.confirmDialog = function (options = {}) {
        return new Promise((resolve) => {
            let modal = document.getElementById("global-confirm-modal");
            if (!modal) {
                modal = document.createElement("div");
                modal.id = "global-confirm-modal";
                modal.className = "modal-backdrop";
                modal.innerHTML = `
                    <div class="glass modal confirm-modal">
                        <div class="modal-header">
                            <h3 id="confirm-title">请确认操作</h3>
                            <button class="icon-btn modal-close" id="confirm-close" type="button" aria-label="关闭">✕</button>
                        </div>
                        <p class="muted" id="confirm-message">确认继续执行当前操作吗？</p>
                        <div class="confirm-actions">
                            <button class="btn secondary" id="confirm-cancel" type="button">取消</button>
                            <button class="btn danger" id="confirm-ok" type="button">确认</button>
                        </div>
                    </div>`;
                document.body.appendChild(modal);
            }

            const titleEl = document.getElementById("confirm-title");
            const messageEl = document.getElementById("confirm-message");
            const closeEl = document.getElementById("confirm-close");
            const cancelEl = document.getElementById("confirm-cancel");
            const okEl = document.getElementById("confirm-ok");

            titleEl.textContent = options.title || "请确认操作";
            messageEl.textContent = options.message || "确认继续执行当前操作吗？";
            cancelEl.textContent = options.cancelText || "取消";
            okEl.textContent = options.confirmText || "确认";

            const cleanup = (confirmed) => {
                modal.classList.remove("show");
                modal.onclick = null;
                closeEl.onclick = null;
                cancelEl.onclick = null;
                okEl.onclick = null;
                resolve(confirmed);
            };

            modal.onclick = (event) => {
                if (event.target === modal) {
                    cleanup(false);
                }
            };
            closeEl.onclick = () => cleanup(false);
            cancelEl.onclick = () => cleanup(false);
            okEl.onclick = () => cleanup(true);
            modal.classList.add("show");
        });
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

    /**
     * 初始化导航栏：根据当前用户可访问的页面 (me.pages) 显示/隐藏菜单项与分组。
     * @returns {Promise<object|null>} 当前用户信息
     */
    window.initNav = async function () {
        const nav = document.getElementById("main-nav");
        if (!nav) {
            return null;
        }
        try {
            const me = await window.api("/api/auth/me");
            const pages = new Set(me.pages || []);
            // 控制台与个人中心对所有登录用户开放，其余按权限显示
            nav.querySelectorAll("[data-page]").forEach((el) => {
                const key = el.getAttribute("data-page");
                if (key === "dashboard" || key === "profile") {
                    return;
                }
                el.style.display = pages.has(key) ? "" : "none";
            });
            // 分组下拉：无任何可见子项时隐藏整个分组
            nav.querySelectorAll(".nav-dropdown").forEach((group) => {
                const links = group.querySelectorAll(".nav-dropdown-menu [data-page]");
                const anyVisible = Array.from(links).some((a) => a.style.display !== "none");
                group.style.display = anyVisible ? "" : "none";
            });
            return me;
        } catch (e) {
            return null;
        }
    };

    // 主题按钮初始文案
    document.addEventListener("DOMContentLoaded", function () {
        const btn = document.querySelector(".theme-toggle");
        if (btn) {
            btn.textContent = savedTheme === "dark" ? "☀️" : "🌙";
        }
        if (document.getElementById("main-nav")) {
            window.initNav();
        }
    });
})();

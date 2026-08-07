/* EcoVault 隐私模式脚本：一键隐藏页面内容并需密码解锁 */
(function () {
    "use strict";

    const PRIVACY_KEY = "ecovault-privacy";

    function isPrivacyOn() {
        return localStorage.getItem(PRIVACY_KEY) === "on";
    }

    function hasNavbar() {
        return !!document.getElementById("main-nav");
    }

    function buildPrivacyOverlay() {
        let overlay = document.getElementById("privacy-overlay");
        if (overlay) {
            return overlay;
        }
        overlay = document.createElement("div");
        overlay.id = "privacy-overlay";
        overlay.className = "privacy-overlay";
        overlay.innerHTML = `
            <div class="glass privacy-card">
                <div class="privacy-emoji">🕶️</div>
                <h2>隐私模式已开启</h2>
                <p class="muted">页面内容已隐藏，请输入当前账户登录密码以继续访问。</p>
                <form id="privacy-form" autocomplete="off">
                    <input type="password" id="privacy-password" placeholder="登录密码"
                        autocomplete="off" aria-label="登录密码"/>
                    <button class="btn" type="submit" id="privacy-unlock">解锁</button>
                </form>
                <p class="privacy-error" id="privacy-error" role="alert"></p>
            </div>`;
        document.body.appendChild(overlay);
        overlay.querySelector("#privacy-form").addEventListener("submit", (event) => {
            event.preventDefault();
            unlockPrivacy();
        });
        return overlay;
    }

    function showPrivacyOverlay() {
        const overlay = buildPrivacyOverlay();
        document.documentElement.setAttribute("data-privacy", "on");
        overlay.classList.add("show");
        const errorEl = document.getElementById("privacy-error");
        if (errorEl) {
            errorEl.textContent = "";
        }
        const input = document.getElementById("privacy-password");
        if (input) {
            input.value = "";
            requestAnimationFrame(() => input.focus());
        }
    }

    function hidePrivacyOverlay() {
        const overlay = document.getElementById("privacy-overlay");
        if (overlay) {
            overlay.classList.remove("show");
        }
        document.documentElement.removeAttribute("data-privacy");
    }

    async function unlockPrivacy() {
        const input = document.getElementById("privacy-password");
        const errorEl = document.getElementById("privacy-error");
        const button = document.getElementById("privacy-unlock");
        const password = input ? input.value : "";
        if (!password) {
            if (errorEl) {
                errorEl.textContent = "请输入登录密码";
            }
            return;
        }
        if (button) {
            button.disabled = true;
        }
        try {
            await window.api("/api/auth/verify-password", {
                method: "POST",
                body: JSON.stringify({ password })
            });
            localStorage.removeItem(PRIVACY_KEY);
            if (input) {
                input.value = "";
            }
            hidePrivacyOverlay();
            window.toast("已退出隐私模式");
        }
        catch (e) {
            if (errorEl) {
                errorEl.textContent = e.message || "密码错误";
            }
            if (input) {
                input.value = "";
                input.focus();
            }
        }
        finally {
            if (button) {
                button.disabled = false;
            }
        }
    }

    window.togglePrivacy = function () {
        if (!hasNavbar()) {
            return;
        }
        localStorage.setItem(PRIVACY_KEY, "on");
        showPrivacyOverlay();
    };

    if (hasNavbar() && isPrivacyOn()) {
        showPrivacyOverlay();
    }

    // F12 快捷键：在已登录页面快速进入隐私模式（仅当隐私模式未激活时触发）
    document.addEventListener("keydown", (e) => {
        if (e.key === "F12" && hasNavbar() && !isPrivacyOn()) {
            window.togglePrivacy();
        }
    });
})();

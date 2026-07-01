package com.tlcsdm.ecovault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlcsdm.ecovault.dto.LoginRequest;
import com.tlcsdm.ecovault.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用集成测试：验证上下文加载、注册登录流程与 RBAC 权限控制。
 *
 * @author 梦里不知身是客
 */
@SpringBootTest
class EcoVaultApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Spring 上下文正常加载")
    void contextLoads() {
    }

    @Test
    @DisplayName("注册后可成功登录并返回令牌")
    void registerThenLogin() throws Exception {
        RegisterRequest register = new RegisterRequest("ituser", "Passw0rd!", "集成用户", "it@ecovault.com");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        LoginRequest login = new LoginRequest("ituser", "Passw0rd!");
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("普通用户访问管理接口返回 403")
    @WithMockUser(username = "user", roles = "USER")
    void userForbiddenOnAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员可访问管理接口")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("未认证访问受保护接口返回 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/passwords"))
                .andExpect(status().isUnauthorized());
    }
}

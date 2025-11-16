package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.user.UserResponse;
import com.example.bankcards.dto.user.UserUpdateRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    private UserResponse userResponse;
    private UserUpdateRequest updateRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        userResponse = new UserResponse(
                1L,
                "testuser",
                "test@example.com",
                "Test",
                "User",
                "USER",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        updateRequest = new UserUpdateRequest();
        updateRequest.setEmail("newemail@example.com");
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");

        when(userDetailsService.loadUserEntityByUsername(anyString())).thenReturn(testUser);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        List<UserResponse> users = Collections.singletonList(userResponse);
        Page<UserResponse> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_ForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCurrentUser_Success() throws Exception {
        when(userService.updateUser(anyLong(), any(UserUpdateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void updateCurrentUser_InvalidEmail() throws Exception {
        updateRequest.setEmail("invalid-email");

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_Success() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateUser_ForbiddenForUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(eq(1L));

        mockMvc.perform(delete("/api/v1/users/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_ForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleUserStatus_Success() throws Exception {
        UserResponse disabledUser = new UserResponse(
                1L, "testuser", "test@example.com", "Test", "User",
                "USER", false, LocalDateTime.now(), LocalDateTime.now()
        );

        when(userService.toggleUserStatus(eq(1L))).thenReturn(disabledUser);

        mockMvc.perform(patch("/api/v1/users/1/toggle-status")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toggleUserStatus_ForbiddenForUser() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1/toggle-status")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void updateCurrentUser_EmptyRequestBody() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateCurrentUser_PartialUpdate() throws Exception {
        UserUpdateRequest partial = new UserUpdateRequest();
        partial.setFirstName("OnlyFirstName");

        when(userService.updateUser(anyLong(), any(UserUpdateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partial)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
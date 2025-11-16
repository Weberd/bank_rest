package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.contracts.TransferServiceInterface;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferServiceInterface transferService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    private TransferRequest transferRequest;
    private TransferResponse transferResponse;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100.50));
        transferRequest.setDescription("Test transfer");

        transferResponse = new TransferResponse(
                1L,
                1L,
                "**** **** **** 1234",
                2L,
                "**** **** **** 5678",
                BigDecimal.valueOf(100.50),
                "COMPLETED",
                "Test transfer",
                1L,
                LocalDateTime.now()
        );

        when(userDetailsService.loadUserEntityByUsername(anyString())).thenReturn(testUser);
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_Success() throws Exception {
        when(transferService.executeTransfer(any(TransferRequest.class), anyLong()))
                .thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fromCardId").value(1))
                .andExpect(jsonPath("$.toCardId").value(2))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromCardNumberMasked").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.toCardNumberMasked").value("**** **** **** 5678"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_MissingFromCardId() throws Exception {
        transferRequest.setFromCardId(null);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fromCardId").value("Source card ID is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_MissingToCardId() throws Exception {
        transferRequest.setToCardId(null);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.toCardId").value("Destination card ID is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_MissingAmount() throws Exception {
        transferRequest.setAmount(null);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Amount is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_NegativeAmount() throws Exception {
        transferRequest.setAmount(BigDecimal.valueOf(-100));

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Amount must be greater than 0"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_ZeroAmount() throws Exception {
        transferRequest.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyTransfers_Success() throws Exception {
        List<TransferResponse> transfers = Collections.singletonList(transferResponse);
        Page<TransferResponse> page = new PageImpl<>(transfers, PageRequest.of(0, 10), 1);

        when(transferService.getUserTransfers(anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transfers/my")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyTransfers_WithCustomPagination() throws Exception {
        List<TransferResponse> transfers = Collections.singletonList(transferResponse);
        Page<TransferResponse> page = new PageImpl<>(transfers, PageRequest.of(1, 20), 50);

        when(transferService.getUserTransfers(anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transfers/my")
                        .param("page", "1")
                        .param("size", "20")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalElements").value(50));
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_WithDescription() throws Exception {
        transferRequest.setDescription("Payment for services");

        when(transferService.executeTransfer(any(TransferRequest.class), anyLong()))
                .thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void executeTransfer_WithoutDescription() throws Exception {
        transferRequest.setDescription(null);

        when(transferService.executeTransfer(any(TransferRequest.class), anyLong()))
                .thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void executeTransfer_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyTransfers_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/transfers/my")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void executeTransfer_EmptyRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @WithMockUser
    void executeTransfer_LargeAmount() throws Exception {
        transferRequest.setAmount(new BigDecimal("999999999.99"));

        when(transferService.executeTransfer(any(TransferRequest.class), anyLong()))
                .thenReturn(transferResponse);

        mockMvc.perform(post("/api/v1/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }
}
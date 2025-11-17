package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.controller.user.UserCardController;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardStatusUpdateRequest;
import com.example.bankcards.dto.card.CardUpdateRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardCommandService;
import com.example.bankcards.service.CardQueryService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardCommandService cardCommandService;

    @MockBean
    private CardQueryService cardQueryService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    private CardResponse cardResponse;
    private CardCreateRequest createRequest;
    private CardUpdateRequest updateRequest;
    private CardStatusUpdateRequest statusUpdateRequest;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        cardResponse = new CardResponse(
                1L,
                "**** **** **** 1234",
                "JOHN DOE",
                LocalDate.now().plusYears(3),
                "ACTIVE",
                BigDecimal.valueOf(1000.00),
                1L,
                "testuser",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        createRequest = new CardCreateRequest();
        createRequest.setCardNumber("1234567890123456");
        createRequest.setCardHolder("JOHN DOE");
        createRequest.setExpiryDate(LocalDate.now().plusYears(3));
        createRequest.setCvv("123");
        createRequest.setUserId(1L);
        createRequest.setInitialBalance(BigDecimal.valueOf(1000.00));

        updateRequest = new CardUpdateRequest();
        updateRequest.setCardHolder("JANE DOE");

        statusUpdateRequest = new CardStatusUpdateRequest();
        statusUpdateRequest.setStatus("BLOCKED");
        statusUpdateRequest.setReason("Lost card");

        when(userDetailsService.loadUserEntityByUsername(anyString())).thenReturn(testUser);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyCards_Success() throws Exception {
        List<CardResponse> cards = Collections.singletonList(cardResponse);
        Page<CardResponse> page = new PageImpl<>(cards, PageRequest.of(0, 10), 1);

        when(cardQueryService.getUserCards(anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/user/cards")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockCard_Success() throws Exception {
        when(cardCommandService.updateCardStatus(eq(1L), any(CardStatusUpdateRequest.class), anyLong()))
                .thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/user/cards/1/status")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.controller.admin.AdminCardController;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AdminCardControllerTest {

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
    @WithMockUser(roles = "ADMIN")
    void createCard_Success() throws Exception {
        when(cardCommandService.createCard(any(CardCreateRequest.class))).thenReturn(cardResponse);

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumberMasked").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.cardHolder").value("JOHN DOE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCard_ForbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_InvalidCardNumber() throws Exception {
        createRequest.setCardNumber("12345");

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cardNumber").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_InvalidCvv() throws Exception {
        createRequest.setCvv("12");

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cvv").value("CVV must be 3 or 4 digits"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_MissingRequiredFields() throws Exception {
        CardCreateRequest emptyRequest = new CardCreateRequest();

        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cardNumber").exists())
                .andExpect(jsonPath("$.errors.cardHolder").exists())
                .andExpect(jsonPath("$.errors.cvv").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_Success() throws Exception {
        List<CardResponse> cards = Collections.singletonList(cardResponse);
        Page<CardResponse> page = new PageImpl<>(cards);

        when(cardQueryService.getAllCards(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllCards_ForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cards")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_Success() throws Exception {
        when(cardCommandService.updateCard(eq(1L), any(CardUpdateRequest.class), anyLong()))
                .thenReturn(cardResponse);

        mockMvc.perform(put("/api/v1/admin/cards/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCardStatus_Success() throws Exception {
        when(cardCommandService.updateCardStatus(eq(1L), any(CardStatusUpdateRequest.class), anyLong()))
                .thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/admin/cards/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCardStatus_InvalidStatus() throws Exception {
        statusUpdateRequest.setStatus("INVALID_STATUS");

        mockMvc.perform(patch("/api/v1/admin/cards/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_Success() throws Exception {
        doNothing().when(cardCommandService).deleteCard(eq(1L), anyLong());

        mockMvc.perform(delete("/api/v1/admin/cards/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCard_ForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/cards/1")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void createCard_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
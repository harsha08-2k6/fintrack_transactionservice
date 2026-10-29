package com.spendwise.transaction.controller;

import com.spendwise.transaction.config.InternalGatewaySecurityFilter;
import com.spendwise.transaction.entity.Transaction;
import com.spendwise.transaction.entity.TransactionType;
import com.spendwise.transaction.repository.TransactionRepository;
import com.spendwise.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(InternalGatewaySecurityFilter.class)
@TestPropertySource(properties = {
        "fintrack.internal.gateway-key=secret-internal-key-test"
})
class TransactionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Direct access without internal gateway key returns 403 Forbidden")
    void testDirectAccessWithoutGatewayKeyRejected() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("X-User-Id", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Request with valid gateway key but missing X-User-Id returns 401 Unauthorized")
    void testMissingUserIdReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("X-Internal-Gateway-Key", "secret-internal-key-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cross-tenant tampering: user accessing another user's transaction returns 403 Forbidden")
    void testCrossTenantTamperingReturns403() throws Exception {
        when(transactionService.getTransactionById(10L, 999L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to transaction"));

        mockMvc.perform(get("/api/transactions/999")
                        .header("X-Internal-Gateway-Key", "secret-internal-key-test")
                        .header("X-User-Id", "10"))
                .andExpect(status().isForbidden());
    }
}


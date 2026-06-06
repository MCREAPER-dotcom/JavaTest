package com.example.wallet;

import com.example.wallet.config.ErrorResponse;
import com.example.wallet.dto.OperationType;
import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class WalletIntegrationTest {
    
    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("wallet_db")
            .withUsername("wallet_user")
            .withPassword("wallet_password");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        walletId = UUID.randomUUID();
        walletRepository.save(new Wallet(walletId, BigDecimal.valueOf(1000)));
    }

    @Test
    void testDepositSuccess() {
        var request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, BigDecimal.valueOf(500));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet", request, Void.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Wallet updated = walletRepository.findById(walletId).orElseThrow();
        assertEquals(BigDecimal.valueOf(1500), updated.getBalance());
    }

    @Test
    void testWithdrawSuccess() {
        var request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, BigDecimal.valueOf(600));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet", request, Void.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Wallet updated = walletRepository.findById(walletId).orElseThrow();
        assertEquals(BigDecimal.valueOf(400), updated.getBalance());
    }

    @Test
    void testInsufficientFunds() {
        var request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, BigDecimal.valueOf(2000));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/wallet", request, ErrorResponse.class);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("Недостаточно средств"));
    }

    @Test
    void testWalletNotFound() {
        var request = new WalletOperationRequest(UUID.randomUUID(), OperationType.DEPOSIT, BigDecimal.valueOf(100));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/wallet", request, ErrorResponse.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testInvalidJson() {
        String invalidJson = "{\"walletId\": \"not-a-uuid\", \"operationType\": \"DEPOSIT\", \"amount\": 100}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(invalidJson, headers);
        
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/wallet", entity, ErrorResponse.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHighConcurrencyNoLostUpdates() throws InterruptedException {
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    var request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, BigDecimal.valueOf(10));
                    restTemplate.postForEntity("/api/v1/wallet", request, Void.class);
                } catch (Exception e) {
                    fail("Не должно быть исключений при конкурентном доступе: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(BigDecimal.ZERO, finalWallet.getBalance());
    }
}
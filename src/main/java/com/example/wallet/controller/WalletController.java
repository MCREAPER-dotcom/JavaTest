package com.example.wallet.controller;

import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/wallet")
    public ResponseEntity<Void> processOperation(@Valid @RequestBody WalletOperationRequest request) {
        walletService.processOperation(request.walletId(), request.operationType(), request.amount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getWallet(walletId));
    }
}
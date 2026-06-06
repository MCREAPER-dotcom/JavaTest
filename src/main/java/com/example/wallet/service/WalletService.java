package com.example.wallet.service;

import com.example.wallet.dto.OperationType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public void processOperation(UUID walletId, OperationType operationType, BigDecimal amount) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException(walletId);
        }
        
        BigDecimal delta = (operationType == OperationType.DEPOSIT) ? amount : amount.negate();
        
        int updatedRows = walletRepository.updateBalanceAtomic(walletId, delta);
        
        if (updatedRows == 0) {
            // Кошелек есть, но не хватило средств
            throw new InsufficientFundsException(walletId);
        }
    }

    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
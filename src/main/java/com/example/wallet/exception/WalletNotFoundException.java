package com.example.wallet.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(UUID id) { 
        super("Кошелек не найден: " + id); 
    }
}
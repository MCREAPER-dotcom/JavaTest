package com.example.wallet.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID id) { 
        super("Недостаточно средств на кошельке: " + id); 
    }
}
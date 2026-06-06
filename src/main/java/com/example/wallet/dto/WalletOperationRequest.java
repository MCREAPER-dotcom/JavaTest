package com.example.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest(
    @NotNull(message = "walletId не может быть null") UUID walletId,
    @NotNull(message = "operationType не может быть null") OperationType operationType,
    @NotNull @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0") BigDecimal amount
) {}
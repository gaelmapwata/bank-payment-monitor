package com.bankpaymentmonitor.paymentservice.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentCreateDTO(

        @NotBlank(message = "Le système source est obligatoire")
        @Size(max = 50, message = "Le système source ne peut pas dépasser 50 caractères")
        String sourceSystem,

        @NotBlank(message = "La référence du paiement source est obligatoire")
        @Size(max = 100, message = "La référence source ne peut pas dépasser 100 caractères")
        String sourcePaymentReference,

        @NotBlank(message = "Le code de l'agence est obligatoire")
        @Size(max = 30, message = "Le code de l'agence ne peut pas dépasser 30 caractères")
        String branchCode,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.00", inclusive = false,
                message = "Le montant doit être supérieur à zéro")
        BigDecimal amount,

        @NotBlank(message = "La devise est obligatoire")
        @Pattern(regexp = "^[A-Z]{3}$",
                message = "La devise doit contenir exactement trois lettres majuscules")
        String currency
) {
}
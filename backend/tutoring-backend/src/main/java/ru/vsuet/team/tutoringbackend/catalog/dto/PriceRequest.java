package ru.vsuet.team.tutoringbackend.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PriceRequest(@NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") BigDecimal pricePerHour) {}

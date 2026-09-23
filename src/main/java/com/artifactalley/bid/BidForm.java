package com.artifactalley.bid;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BidForm {
    @NotNull(message = "Enter a bid amount.")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than zero.")
    @Digits(integer = 10, fraction = 2, message = "Use an amount with no more than two decimal places.")
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

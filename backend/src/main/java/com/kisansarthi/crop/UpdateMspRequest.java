package com.kisansarthi.crop;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class UpdateMspRequest {

    @NotNull
    @Positive
    private BigDecimal standardMsp;

    private BigDecimal bonus = BigDecimal.ZERO;

    public UpdateMspRequest() {}

    public BigDecimal getStandardMsp() { return standardMsp; }
    public void setStandardMsp(BigDecimal standardMsp) { this.standardMsp = standardMsp; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
}

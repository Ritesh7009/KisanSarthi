package com.kisansarthi.booking;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "mandi_token_sequences")
@IdClass(MandiTokenSequence.TokenSequenceId.class)
public class MandiTokenSequence {

    @Id
    @Column(name = "mandi_id", length = 50)
    private String mandiId;

    @Id
    @Column(name = "procurement_date")
    private LocalDate procurementDate;

    @Column(name = "current_sequence", nullable = false)
    private int currentSequence = 0;

    public MandiTokenSequence() {}

    public MandiTokenSequence(String mandiId, LocalDate procurementDate, int currentSequence) {
        this.mandiId = mandiId;
        this.procurementDate = procurementDate;
        this.currentSequence = currentSequence;
    }

    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }
    public LocalDate getProcurementDate() { return procurementDate; }
    public void setProcurementDate(LocalDate procurementDate) { this.procurementDate = procurementDate; }
    public int getCurrentSequence() { return currentSequence; }
    public void setCurrentSequence(int currentSequence) { this.currentSequence = currentSequence; }

    public static class TokenSequenceId implements Serializable {
        private String mandiId;
        private LocalDate procurementDate;

        public TokenSequenceId() {}

        public TokenSequenceId(String mandiId, LocalDate procurementDate) {
            this.mandiId = mandiId;
            this.procurementDate = procurementDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenSequenceId that = (TokenSequenceId) o;
            return Objects.equals(mandiId, that.mandiId) && Objects.equals(procurementDate, that.procurementDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mandiId, procurementDate);
        }
    }
}

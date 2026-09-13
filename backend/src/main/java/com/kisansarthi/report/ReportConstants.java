package com.kisansarthi.report;

import java.math.BigDecimal;

public final class ReportConstants {

    private ReportConstants() {}

    /**
     * Official Government of India / MP e-Uparjan Fair Average Quality (FAQ) default moisture threshold.
     */
    public static final BigDecimal DEFAULT_FAQ_MOISTURE_LIMIT_PCT = new BigDecimal("12.00");
    public static final double DEFAULT_FAQ_MOISTURE_LIMIT_DOUBLE = 12.0;

    /**
     * Bounded limit for delayed payment escalation alerts returned in dashboards.
     */
    public static final int MAX_DELAYED_PAYMENT_ALERTS = 50;

    /**
     * Batch size for memory-bounded streaming CSV export.
     */
    public static final int CSV_EXPORT_BATCH_SIZE = 500;

    /**
     * Max allowed page size for procurement register pagination queries.
     */
    public static final int MAX_REGISTER_PAGE_SIZE = 200;
}

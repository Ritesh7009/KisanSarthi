package com.kisansarthi.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class CsvStreamWriter {

    private final Writer writer;

    public CsvStreamWriter(OutputStream outputStream) {
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    }

    public void writeHeader(String... headers) throws IOException {
        writeRow(headers);
    }

    public void writeRow(String... columns) throws IOException {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            writer.write(escape(columns[i]));
        }
        writer.write("\n");
        writer.flush();
    }

    public void writeProcurementRow(ProcurementRegisterRowDto r) throws IOException {
        writeRow(
                r.getTokenNumber(),
                r.getScheduledDate(),
                r.getFarmerName(),
                r.getFarmerPhone(),
                r.getMaskedAadhar(),
                r.getDistrict(),
                r.getMandiName(),
                r.getCropName(),
                r.getEstimatedYieldQuintals() != null ? r.getEstimatedYieldQuintals().toString() : "",
                r.getNetWeightQuintals() != null ? r.getNetWeightQuintals().toString() : "",
                r.getMoisturePercentage() != null ? r.getMoisturePercentage().toString() : "",
                r.getForeignMatterPercentage() != null ? r.getForeignMatterPercentage().toString() : "",
                r.getTotalPayoutRs() != null ? r.getTotalPayoutRs().toString() : "",
                r.getStatus(),
                r.getPaymentStatus(),
                r.getDbtReferenceNo() != null ? r.getDbtReferenceNo() : "N/A",
                r.getBankAccountLast4(),
                r.getIfscCode(),
                r.getCompletedAt() != null ? r.getCompletedAt() : "N/A"
        );
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void flush() throws IOException {
        writer.flush();
    }
}

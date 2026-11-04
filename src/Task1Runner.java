
package task1;

import java.util.Base64;

interface ReportFormatter {
    String format(String title, String[] headers, String[][] rows);
}

class JsonFormatter implements ReportFormatter {
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder s = new StringBuilder("{\n \"report\": \"" + title + "\",\n \"records\": [\n");
        for (int i = 0; i < rows.length; i++) {
            s.append(" {");
            for (int j = 0; j < headers.length; j++) {
                s.append("\"").append(headers[j]).append("\": \"").append(rows[i][j]).append("\"");
                if (j < headers.length - 1) s.append(", ");
            }
            s.append("}").append(i < rows.length - 1 ? ",\n" : "\n");
        }
        return s.append(" ]\n}").toString();
    }
}

class CsvFormatter implements ReportFormatter {
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder s = new StringBuilder("# TITLE: " + title + "\n");
        s.append(String.join(",", headers)).append("\n");
        for (String[] row : rows)
            s.append(String.join(",", row)).append("\n");
        return s.toString();
    }
}

interface Exporter {
    void export(String destination, String data);
}

class BaseFileExporter implements Exporter {
    public void export(String destination, String data) {
        System.out.printf("[DISK WRITE] Writing %d bytes to file://%s%n", data.length(), destination);
        System.out.println("----- FILE PAYLOAD BEGIN -----");
        System.out.println(data);
        System.out.println("----- FILE PAYLOAD END -----");
    }
}

abstract class ExporterDecorator implements Exporter {
    protected final Exporter wrappedExporter;

    ExporterDecorator(Exporter e) {
        if (e == null) throw new IllegalArgumentException();
        wrappedExporter = e;
    }

    public void export(String destination, String data) {
        wrappedExporter.export(destination, data);
    }
}

class HeaderFooterDecorator extends ExporterDecorator {
    private final String timestamp;

    HeaderFooterDecorator(Exporter e, String timestamp) {
        super(e);
        this.timestamp = timestamp;
    }

    public void export(String destination, String data) {
        super.export(destination, "=== METADATA HEADER: " + timestamp +
                " ===\n" + data +
                "\n=== INTEGRITY CHECK: SHA-256 OK | END RECORD ===");
    }
}

class Base64EncryptDecorator extends ExporterDecorator {
    Base64EncryptDecorator(Exporter e) {
        super(e);
    }

    public void export(String destination, String data) {
        String encoded = Base64.getEncoder().encodeToString(
                data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        super.export(destination, "[BASE64_CIPHERTEXT:" + encoded + "]");
    }
}

public class Task1Runner {
    public static void main(String[] args) {
        String[] headers = {"ID", "User", "Role"};
        String[][] rows = {
                {"101", "sufyan", "Admin"},
                {"102", "alex", "Dev"}
        };

        ReportFormatter formatter = new JsonFormatter();
        String json = formatter.format("UserAuditReport", headers, rows);

        System.out.println(">>> TESTING DECORATOR STACK: Base64(HeaderFooter(BaseFileExporter)) <<<");

        Exporter exporter = new Base64EncryptDecorator(
                new HeaderFooterDecorator(
                        new BaseFileExporter(), "2026-11-02 10:00:00 UTC"));

        exporter.export("/var/logs/audit.sec", json);
    }
}

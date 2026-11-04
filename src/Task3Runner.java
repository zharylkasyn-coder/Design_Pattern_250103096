
package task3;

interface DispatchStrategy {
    boolean shouldDispatch(int severity, int hour);
    String getStrategyName();
}

class ImmediateStrategy implements DispatchStrategy {
    public boolean shouldDispatch(int severity, int hour) {
        return true;
    }

    public String getStrategyName() {
        return "IMMEDIATE_DISPATCH";
    }
}

class BusinessHoursStrategy implements DispatchStrategy {
    public boolean shouldDispatch(int severity, int hour) {
        return severity >= 5 || (hour >= 9 && hour <= 17);
    }

    public String getStrategyName() {
        return "BUSINESS_HOURS_ONLY (09:00 - 17:00)";
    }
}

interface MessageChannel {
    void transmit(String recipient, String message);
}

class EmailChannel implements MessageChannel {
    public void transmit(String recipient, String message) {
        System.out.printf("[EMAIL PROTOCOL -> %s]%n%s%n", recipient, message);
    }
}

class SmsChannel implements MessageChannel {
    public void transmit(String recipient, String message) {
        System.out.printf("[SMS PROTOCOL -> %s] %s%n",
                recipient, message.replace("\n", " -- "));
    }
}

abstract class Alert {
    protected final MessageChannel channel;
    protected final DispatchStrategy strategy;

    Alert(MessageChannel channel, DispatchStrategy strategy) {
        this.channel = channel;
        this.strategy = strategy;
    }

    public void processAlert(String recipient, String content, int hour) {
        int severity = getSeverityLevel();

        if (strategy.shouldDispatch(severity, hour))
            channel.transmit(recipient, formatAlertBody(content));
        else
            System.out.printf(
                    "[DISPATCH SUPPRESSED] Alert blocked by policy '%s' at hour %02d:00 (Severity: %d)%n",
                    strategy.getStrategyName(), hour, severity);
    }

    public abstract int getSeverityLevel();
    public abstract String formatAlertBody(String raw);
}

class CriticalSystemAlert extends Alert {
    CriticalSystemAlert(MessageChannel c, DispatchStrategy s) {
        super(c, s);
    }

    public int getSeverityLevel() {
        return 5;
    }

    public String formatAlertBody(String raw) {
        return "!!! CRITICAL PRODUCTION ALERT !!!\n" +
                "Incident Report: " + raw + "\n" +
                "ACTION REQUIRED: SRE on-call immediate intervention.";
    }
}

class DailySummaryAlert extends Alert {
    DailySummaryAlert(MessageChannel c, DispatchStrategy s) {
        super(c, s);
    }

    public int getSeverityLevel() {
        return 1;
    }

    public String formatAlertBody(String raw) {
        return "Daily Digest:\n" + raw;
    }
}

public class Task3Runner {
    public static void main(String[] args) {
        MessageChannel email = new EmailChannel();
        MessageChannel sms = new SmsChannel();
        DispatchStrategy business = new BusinessHoursStrategy();

        System.out.println(">>> 1. Critical Alert via SMS (Night time: 02:00, Emergency Bypass) <<<");
        Alert emergency = new CriticalSystemAlert(sms, business);
        emergency.processAlert("+1555019283", "Database primary node kernel panic", 2);

        System.out.println("\n>>> 2. Daily Summary via Email at Night (Hour: 23:00 - Should Suppress) <<<");
        Alert night = new DailySummaryAlert(email, business);
        night.processAlert("dev-team@company.internal", "99.98% uptime achieved today.", 23);

        System.out.println("\n>>> 3. Daily Summary via Email during Workday (Hour: 11:00 - Should Deliver) <<<");
        Alert day = new DailySummaryAlert(email, business);
        day.processAlert("dev-team@company.internal", "99.98% uptime achieved today.", 11);
    }
}

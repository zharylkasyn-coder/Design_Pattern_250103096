
package task4;

class LegacyForeignOrder {
    private final String id;
    private final double amount;

    LegacyForeignOrder(String id, double amount) {
        this.id = id;
        this.amount = amount;
    }

    String getOrderId() { return id; }
    double getGbpAmount() { return amount; }
}

class LegacyCurrencyConverter {
    double convert(double amount, String from, String to) {
        if (from.equalsIgnoreCase("GBP") && to.equalsIgnoreCase("USD"))
            return amount * 1.25;
        return amount;
    }
}

interface PricingService {
    double calculateTotal();
    String getReceiptBreakdown();
}

class CurrencyOrderAdapter implements PricingService {
    private final LegacyForeignOrder order;
    private final LegacyCurrencyConverter converter;
    private final String currency;

    CurrencyOrderAdapter(LegacyForeignOrder order,
                         LegacyCurrencyConverter converter, String currency) {
        this.order = order;
        this.converter = converter;
        this.currency = currency;
    }

    public double calculateTotal() {
        return converter.convert(order.getGbpAmount(), "GBP", currency);
    }

    public String getReceiptBreakdown() {
        return String.format(
                "Order [%s] Base Item: $%.2f %s (Converted from %.2f GBP)",
                order.getOrderId(), calculateTotal(), currency, order.getGbpAmount());
    }
}

abstract class PricingDecorator implements PricingService {
    protected final PricingService wrappedService;

    PricingDecorator(PricingService service) {
        wrappedService = service;
    }

    public double calculateTotal() {
        return wrappedService.calculateTotal();
    }

    public String getReceiptBreakdown() {
        return wrappedService.getReceiptBreakdown();
    }
}

class GiftWrapDecorator extends PricingDecorator {
    GiftWrapDecorator(PricingService s) { super(s); }

    public double calculateTotal() {
        return super.calculateTotal() + 5;
    }

    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Gift Wrap: $5.00]";
    }
}

class ExpressCourierDecorator extends PricingDecorator {
    ExpressCourierDecorator(PricingService s) { super(s); }

    public double calculateTotal() {
        return super.calculateTotal() + 15;
    }

    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Express Courier: $15.00]";
    }
}

class CustomsDutyDecorator extends PricingDecorator {
    CustomsDutyDecorator(PricingService s) { super(s); }

    public double calculateTotal() {
        return Math.round(super.calculateTotal() * 1.085 * 100) / 100.0;
    }

    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Customs Duty: 8.5%]";
    }
}

public class Task4Runner {
    public static void main(String[] args) {
        LegacyForeignOrder order = new LegacyForeignOrder("ORD-9821", 100);
        LegacyCurrencyConverter converter = new LegacyCurrencyConverter();

        PricingService base = new CurrencyOrderAdapter(order, converter, "USD");
        System.out.printf("Base Converted: $%.2f%n", base.calculateTotal());

        PricingService checkout = new CustomsDutyDecorator(
                new ExpressCourierDecorator(
                        new GiftWrapDecorator(base)));

        System.out.println("\n>>> INVOICE BREAKDOWN <<<");
        System.out.println(checkout.getReceiptBreakdown());
        System.out.printf("FINAL CHARGE TOTAL: $%.2f%n", checkout.calculateTotal());
    }
}

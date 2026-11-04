
package task2;

import java.util.*;

class LegacyStockFeed {
    String fetchRawTickerPacket(String ticker) {
        try { Thread.sleep(250); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        if (ticker.equalsIgnoreCase("AAPL"))
            return "TICKER=AAPL;BID=182.50;ASK=183.50;STATUS=OPEN";
        if (ticker.equalsIgnoreCase("GOOGL"))
            return "TICKER=GOOGL;BID=164.20;ASK=164.80;STATUS=OPEN";
        return "TICKER=" + ticker + ";BID=0;ASK=0;STATUS=UNKNOWN";
    }
}

interface StockProvider {
    double getMarketPrice(String ticker);
    String getStatus(String ticker);
}

class StockFeedAdapter implements StockProvider {
    private final LegacyStockFeed feed;

    StockFeedAdapter(LegacyStockFeed feed) {
        this.feed = feed;
    }

    private Map<String, String> parse(String ticker) {
        Map<String, String> map = new HashMap<>();
        for (String part : feed.fetchRawTickerPacket(ticker).split(";")) {
            String[] p = part.split("=", 2);
            if (p.length == 2) map.put(p[0], p[1]);
        }
        return map;
    }

    public double getMarketPrice(String ticker) {
        Map<String, String> m = parse(ticker);
        return (Double.parseDouble(m.get("BID")) +
                Double.parseDouble(m.get("ASK"))) / 2;
    }

    public String getStatus(String ticker) {
        return parse(ticker).get("STATUS");
    }
}

class CachedStockProxy implements StockProvider {
    private final StockProvider provider;
    private final long ttl;
    private final Map<String, Double> prices = new HashMap<>();
    private final Map<String, Long> times = new HashMap<>();

    CachedStockProxy(StockProvider provider, long ttl) {
        this.provider = provider;
        this.ttl = ttl;
    }

    public double getMarketPrice(String ticker) {
        long now = System.currentTimeMillis();

        if (prices.containsKey(ticker) && now - times.get(ticker) < ttl) {
            System.out.println("[CACHE HIT] " + ticker);
            return prices.get(ticker);
        }

        System.out.println("[NETWORK FETCH] " + ticker);
        double price = provider.getMarketPrice(ticker);
        prices.put(ticker, price);
        times.put(ticker, System.currentTimeMillis());
        return price;
    }

    public String getStatus(String ticker) {
        return provider.getStatus(ticker);
    }
}

public class Task2Runner {
    public static void main(String[] args) throws InterruptedException {
        StockProvider proxy = new CachedStockProxy(
                new StockFeedAdapter(new LegacyStockFeed()), 1500);

        System.out.println(">>> 1. Initial Request (Cold Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));

        System.out.println("\n>>> 2. Immediate Repeat Request (Hot Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));

        System.out.println("\n>>> 3. Sleeping 1600ms to invalidate cache... <<<");
        Thread.sleep(1600);

        System.out.println(">>> 4. Subsequent Request (Expired Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));
    }
}

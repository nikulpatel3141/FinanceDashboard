package org.dashutils;

import java.util.*;

public class MockDataProvider implements DataRequester {
    private static final String[] CRYPTO_COINS = {"BTC", "ETH", "SOL", "ADA", "DOT"};
    private static final double[] SPOT_PRICES = {50000.0, 3500.0, 100.0, 0.5, 8.0};
    
    @Override
    public HashMap<String, Double> getBorrowRates() {
        var rates = new HashMap<String, Double>();
        rates.put("BTC", 0.05);
        rates.put("ETH", 0.04);
        rates.put("SOL", 0.06);
        rates.put("ADA", 0.07);
        rates.put("DOT", 0.05);
        return rates;
    }

    @Override
    public HashMap<String, OptionChain> getOptionChain() {
        var chains = new HashMap<String, OptionChain>();
        
        for (String coin : CRYPTO_COINS) {
            var options = new ArrayList<Option>();
            double spotPrice = getSpotPrice(coin);
            
            // Create options with different strikes and expirations
            double[] strikeMultipliers = {0.8, 0.9, 1.0, 1.1, 1.2};
            long[] expiryDays = {7, 30, 90}; // 1 week, 1 month, 3 months
            
            for (double mult : strikeMultipliers) {
                for (long days : expiryDays) {
                    double strike = spotPrice * mult;
                    Date expiry = new Date(System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L);
                    
                    options.add(new Option(coin + "-C-" + (int)strike, coin, strike, expiry, CallPut.CALL));
                    options.add(new Option(coin + "-P-" + (int)strike, coin, strike, expiry, CallPut.PUT));
                }
            }
            
            chains.put(coin, new OptionChain(coin, options));
        }
        
        return chains;
    }

    @Override
    public HashMap<String, OptionMarketData> getOptionMarketData() {
        var marketData = new HashMap<String, OptionMarketData>();
        var chains = getOptionChain();
        
        chains.forEach((underlying, chain) -> {
            double spotPrice = getSpotPrice(underlying);
            
            chain.optionSeries().forEach(option -> {
                // Create mock volatility smile (higher vol for OTM options)
                double moneyness = option.strike() / spotPrice;
                double baseVol = 0.15 + Math.abs(moneyness - 1.0) * 0.5; // V-shape smile
                double vol = baseVol + (Math.random() - 0.5) * 0.05;
                
                // Mock price and delta
                double price = spotPrice * vol * 0.1; // Simple mock price
                double delta = option.callPut() == CallPut.CALL ? 0.5 : -0.5;
                
                marketData.put(option.symbol(), new OptionMarketData(price, delta, vol));
            });
        });
        
        return marketData;
    }

    @Override
    public Double getSpotMarketPrice(String ticker) {
        return getSpotPrice(ticker);
    }
    
    private double getSpotPrice(String coin) {
        for (int i = 0; i < CRYPTO_COINS.length; i++) {
            if (CRYPTO_COINS[i].equals(coin)) {
                return SPOT_PRICES[i];
            }
        }
        return 100.0;
    }
}
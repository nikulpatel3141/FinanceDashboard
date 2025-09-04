package org.dashutils;

import java.util.HashMap;

public class CachedDataRequester implements DataRequester {
    private final DataRequester delegate;
    private HashMap<String, Double> borrowRatesCache;
    private HashMap<String, OptionChain> optionChainCache;
    private HashMap<String, OptionMarketData> marketDataCache;
    final private HashMap<String, Double> spotPriceCache;
    
    public CachedDataRequester(DataRequester delegate) {
        this.delegate = delegate;
        this.spotPriceCache = new HashMap<>();
    }
    
    @Override
    public HashMap<String, Double> getBorrowRates() {
        if (borrowRatesCache == null) {
            borrowRatesCache = delegate.getBorrowRates();
        }
        return borrowRatesCache;
    }
    
    @Override
    public HashMap<String, OptionChain> getOptionChain() {
        if (optionChainCache == null) {
            optionChainCache = delegate.getOptionChain();
        }
        return optionChainCache;
    }
    
    @Override
    public HashMap<String, OptionMarketData> getOptionMarketData() {
        if (marketDataCache == null) {
            marketDataCache = delegate.getOptionMarketData();
        }
        return marketDataCache;
    }
    
    @Override
    public Double getSpotMarketPrice(String ticker) {
        if (!spotPriceCache.containsKey(ticker)) {
            spotPriceCache.put(ticker, delegate.getSpotMarketPrice(ticker));
        }
        return spotPriceCache.get(ticker);
    }
    
    public void clearCaches() {
        borrowRatesCache = null;
        optionChainCache = null;
        marketDataCache = null;
        spotPriceCache.clear();
    }
}
package org.dashutils;

import java.util.HashMap;

public interface DataRequester {
    public HashMap<String, Double> getBorrowRates();

    public HashMap<String, OptionChain> getOptionChain();

    public HashMap<String, OptionMarketData> getOptionMarketData();

    public Double getSpotMarketPrice(String ticker);
}
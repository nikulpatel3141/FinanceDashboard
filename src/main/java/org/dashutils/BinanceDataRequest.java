package org.dashutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


class BinanceApiQuery {
    enum Endpoints {
        BorrowRates("https://www.binance.com/bapi/margin/v1/friendly/margin/vip/spec/list-all"),
        CoinInfo("https://eapi.binance.com/eapi/v1/exchangeInfo"),
        MarketPrices("https://eapi.binance.com/eapi/v1/mark");

        final String url;

        Endpoints(String url) {
            this.url = url;
        }
    }

    static JsonNode queryEndpoint(Endpoints endpoint) throws IOException {
        var rawJSONResponse = Request.get(endpoint.url).execute().returnContent().asString();
        var objectMapper = new ObjectMapper();
        return objectMapper.readTree(rawJSONResponse);
    }
}

public class BinanceDataRequest implements DataRequester {
    private JsonNode makeRequest(BinanceApiQuery.Endpoints endpoint){
        JsonNode response;
        try {
            response = BinanceApiQuery.queryEndpoint(endpoint);
        } catch (Exception e) {
            return null;
        }
        return response;
    }

    public HashMap<String, Double> getBorrowRates() {
        JsonNode ratesResponse = makeRequest(BinanceApiQuery.Endpoints.BorrowRates);
        if (ratesResponse == null) return new HashMap<>();

        var parsedRates = new HashMap<String, Double>();
        var data = ratesResponse.get("data").elements();
        data.forEachRemaining((node) -> {
            var asset = node.get("assetName").asText();
            var dailyBorrowRate = node.get("specs").elements().next().get("dailyInterestRate").asDouble();
            parsedRates.put(asset, dailyBorrowRate * 365);
        });
        return parsedRates;
    }

    public HashMap<String, OptionChain> getOptionChain(){
        JsonNode chainResponse = makeRequest(BinanceApiQuery.Endpoints.CoinInfo);
        if (chainResponse == null) return new HashMap<>();

        var parsedChains = new HashMap<String, OptionChain>();
        chainResponse.get("optionSymbols").forEach((node) -> {
            String underl = node.get("underlying").asText();
            if (!parsedChains.containsKey(underl)){
                parsedChains.put(underl, new OptionChain(underl, new ArrayList<>()));
            }
            var symbol = node.get("symbol").asText();
            var strike = node.get("strikePrice").asDouble();
            var expiry = new Date(node.get("expiryDate").asLong());
            var callPut = CallPut.fromString(node.get("side").asText());
            var option = new Option(symbol, underl, strike, expiry, callPut);

            var chain = parsedChains.get(underl);
            chain.optionSeries().add(option);
        });

        return parsedChains;
    }

    public HashMap<String, MarketData> getMarketData(){
        JsonNode pricesResponse = makeRequest(BinanceApiQuery.Endpoints.MarketPrices);
        if (pricesResponse == null) return new HashMap<>();

        var parsedPrices = new HashMap<String, MarketData>();
        pricesResponse.forEach((node) -> {
            var price = node.get("markPrice").asDouble();
            var delta = node.get("delta").asDouble();
            var impliedVol = node.get("markIV").asDouble();
            var symbol = node.get("symbol").asText();

            parsedPrices.put(symbol, new MarketData(price, delta, impliedVol));
        });

        return parsedPrices;
    }

    public static void main(String[] args){
        var requester = new BinanceDataRequest();
//        var borrowRates = requester.getBorrowRates();
//        var optionChain = requester.getOptionChain();
        var marketPrices = requester.getMarketData();
    }
}

interface DataRequester {
    public HashMap<String, Double> getBorrowRates();

    public HashMap<String, OptionChain> getOptionChain();

    public HashMap<String, MarketData> getMarketData();
}


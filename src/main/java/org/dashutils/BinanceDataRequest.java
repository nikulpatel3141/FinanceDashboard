package org.dashutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


class BinanceApiQuery {
    enum Endpoints {
        BorrowRates("https://www.binance.com/bapi/margin/v1/friendly/margin/vip/spec/list-all"),
        CoinInfo("https://eapi.binance.com/eapi/v1/exchangeInfo"),
        OptionMarketPrices("https://eapi.binance.com/eapi/v1/mark"),
        SpotMarketPrice("https://data-api.binance.vision/api/v3/avgPrice");

        final String url;

        Endpoints(String url) {
            this.url = url;
        }
    }

    static JsonNode queryEndpoint(Endpoints endpoint, Map<String, String> queryParams) throws IOException {
        URI uri;
        try {
            final URIBuilder[] uriBuilder = {new URIBuilder(endpoint.url)};
            queryParams.forEach((k, v) -> {
                uriBuilder[0] = uriBuilder[0].addParameter(k, v);
            } );
            uri = uriBuilder[0].build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var rawJSONResponse = Request.get(uri).execute().returnContent().asString();
        var objectMapper = new ObjectMapper();
        return objectMapper.readTree(rawJSONResponse);
    }
}

public class BinanceDataRequest implements DataRequester {
    private JsonNode makeRequest(BinanceApiQuery.Endpoints endpoint){
        return makeRequest(endpoint, null);
    }

    private JsonNode makeRequest(BinanceApiQuery.Endpoints endpoint, Map<String, String> queryParams){
        JsonNode response;
        try {
            response = BinanceApiQuery.queryEndpoint(endpoint, queryParams);
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

    public HashMap<String, OptionMarketData> getOptionMarketData(){
        JsonNode pricesResponse = makeRequest(BinanceApiQuery.Endpoints.OptionMarketPrices);
        if (pricesResponse == null) return new HashMap<>();

        var parsedPrices = new HashMap<String, OptionMarketData>();
        pricesResponse.forEach((node) -> {
            var price = node.get("markPrice").asDouble();
            var delta = node.get("delta").asDouble();
            var impliedVol = node.get("markIV").asDouble();
            var symbol = node.get("symbol").asText();

            parsedPrices.put(symbol, new OptionMarketData(price, delta, impliedVol));
        });

        return parsedPrices;
    }

    public Double getSpotMarketPrice(String symbol){
        var queryParams = new HashMap<String, String>();
        queryParams.put("symbol", symbol);

        JsonNode priceResponse = makeRequest(BinanceApiQuery.Endpoints.SpotMarketPrice, queryParams);
        if (priceResponse == null) return null;

        return priceResponse.get("price").asDouble();
    }

    public static void main(String[] args){
        var requester = new BinanceDataRequest();
//        var borrowRates = requester.getBorrowRates();
//        var optionChain = requester.getOptionChain();
//        var marketPrices = requester.getOptionMarketData();
//        var spotPrice = requester.getSpotMarketPrice("SOLUSDT");
    }
}


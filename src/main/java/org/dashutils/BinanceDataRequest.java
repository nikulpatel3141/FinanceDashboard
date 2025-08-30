package org.dashutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;
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
    public HashMap<String, Double> getBorrowRates() {
        JsonNode ratesResponse;
        try {
            ratesResponse = BinanceApiQuery.queryEndpoint(BinanceApiQuery.Endpoints.BorrowRates);
        } catch (Exception e) {
            return new HashMap<>();
        }

        var parsedRates = new HashMap<String, Double>();
        var data = ratesResponse.get("data").elements();
        data.forEachRemaining((node) -> {
            var asset = node.get("assetName").asText();
            var dailyBorrowRate = node.get("specs").elements().next().get("dailyInterestRate").asDouble();
            parsedRates.put(asset, dailyBorrowRate * 365);
        });
        return parsedRates;
    }

    public OptionChain getOptionChain(String underlying){
        JsonNode chainResponse;
        try {
            chainResponse = BinanceApiQuery.queryEndpoint(BinanceApiQuery.Endpoints.CoinInfo);
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public HashMap<String, Double> getMarketPrices(String underlying){
        return null;
    }

    public static void main(String[] args){
        var requester = new BinanceDataRequest();
        var borrowRates = requester.getBorrowRates();

    }
}

interface DataRequester {
    public HashMap<String, Double> getBorrowRates();

    public OptionChain getOptionChain(String underlying);

    public HashMap<String, Double> getMarketPrices(String underlying);
}

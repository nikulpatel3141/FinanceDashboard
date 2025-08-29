package org.dashutils;


import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;


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

    static String queryEndpoint(Endpoints endpoint) throws IOException {
        return Request.get(endpoint.url).execute().returnContent().asString();
    }
}


public class BinanceDataRequest implements DataRequester {
    public HashMap<String, Double> getBorrowRates() {
        String ratesResponse;
        try {
            ratesResponse = BinanceApiQuery.queryEndpoint(BinanceApiQuery.Endpoints.BorrowRates);
        } catch (Exception e) {
            return new HashMap<>();
        }

        System.out.println(ratesResponse);


        return new HashMap<>();
    }

    public OptionChain getOptionChain(String underlying){
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

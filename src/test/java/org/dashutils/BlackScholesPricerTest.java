package org.dashutils;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class BlackScholesPricerTest {
    private final BlackScholesPricer pricer = new BlackScholesPricer();

    @Test
    public void testImpliedVolCall() {
        var option = new Option("TEST", "BTC", 50000.0, new Date(1735689600000L), CallPut.CALL); // 2025-01-01
        double optionPrice = 2000.0;
        double spotPrice = 50000.0;
        double riskFreeRate = 0.05;
        double dividendRate = 0.0;
        Date currentDate = new Date(1704067200000L); // 2024-01-01
        
        double impliedVol = pricer.impliedVol(option, optionPrice, spotPrice, riskFreeRate, dividendRate, currentDate);

        assertEquals(0.1855, impliedVol, 0.01, "Implied volatility should match expected value");
    }

    @Test
    public void testImpliedVolPut() {
        var option = new Option("TEST", "BTC", 45000.0, new Date(1735689600000L), CallPut.PUT); // 2025-01-01
        double optionPrice = 1000.0;
        double spotPrice = 50000.0;
        double riskFreeRate = 0.05;
        double dividendRate = 0.0;
        Date currentDate = new Date(1704067200000L); // 2024-01-01
        
        double impliedVol = pricer.impliedVol(option, optionPrice, spotPrice, riskFreeRate, dividendRate, currentDate);
        
        // TODO: Replace with expected value after running test
        assertEquals(0.1368, impliedVol, 0.01, "Implied volatility should match expected value");
    }

    @Test
    public void testHigherPriceHigherVol() {
        var option = new Option("TEST", "BTC", 50000.0, new Date(1735689600000L), CallPut.CALL); // 2025-01-01
        double spotPrice = 50000.0;
        double riskFreeRate = 0.05;
        double dividendRate = 0.0;
        Date currentDate = new Date(1704067200000L); // 2024-01-01
        
        double lowPrice = 1000.0;
        double highPrice = 3000.0;
        
        double lowVol = pricer.impliedVol(option, lowPrice, spotPrice, riskFreeRate, dividendRate, currentDate);
        double highVol = pricer.impliedVol(option, highPrice, spotPrice, riskFreeRate, dividendRate, currentDate);
        
        assertTrue(highVol > lowVol, "Higher option price should result in higher implied volatility");
    }
}
package org.dashutils;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BlackScholesPricer implements OptionPricer {
    final static NormalDistribution normalDist = new NormalDistribution(0, 1);

    static double blackScholesPrice(Option option, Double spotPrice, Double riskFreeRate, Double dividendRate, Double volatility, Date currentDate){
        double daysToExpiry = TimeUnit.DAYS.convert(option.expiry().getTime() - currentDate.getTime(), TimeUnit.MILLISECONDS);

        var d1 = (
            Math.log(spotPrice / option.strike()) +
            daysToExpiry * (riskFreeRate - dividendRate + 0.5 * Math.pow(volatility, 2.0))
        ) / (volatility * Math.pow(daysToExpiry, 0.5));
        var d2 = d1 - volatility * Math.pow(daysToExpiry, 0.5);

        var riskFreeDiscount = Math.exp(-riskFreeRate * daysToExpiry);
        var dividendDiscount = Math.exp(-dividendRate * daysToExpiry);

        if (option.callPut() == CallPut.CALL){
            return (
                spotPrice * riskFreeDiscount * normalDist.inverseCumulativeProbability(d1) -
                option.strike() * dividendDiscount * normalDist.inverseCumulativeProbability(d2)
            );
        }
        return (
                option.strike() * dividendDiscount * normalDist.inverseCumulativeProbability(-d2) -
                spotPrice * riskFreeDiscount * normalDist.inverseCumulativeProbability(-d1)
        );

    }

    @Override
    public Double impliedVol(Option option, Double optionPrice, Double spotPrice, Double riskFreeRate, Double dividendRate, Date currentDate){
        var lowerVolBound = 1e-5;
        var upperVolBound = 1;

        var calcPrice = (Double volGuess) -> {
            return blackScholesPrice(option, spotPrice, riskFreeRate, dividendRate, volGuess, currentDate);
        };

        while (calcPrice(upperVolBound) < optionPrice){
            lowerVolBound = upperVolBound;
            upperVolBound *= 2;
        }

        return 0.0;
    }
}

interface OptionPricer {
    public Double impliedVol(Option option, Double optionPrice, Double spotPrice, Double riskFreeRate, Double dividendRate, Date currentDate);
}

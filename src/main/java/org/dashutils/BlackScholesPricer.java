package org.dashutils;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BlackScholesPricer implements OptionPricer {
    final static NormalDistribution normalDist = new NormalDistribution(0, 1);

    static double blackScholesPrice(Option option, Double spotPrice, Double riskFreeRate, Double dividendRate, Double volatility, Date currentDate){
        double yearsToExpiry = TimeUnit.DAYS.convert(option.expiry().getTime() - currentDate.getTime(), TimeUnit.MILLISECONDS) / 365.0;

        var d1 = (
            Math.log(spotPrice / option.strike()) +
            yearsToExpiry * (riskFreeRate - dividendRate + 0.5 * Math.pow(volatility, 2.0))
        ) / (volatility * Math.pow(yearsToExpiry, 0.5));
        var d2 = d1 - volatility * Math.pow(yearsToExpiry, 0.5);

        var riskFreeDiscount = Math.exp(-riskFreeRate * yearsToExpiry);
        var dividendDiscount = Math.exp(-dividendRate * yearsToExpiry);

        if (option.callPut() == CallPut.CALL){
            return (
                spotPrice * riskFreeDiscount * normalDist.cumulativeProbability(d1) -
                option.strike() * dividendDiscount * normalDist.cumulativeProbability(d2)
            );
        }
        return (
                option.strike() * dividendDiscount * normalDist.cumulativeProbability(-d2) -
                spotPrice * riskFreeDiscount * normalDist.cumulativeProbability(-d1)
        );

    }

    @Override
    public Double impliedVol(Option option, Double optionPrice, Double spotPrice, Double riskFreeRate, Double dividendRate, Date currentDate){
        var lowerVolBound = 1e-5;
        var upperVolBound = 1.0;

        Function<Double, Double> calcPrice = (Double volGuess) ->
                blackScholesPrice(option, spotPrice, riskFreeRate, dividendRate, volGuess, currentDate);

        while (calcPrice.apply(upperVolBound) < optionPrice){
            lowerVolBound = upperVolBound;
            upperVolBound *= 2;
        }

        var tolerance = 1e-5;
        while (upperVolBound - lowerVolBound > tolerance){
            var mid = lowerVolBound + (upperVolBound - lowerVolBound) / 2;
            var bsPrice = calcPrice.apply(mid);
            if (bsPrice > optionPrice){
                upperVolBound = mid;
            } else {
                lowerVolBound = mid;
            }
        }

        return (upperVolBound + lowerVolBound) / 2;
    }
}

interface OptionPricer {
    public Double impliedVol(Option option, Double optionPrice, Double spotPrice, Double riskFreeRate, Double dividendRate, Date currentDate);
}

package org.dash;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.measure.fxopt.FxVanillaOptionTradeCalculations;
import com.opengamma.strata.pricer.fxopt.BlackFxVanillaOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.VannaVolgaFxVanillaOptionTradePricer;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import org.dashutils.BinanceDataRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.product.common.LongShort.SHORT;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        ReferenceData refData = ReferenceData.standard();
        System.out.println(refData);
        FxSingle fx = FxSingle.of(CurrencyAmount.of(USD, 1000),
                FxRate.of(EUR, USD, 1.115),
                LocalDate.of(2016, 10, 5));

        FxVanillaOption fxOpt = FxVanillaOption.builder()
                .longShort(SHORT)
                .expiryDate(LocalDate.of(2016, 10, 5))
                .expiryTime(LocalTime.of(13, 00))
                .expiryZone(ZoneId.of("Europe/Paris"))
                .underlying(fx)
                .build();

        var calcs = new FxVanillaOptionTradeCalculations(BlackFxVanillaOptionTradePricer.DEFAULT, VannaVolgaFxVanillaOptionTradePricer.DEFAULT);
//        calcs.presentValue();

        var expiry = fxOpt.getExpiry();
        System.out.println(expiry);

//        var x = BinanceDataRequest.x;
//        System.out.println(x);

    }
}
package org.dashutils;

import java.util.Date;

public record Option (String symbol, String underlying, double strike, Date expiry, CallPut callPut){}

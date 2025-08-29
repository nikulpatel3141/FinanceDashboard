package org.dashutils;

import java.util.Date;

public record Option (String underlying, double strike, Date expiry){}

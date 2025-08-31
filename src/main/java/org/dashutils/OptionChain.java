package org.dashutils;

import java.util.List;

public record OptionChain(String underlying, List<Option> optionSeries) {
}

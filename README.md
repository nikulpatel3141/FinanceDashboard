# OptionDash

[![Code Linting](https://github.com/nikulpatel3141/FinanceDashboard/actions/workflows/lint.yml/badge.svg)](https://github.com/nikulpatel3141/FinanceDashboard/actions/workflows/lint.yml)

A Java application for visualizing cryptocurrency options volatility smiles. Displays implied volatility curves for crypto options with data from Binance or mock data for testing.

![image](/img/img.png)

## Features

- Volatility smile charts for BTC, ETH, SOL, ADA, DOT options
- Filter by expiration date
- Switch between mock data and live Binance API
- Black-Scholes pricing model implementation

## Requirements

- Java 24+
- Maven

## Usage

Build and run:
```bash
mvn compile
mvn exec:java -Dexec.mainClass="org.dashui.VolatilitySmileUI"
```

## Development

The UI components were built with assistance from Claude Code for the Swing interface and async data loading. The core options pricing logic and data integration were implemented independently.

Run tests:
```bash
mvn test
```

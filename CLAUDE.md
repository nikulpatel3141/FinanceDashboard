# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

- **Build**: `mvn compile` - Compile the Java source code
- **Run**: `mvn exec:java -Dexec.mainClass="org.dash.Main"` - Execute the main application
- **Run UI**: `mvn exec:java -Dexec.mainClass="org.dashui.VolatilitySmileUI"` - Launch volatility smile UI
- **Test**: `mvn test` - Run all unit tests
- **Test Single Class**: `mvn test -Dtest=BlackScholesPricerTest` - Run specific test class
- **Clean**: `mvn clean` - Clean build artifacts
- **Package**: `mvn package` - Create JAR file

## Architecture Overview

This is an options pricing and market data application for cryptocurrency options with a modular architecture:

### Core Packages

- **org.dash**: Main application entry point and integration with OpenGamma Strata library
- **org.dashutils**: Utilities for options modeling, market data, and pricing

### Key Components

1. **Market Data Layer** (`BinanceDataRequest.java`):
   - Implements `DataRequester` interface for external API calls
   - Fetches option chains, market prices, borrow rates, and spot prices from Binance APIs
   - Uses Jackson for JSON parsing and Apache HTTP Client for requests

2. **Options Model** (`Option.java`, `OptionChain.java`, `OptionMarketData.java`):
   - Record-based data structures for options representation
   - `CallPut` enum for option types with string parsing
   - Immutable data structures using Java records

3. **Pricing Engine** (`BlackScholesPricer.java`):
   - Implements Black-Scholes pricing model for options
   - Binary search algorithm for implied volatility calculation
   - Interface-based design (`OptionPricer`) for extensibility

4. **External Dependencies**:
   - **OpenGamma Strata**: Professional derivatives library for FX options (used in Main.java)
   - **Jackson**: JSON processing for API responses
   - **Apache HTTP Client**: HTTP requests to Binance APIs
   - **Apache Commons Math**: Statistical distributions for pricing models

### Design Patterns

- Interface-based architecture (`DataRequester`, `OptionPricer`)
- Record pattern for immutable data structures
- Binary search implementation for numerical root finding
- Factory pattern in `CallPut.fromString()` method

## Java Version

The project uses Java 24 with modern language features including records and switch expressions.
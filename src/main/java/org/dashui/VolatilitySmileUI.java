package org.dashui;

import org.dashutils.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class VolatilitySmileUI extends JFrame {
    private CachedDataRequester dataProvider;
    private JComboBox<String> coinSelector;
    private JComboBox<String> dataSourceSelector;
    private ChartPanel chartPanel;
    private JButton refreshButton;
    private JLabel statusLabel;
    
    public VolatilitySmileUI() {
        this.dataProvider = new CachedDataRequester(new MockDataProvider());
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Crypto Options Volatility Smile");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());
        
        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout());
        
        // Data source selector
        topPanel.add(new JLabel("Data Source:"));
        String[] dataSources = {"Mock Data", "Binance API"};
        dataSourceSelector = new JComboBox<>(dataSources);
        dataSourceSelector.addActionListener(e -> switchDataSource());
        topPanel.add(dataSourceSelector);
        
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Coin selector
        topPanel.add(new JLabel("Select Crypto:"));
        String[] coins = {"BTC", "ETH", "SOL", "ADA", "DOT"};
        for (var i = 0; i < coins.length; ++i) coins[i] += "USDT";

        coinSelector = new JComboBox<>(coins);
        coinSelector.addActionListener(e -> updateChart());
        topPanel.add(coinSelector);
        
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Refresh button
        refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        topPanel.add(refreshButton);
        
        // Status label
        statusLabel = new JLabel("Ready");
        topPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Create initial chart
        updateChart();
        
        setLocationRelativeTo(null);
    }
    
    private void updateChart() {
        String selectedCoin = (String) coinSelector.getSelectedItem();
        if (selectedCoin == null) return;
        
        setLoadingState(true, "Loading chart data...");
        
        SwingWorker<ChartData, Void> worker = new SwingWorker<ChartData, Void>() {
            @Override
            protected ChartData doInBackground() throws Exception {
                var optionChains = dataProvider.getOptionChain();
                var marketData = dataProvider.getOptionMarketData();
                Double spotPrice = dataProvider.getSpotMarketPrice(selectedCoin);
                
                return new ChartData(optionChains, marketData, spotPrice);
            }
            
            @Override
            protected void done() {
                try {
                    ChartData data = get();
                    createChart(selectedCoin, data);
                    setLoadingState(false, "Ready");
                } catch (InterruptedException | ExecutionException e) {
                    setLoadingState(false, "Error loading data");
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void createChart(String selectedCoin, ChartData data) {
        XYSeries series = new XYSeries("Implied Volatility");
        
        if (data.optionChains.containsKey(selectedCoin)) {
            OptionChain chain = data.optionChains.get(selectedCoin);
            
            // Filter for calls only and sort by strike
            chain.optionSeries().stream()
                    .filter(option -> option.callPut() == CallPut.CALL)
                    .sorted((a, b) -> Double.compare(a.strike(), b.strike()))
                    .forEach(option -> {
                        OptionMarketData marketData = data.marketData.get(option.symbol());
                        if (marketData != null) {
                            series.add(option.strike(), marketData.impliedVol() * 100); // Convert to percentage
                        }
                    });
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                selectedCoin + " Volatility Smile (Spot: $" + String.format("%.2f", data.spotPrice) + ")",
                "Strike Price",
                "Implied Volatility (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        if (chartPanel != null) {
            remove(chartPanel);
        }
        
        chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    private static class ChartData {
        final HashMap<String, OptionChain> optionChains;
        final HashMap<String, OptionMarketData> marketData;
        final Double spotPrice;
        
        ChartData(HashMap<String, OptionChain> optionChains, 
                 HashMap<String, OptionMarketData> marketData, 
                 Double spotPrice) {
            this.optionChains = optionChains;
            this.marketData = marketData;
            this.spotPrice = spotPrice;
        }
    }
    
    private void switchDataSource() {
        String selected = (String) dataSourceSelector.getSelectedItem();
        
        setLoadingState(true, "Switching data source...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DataRequester newProvider;
                
                if ("Binance API".equals(selected)) {
                    newProvider = new BinanceDataRequest();
                } else {
                    newProvider = new MockDataProvider();
                }
                
                dataProvider = new CachedDataRequester(newProvider);
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    setLoadingState(false, "Ready");
                    updateChart();
                } catch (InterruptedException | ExecutionException e) {
                    setLoadingState(false, "Error switching data source");
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void refreshData() {
        setLoadingState(true, "Refreshing data...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dataProvider.clearCaches();
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    updateChart();
                } catch (InterruptedException | ExecutionException e) {
                    setLoadingState(false, "Error refreshing data");
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void setLoadingState(boolean loading, String message) {
        statusLabel.setText(message);
        coinSelector.setEnabled(!loading);
        dataSourceSelector.setEnabled(!loading);
        refreshButton.setEnabled(!loading);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default look and feel
            }
            
            new VolatilitySmileUI().setVisible(true);
        });
    }
}
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

public class VolatilitySmileUI extends JFrame {
    private CachedDataRequester dataProvider;
    private JComboBox<String> coinSelector;
    private JComboBox<String> dataSourceSelector;
    private ChartPanel chartPanel;
    
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
        coinSelector = new JComboBox<>(coins);
        coinSelector.addActionListener(e -> updateChart());
        topPanel.add(coinSelector);
        
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        topPanel.add(refreshButton);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Create initial chart
        updateChart();
        
        setLocationRelativeTo(null);
    }
    
    private void updateChart() {
        String selectedCoin = (String) coinSelector.getSelectedItem();
        if (selectedCoin == null) return;
        
        var optionChains = dataProvider.getOptionChain();
        var marketData = dataProvider.getOptionMarketData();
        Double spotPrice = dataProvider.getSpotMarketPrice(selectedCoin);
        
        XYSeries series = new XYSeries("Implied Volatility");
        
        if (optionChains.containsKey(selectedCoin)) {
            OptionChain chain = optionChains.get(selectedCoin);
            
            // Filter for calls only and sort by strike
            chain.optionSeries().stream()
                    .filter(option -> option.callPut() == CallPut.CALL)
                    .sorted((a, b) -> Double.compare(a.strike(), b.strike()))
                    .forEach(option -> {
                        OptionMarketData data = marketData.get(option.symbol());
                        if (data != null) {
                            series.add(option.strike(), data.impliedVol() * 100); // Convert to percentage
                        }
                    });
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                selectedCoin + " Volatility Smile (Spot: $" + String.format("%.2f", spotPrice) + ")",
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
    
    private void switchDataSource() {
        String selected = (String) dataSourceSelector.getSelectedItem();
        DataRequester newProvider;
        
        if ("Binance API".equals(selected)) {
            newProvider = new BinanceDataRequest();
        } else {
            newProvider = new MockDataProvider();
        }
        
        this.dataProvider = new CachedDataRequester(newProvider);
        updateChart();
    }
    
    private void refreshData() {
        dataProvider.clearCaches();
        updateChart();
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
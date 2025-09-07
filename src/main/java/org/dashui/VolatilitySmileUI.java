package org.dashui;

import org.dashutils.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class VolatilitySmileUI extends JFrame {
    private CachedDataRequester dataProvider;
    private JComboBox<String> coinSelector;
    private JComboBox<String> dataSourceSelector;
    private JComboBox<String> expirySelector;
    private ChartPanel chartPanel;
    private JButton refreshButton;
    private JLabel statusLabel;
    private JTable interestRatesTable;
    private JTable optionPricesTable;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
    private DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    
    public VolatilitySmileUI() {
        this.dataProvider = new CachedDataRequester(new MockDataProvider());
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Crypto Options Volatility Smile");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
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
        coinSelector.addActionListener(e -> {
            updateInterestRatesTable();
            updateExpirySelector();
            updateChart();
        });
        topPanel.add(coinSelector);
        
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Expiry selector
        topPanel.add(new JLabel("Expiry Date:"));
        expirySelector = new JComboBox<>();
        expirySelector.addActionListener(e -> updateChart());
        topPanel.add(expirySelector);
        
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Refresh button
        refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        topPanel.add(refreshButton);
        
        // Status label
        statusLabel = new JLabel("Ready");
        topPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Initialize tables
        initializeTables();
        
        // Create initial chart
        updateExpirySelector();
        updateChart();
        
        setLocationRelativeTo(null);
    }
    
    private void initializeTables() {
        // Create a panel to hold both tables on the left side
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(375, 0)); // 50% wider than 250
        
        // Interest rates table
        String[] rateColumns = {"Asset", "Lending Rate (%)"};
        DefaultTableModel rateModel = new DefaultTableModel(rateColumns, 0);
        interestRatesTable = new JTable(rateModel);
        
        // Right-align the lending rate column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        interestRatesTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        
        JScrollPane rateScrollPane = new JScrollPane(interestRatesTable);
        rateScrollPane.setPreferredSize(new Dimension(375, 100));
        rateScrollPane.setBorder(BorderFactory.createTitledBorder("Lending Rates"));
        leftPanel.add(rateScrollPane);
        
        // Option prices table
        String[] priceColumns = {"Strike", "Call Price", "Put Price", "Implied Vol (%)"};
        DefaultTableModel priceModel = new DefaultTableModel(priceColumns, 0);
        optionPricesTable = new JTable(priceModel);
        
        // Right-align numeric columns
        for (int i = 0; i < priceColumns.length; i++) {
            optionPricesTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
        
        JScrollPane priceScrollPane = new JScrollPane(optionPricesTable);
        priceScrollPane.setBorder(BorderFactory.createTitledBorder("Option Prices"));
        leftPanel.add(priceScrollPane);
        
        add(leftPanel, BorderLayout.WEST);
        
        updateInterestRatesTable();
    }
    
    private void updateInterestRatesTable() {
        SwingWorker<HashMap<String, Double>, Void> worker = new SwingWorker<HashMap<String, Double>, Void>() {
            @Override
            protected HashMap<String, Double> doInBackground() throws Exception {
                return dataProvider.getBorrowRates();
            }
            
            @Override
            protected void done() {
                try {
                    HashMap<String, Double> rates = get();
                    DefaultTableModel model = (DefaultTableModel) interestRatesTable.getModel();
                    model.setRowCount(0);
                    
                    String selectedCoin = (String) coinSelector.getSelectedItem();
                    if (selectedCoin != null) {
                        // Show rate for selected crypto
                        String baseCrypto = selectedCoin.replace("USDT", "");
                        if (rates.containsKey(baseCrypto)) {
                            double rate = rates.get(baseCrypto) * 100; // Convert to percentage
                            model.addRow(new Object[]{baseCrypto, percentFormat.format(rate)});
                        }
                        
                        // Always show USDT rate
                        if (rates.containsKey("USDT")) {
                            double usdtRate = rates.get("USDT") * 100;
                            model.addRow(new Object[]{"USDT", percentFormat.format(usdtRate)});
                        } else {
                            // If USDT rate not available, show 0% as placeholder
                            model.addRow(new Object[]{"USDT", percentFormat.format(0.0)});
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    private void updateExpirySelector() {
        String selectedCoin = (String) coinSelector.getSelectedItem();
        if (selectedCoin == null) return;
        
        SwingWorker<Set<String>, Void> worker = new SwingWorker<Set<String>, Void>() {
            @Override
            protected Set<String> doInBackground() throws Exception {
                var optionChains = dataProvider.getOptionChain();
                Set<String> expiries = new HashSet<>();
                
                if (optionChains.containsKey(selectedCoin)) {
                    OptionChain chain = optionChains.get(selectedCoin);
                    
                    // Group options by expiry date and count them
                    Map<Date, Long> expiryCount = chain.optionSeries().stream()
                            .filter(option -> option.callPut() == CallPut.CALL)
                            .collect(Collectors.groupingBy(Option::expiry, Collectors.counting()));
                    
                    // Only include expiries with 3+ options
                    expiries = expiryCount.entrySet().stream()
                            .filter(entry -> entry.getValue() >= 3)
                            .map(entry -> dateFormat.format(entry.getKey()))
                            .collect(Collectors.toSet());
                }
                
                return expiries;
            }
            
            @Override
            protected void done() {
                try {
                    Set<String> expiries = get();
                    expirySelector.removeAllItems();
                    
                    List<String> sortedExpiries = expiries.stream()
                            .sorted()
                            .collect(Collectors.toList());
                    
                    for (String expiry : sortedExpiries) {
                        expirySelector.addItem(expiry);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void updateChart() {
        String selectedCoin = (String) coinSelector.getSelectedItem();
        String selectedExpiry = (String) expirySelector.getSelectedItem();
        if (selectedCoin == null || selectedExpiry == null) return;
        
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
                    createChart(selectedCoin, selectedExpiry, data);
                    updateOptionPricesTable(selectedCoin, selectedExpiry, data);
                    setLoadingState(false, "Ready");
                } catch (InterruptedException | ExecutionException e) {
                    setLoadingState(false, "Error loading data");
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void createChart(String selectedCoin, String selectedExpiry, ChartData data) {
        XYSeries series = new XYSeries("Implied Volatility");
        
        if (data.optionChains.containsKey(selectedCoin)) {
            OptionChain chain = data.optionChains.get(selectedCoin);
            
            // Parse the selected expiry date
            Date targetExpiry = null;
            try {
                targetExpiry = dateFormat.parse(selectedExpiry);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            
            final Date finalTargetExpiry = targetExpiry;
            
            // Filter for calls only, matching expiry, and sort by strike
            chain.optionSeries().stream()
                    .filter(option -> option.callPut() == CallPut.CALL)
                    .filter(option -> dateFormat.format(option.expiry()).equals(selectedExpiry))
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
                selectedCoin + " Volatility Smile - " + selectedExpiry + " (Spot: $" + String.format("%.2f", data.spotPrice) + ")",
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
    
    private void updateOptionPricesTable(String selectedCoin, String selectedExpiry, ChartData data) {
        DefaultTableModel model = (DefaultTableModel) optionPricesTable.getModel();
        model.setRowCount(0);
        
        if (!data.optionChains.containsKey(selectedCoin)) return;
        
        OptionChain chain = data.optionChains.get(selectedCoin);
        
        // Parse the selected expiry date
        Date targetExpiry = null;
        try {
            targetExpiry = dateFormat.parse(selectedExpiry);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        // Group options by strike price for the selected expiry
        Map<Double, List<Option>> optionsByStrike = chain.optionSeries().stream()
                .filter(option -> dateFormat.format(option.expiry()).equals(selectedExpiry))
                .collect(Collectors.groupingBy(Option::strike));
        
        // Sort by strike and populate table
        optionsByStrike.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    double strike = entry.getKey();
                    List<Option> options = entry.getValue();
                    
                    Option callOption = options.stream()
                            .filter(opt -> opt.callPut() == CallPut.CALL)
                            .findFirst().orElse(null);
                    
                    Option putOption = options.stream()
                            .filter(opt -> opt.callPut() == CallPut.PUT)
                            .findFirst().orElse(null);
                    
                    String callPrice = "N/A";
                    String putPrice = "N/A";
                    String impliedVol = "N/A";
                    
                    if (callOption != null) {
                        OptionMarketData callData = data.marketData.get(callOption.symbol());
                        if (callData != null) {
                            callPrice = "$" + priceFormat.format(callData.price());
                            impliedVol = percentFormat.format(callData.impliedVol() * 100);
                        }
                    }
                    
                    if (putOption != null) {
                        OptionMarketData putData = data.marketData.get(putOption.symbol());
                        if (putData != null) {
                            putPrice = "$" + priceFormat.format(putData.price());
                        }
                    }
                    
                    model.addRow(new Object[]{
                        "$" + priceFormat.format(strike),
                        callPrice,
                        putPrice,
                        impliedVol
                    });
                });
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
                    updateInterestRatesTable();
                    updateExpirySelector();
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
                    updateInterestRatesTable();
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
        expirySelector.setEnabled(!loading);
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
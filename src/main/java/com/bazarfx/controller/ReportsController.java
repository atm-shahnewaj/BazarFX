package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.service.ReportService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Marketplace-wide report screen. Generation runs on ReportGenerator's background thread
 * (which itself fans out across an ExecutorService inside ReportService), so this
 * controller just shows a progress indicator and renders whatever comes back.
 */
public class ReportsController {

    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox categoriesBox;
    @FXML private VBox sellersBox;
    @FXML private VBox weeksBox;
    @FXML private Label statusLabel;

    private ReportService.MarketplaceReport lastReport;

    @FXML
    public void initialize() {
        onGenerate();
    }

    @FXML
    private void onGenerate() {
        AppContext ctx = AppContext.get();
        clearBoxes();
        ctx.reportGenerator.generateAsync(
                () -> progressIndicator.setVisible(true),
                report -> {
                    progressIndicator.setVisible(false);
                    lastReport = report;
                    renderReport(report);
                    statusLabel.setText("Report generated.");
                },
                error -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Failed to generate report: " + error.getMessage());
                });
    }

    @FXML
    private void onExport() {
        if (lastReport == null) {
            statusLabel.setText("Generate a report first.");
            return;
        }
        AppContext ctx = AppContext.get();
        String csv = ctx.reportService.exportAsCsv(lastReport);
        Path out = ctx.storage.dataDir().resolve("marketplace_report.csv");
        try {
            Files.writeString(out, csv);
            statusLabel.setText("Exported to " + out);
        } catch (IOException e) {
            statusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    private void clearBoxes() {
        categoriesBox.getChildren().clear();
        sellersBox.getChildren().clear();
        weeksBox.getChildren().clear();
    }

    private void renderReport(ReportService.MarketplaceReport report) {
        report.topCategories.forEach((cat, count) ->
                categoriesBox.getChildren().add(new Label(cat + ": " + count)));
        report.topRatedSellers.forEach(entry ->
                sellersBox.getChildren().add(new Label(entry.getKey() + ": " + String.format("%.1f\u2605", entry.getValue()))));
        report.busiestWeek.forEach((week, count) ->
                weeksBox.getChildren().add(new Label(week + ": " + count + " order(s)")));
    }
}

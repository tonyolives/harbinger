package com.harbinger.bench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;

/**
 * Entry point for {@code make bench}: runs the fixed-seed {@link BenchmarkRunner} and writes the
 * results to {@code benchmarks/} — {@code report.json} (the README's only source of numbers) and
 * {@code tiers.png} (a tier-count bar chart). Pure I/O glue around {@link BenchmarkRunner}, so it's
 * excluded from the coverage gate like {@code DemoRunner}. Run headless.
 */
public final class Benchmark {

    private static final long SEED = 2L;
    private static final int OWNERS = 8;
    private static final int SIGNALS_PER_OWNER = 6;
    private static final Path OUTPUT_DIR = Path.of("benchmarks");

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        BenchmarkReport report = BenchmarkRunner.run(SEED, OWNERS, SIGNALS_PER_OWNER);
        Files.createDirectories(OUTPUT_DIR);
        writeReport(report);
        writeTierChart(report);
        System.out.printf(
                "Benchmark written to %s/ — f1=%.3f, tiers H/W/C=%d/%d/%d, "
                        + "signal-to-lead p50/p95=%d/%dms, throughput=%.0f signals/s%n",
                OUTPUT_DIR, report.f1(), report.hotCount(), report.warmCount(), report.coldCount(),
                report.signalToLeadP50Ms(), report.signalToLeadP95Ms(), report.throughputSignalsPerSec());
    }

    private static void writeReport(BenchmarkReport report) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(OUTPUT_DIR.resolve("report.json").toFile(), report);
    }

    private static void writeTierChart(BenchmarkReport report) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(640).height(400).title("Leads by tier").xAxisTitle("Tier").yAxisTitle("Count")
                .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.5);
        chart.addSeries("homeowners", List.of("HOT", "WARM", "COLD"),
                List.of(report.hotCount(), report.warmCount(), report.coldCount()));
        BitmapEncoder.saveBitmap(chart, OUTPUT_DIR.resolve("tiers").toString(), BitmapEncoder.BitmapFormat.PNG);
    }
}

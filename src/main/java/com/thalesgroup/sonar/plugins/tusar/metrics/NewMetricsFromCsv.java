package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Formula;
import org.sonar.api.measures.MeanAggregationFormula;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Builder;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.SumChildValuesFormula;

/**
 * @deprecated The CSV file is deprecated. Besides, it never worked as intended
 *             when set at the project level instead of the global level.
 */
public class NewMetricsFromCsv implements Metrics {

	private static final String TUSAR_NEW_METRICS_FILE_PATH_KEY = "sonar.tusar.newMetricsFilePaths";

	private static final int TOKENS_MINIMUM_LENGTH = 3;
	private static final int TOKENS_MAXIMUM_LENGTH = 4;
	private static final String COMMENTS = "#";
	private static final String CSV_SEPARATOR = ";";

	private static final int METRIC_NAME = 0;
	private static final int METRIC_TYPE = 1;
	private static final int METRIC_DOMAIN = 2;
	private static final int METRIC_FORMULA = 3;

	private static final Map<String, Formula> ALLOWED_FORMULAE = new HashMap<String, Formula>();
	static {
		ALLOWED_FORMULAE.put("mean-aggregation", new MeanAggregationFormula());
		ALLOWED_FORMULAE.put("mean-aggregation-true", new MeanAggregationFormula(true));
		ALLOWED_FORMULAE.put("sum", new SumChildValuesFormula(false));
		ALLOWED_FORMULAE.put("sum-true", new SumChildValuesFormula(true));
	}

	private static Logger logger = LoggerFactory.getLogger(NewMetrics.class);

	private List<Metric> metrics = new LinkedList<Metric>();

	public NewMetricsFromCsv(Settings settings) throws FileNotFoundException {
		String csvFilePath = settings.getString(TUSAR_NEW_METRICS_FILE_PATH_KEY);
		if (csvFilePath != null) {
			loadNewMetricsFromDeprecatedCsvFile(csvFilePath);
		}
	}

	@Override
	public List<Metric> getMetrics() {
		return metrics;
	}

	private void loadNewMetricsFromDeprecatedCsvFile(String csvFilePath) {
		try {
			File configFile = new File(csvFilePath);
			logger.info("Loading user defined metrics from CSV file {}...", configFile.getAbsoluteFile());
			InputStream input = new FileInputStream(configFile);
			try {
				for (String[] cells : parseCsvContent(input)) {
					Metric metric = createNewMetric(cells);
					if (metric != null) {
						metrics.add(metric);
					}
				}
			} finally {
				input.close();
			}
		} catch (IOException e) {
			logger.error("When loading new metric definitions", e);
		}
	}

	private List<String[]> parseCsvContent(InputStream inputStream) {
		List<String[]> rows = new ArrayList<String[]>();
		Scanner scanner = new Scanner(inputStream);
		int lineNumber = 0;
		while (scanner.hasNextLine()) {
			lineNumber++;
			String line = scanner.nextLine();
			if (!line.startsWith(COMMENTS)) {
				String[] tokens = line.split(CSV_SEPARATOR);
				if (tokens.length >= TOKENS_MINIMUM_LENGTH) {
					if (tokens.length > TOKENS_MAXIMUM_LENGTH) {
						logger.warn("Line {} contains more than {} elements", lineNumber, TOKENS_MAXIMUM_LENGTH);
					}
					rows.add(tokens);
				} else {
					logger.warn("Line {} contains less than {} elements", lineNumber, TOKENS_MINIMUM_LENGTH);
				}
			}
		}
		return rows;
	}

	private Metric createNewMetric(String[] cells) {
		Metric newMetric = new Builder(
		//
				unprettify(cells[METRIC_NAME]), //
		        cells[METRIC_NAME], //
		        Metric.ValueType.valueOf(cells[METRIC_TYPE].toUpperCase()))
		//
		        .setDirection(0) //
		        .setQualitative(false) //
		        .setDomain(cells[METRIC_DOMAIN]) //
		        .create();

		Formula formula = new SumChildValuesFormula(false);
		if (cells.length > TOKENS_MINIMUM_LENGTH) {
			formula = ALLOWED_FORMULAE.get(unprettify(cells[METRIC_FORMULA]));
			if (formula == null) {
				logger.warn("Formula '{}' not found for metric {}", cells[METRIC_FORMULA], cells[METRIC_NAME]);
				formula = new SumChildValuesFormula(false);
			}
		}
		newMetric.setFormula(formula);

		logger.info("New user defined metric '{}' with type '{}' in domain '{}'", cells[METRIC_NAME],
		        cells[METRIC_TYPE], cells[METRIC_DOMAIN]);

		return newMetric;
	}

	private static String unprettify(String string) {
		return string.replace(' ', '_').toLowerCase();
	}
}

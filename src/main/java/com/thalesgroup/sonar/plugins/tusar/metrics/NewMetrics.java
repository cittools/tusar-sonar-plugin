package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Formula;
import org.sonar.api.measures.MeanAggregationFormula;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Builder;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.SumChildValuesFormula;

public class NewMetrics implements Metrics {

	private static final PropertyFieldDefinition NAME = PropertyFieldDefinition.build("name").name("Key/Name")
	        .type(PropertyType.STRING).build();

	private static final EnumSet<ValueType> ALLOWED_VALUE_TYPES = EnumSet.of(
	//
	        ValueType.INT, //
	        ValueType.FLOAT, //
	        ValueType.PERCENT, //
	        ValueType.BOOL, //
	        ValueType.STRING, //
	        ValueType.MILLISEC, //
	        ValueType.LEVEL, //
	        ValueType.RATING);

	private static final PropertyFieldDefinition TYPE = PropertyFieldDefinition.build("type").name("Type")
	        .type(PropertyType.SINGLE_SELECT_LIST).options(prettify(ALLOWED_VALUE_TYPES)).build();

	private static final PropertyFieldDefinition DOMAIN = PropertyFieldDefinition.build("domain").name("Domain")
	        .type(PropertyType.STRING).build();

	private static final Map<String, Formula> ALLOWED_FORMULAE = new HashMap<String, Formula>();
	static {
		ALLOWED_FORMULAE.put("mean-aggregation", new MeanAggregationFormula());
		ALLOWED_FORMULAE.put("mean-aggregation-true", new MeanAggregationFormula(true));
		ALLOWED_FORMULAE.put("sum", new SumChildValuesFormula(false));
		ALLOWED_FORMULAE.put("sum-true", new SumChildValuesFormula(true));
	}

	private static final PropertyFieldDefinition FORMULA = PropertyFieldDefinition.build("formula").name("Formula")
	        .type(PropertyType.SINGLE_SELECT_LIST).options(prettify(ALLOWED_FORMULAE.keySet())).build();

	public static final PropertyDefinition TUSAR_NEW_METRICS_PROPERTY = PropertyDefinition
	        .builder("sonar.tusar.newMetrics")
	        .name("New metric definitions")
	        .category("TUSAR")
	        /*
			 * SonarQube collects all metrics at startup from Metrics
			 * implementations, then persists them into database. As a
			 * consequence, any changes made to this property won't apply until
			 * the next restart. The Manual Metrics do not suffer from the same
			 * problem because they have a dedicated table into the database and
			 * are specifically updated by SonarQube. We could do the same, but
			 * SonarQube usually warns against such approach emphasizing the
			 * private and internal nature of the database structure.
			 */
	        .subCategory("New metrics (need restart)")
	        .description(
	                "New metrics for SonarQube. "
	                        + "This setting is meant to register additional metrics used in TUSAR reports "
	                        + "and whose measures need to be injected into SonarQube. "
	                        + "Note that the added metrics can be used by any other plugins as well and, "
	                        + "conversely, metrics required by TUSAR could be added by any other means "
	                        + "(including the Manual Metrics, but beware that metrics created this way won't "
	                        + "have any aggregation formula and hence won't show most of time if you don't "
	                        + "provide measures up to the project level).") //
	        .type(PropertyType.PROPERTY_SET) //
	        .fields(NAME, TYPE, DOMAIN, FORMULA) //
	        .build();

	private static Logger logger = LoggerFactory.getLogger(NewMetrics.class);

	private List<Metric> metrics = new LinkedList<Metric>();

	@SuppressWarnings("deprecation")
	public NewMetrics(Settings serverOrBatchSettings) throws FileNotFoundException {
		String[] newMetrics = serverOrBatchSettings.getStringArray(TUSAR_NEW_METRICS_PROPERTY.key());
		if (newMetrics != null) {
			for (String newMetric : newMetrics) {
				String prefix = new StringBuilder(TUSAR_NEW_METRICS_PROPERTY.key()).append('.').append(newMetric)
				        .append('.').toString();

				String name = serverOrBatchSettings.getString(prefix + NAME.key());
				String type = serverOrBatchSettings.getString(prefix + TYPE.key());
				String domain = serverOrBatchSettings.getString(prefix + DOMAIN.key());
				String formula = serverOrBatchSettings.getString(prefix + FORMULA.key());

				Metric metric = createNewMetric(name, type, domain, formula);
				if (metric != null) {
					metrics.add(metric);
				}
			}
		} else {
			metrics = new NewMetricsFromCsv(serverOrBatchSettings).getMetrics();
		}
	}

	@Override
	public List<Metric> getMetrics() {
		return metrics;
	}

	private Metric createNewMetric(String name, String typeName, String domain, String formulaName) {
		Metric newMetric;
		if (name != null && !name.isEmpty()) {
			Metric.ValueType type = Metric.ValueType.STRING;
			if (typeName != null) {
				try {
					type = Metric.ValueType.valueOf(typeName.toUpperCase());
				} catch (IllegalArgumentException e) {
					logger.warn("Unknown type '{}' for new metric '{}' replace by '{}'.", typeName, name, type.name());
				}
			}
			newMetric = new Builder(name, name, type).setDomain(domain).create();

			Formula formula = null;
			if (formulaName != null) {
				formula = ALLOWED_FORMULAE.get(unprettify(formulaName));
				if (formula == null) {
					logger.warn("Formula '{}' not found for metric {}", formulaName, name);
				}
			}
			if (formula == null) {
				formula = new SumChildValuesFormula(false);
			}
			newMetric.setFormula(formula);

			logger.info("New user defined metric '{}' with type '{}' in domain '{}'", name, type.name(), domain);
		} else {
			newMetric = null;
		}
		return newMetric;
	}

	private static String[] prettify(Collection<?> values) {
		String[] names = new String[values.size()];
		int i = 0;
		for (Object value : values) {
			names[i++] = prettify(String.valueOf(value));
		}
		return names;
	}

	private static String prettify(String string) {
		StringBuilder name = new StringBuilder();
		for (int i = 0; i < string.length(); ++i) {
			char c = string.charAt(i);
			if (c == '-') {
				c = ' ';
			} else if (i == 0) {
				c = Character.toUpperCase(c);
			} else {
				c = Character.toLowerCase(c);
			}
			name.append(c);
		}
		return name.toString();
	}

	private static String unprettify(String string) {
		return string.replace(' ', '-').toLowerCase();
	}
}

package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor.Report;

public class PostProcessing {

	static final Logger logger = LoggerFactory.getLogger(PostProcessing.class);

	/**
	 * An explicit list since we can't rely on dynamic resolution giving the
	 * same result both server and client side.
	 */
	private static final Map<String, ScriptEngineFactory> SCRIPT_ENGINES = new HashMap<String, ScriptEngineFactory>();
	static {
		ScriptEngineManager manager = new ScriptEngineManager();

		try {
			ScriptEngineFactory factory = new GroovyScriptEngineFactory();
			manager.registerEngineName(factory.getEngineName(), factory);
			logger.info("Successfully loaded Groovy script engine");
			SCRIPT_ENGINES.put(factory.getLanguageName(), factory);
		} catch (Exception e) {
			logger.error("Failed to load Groovy script engine", e);
		}

		// for (ScriptEngineFactory factory : manager.getEngineFactories()) {
		// logger.info("Found script engine factory: {}", factory);
		// ENGINES.put(factory.getLanguageName(), factory);
		// }
	}

	public static final PropertyDefinition TUSAR_POST_PROCESSING_LANGUAGE_PROPERTY = addOptions(
	        SCRIPT_ENGINES.keySet(), PropertyDefinition.builder("sonar.tusar.postProcessingLanguage") //
	                .subCategory("Post-processing") //
	                .name("Language") //
	                .description("Script language.") //
	                .type(PropertyType.SINGLE_SELECT_LIST) //
	                .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)).build();

	private static PropertyDefinition.Builder addOptions(Collection<String> options, PropertyDefinition.Builder builder) {
		List<String> optionList = new ArrayList<String>(options);
		builder.options(optionList);
		builder.defaultValue(optionList.get(0));
		return builder;
	}

	public static final PropertyDefinition TUSAR_POST_PROCESSING_SCRIPT_PROPERTY = PropertyDefinition
	        .builder("sonar.tusar.postProcessingScript")
	        .subCategory("Post-processing")
	        .name("Script")
	        .description(
	                "Path to a post-processing script to apply on TUSAR measures before their injection into the database.") //
	        .type(PropertyType.TEXT) //
	        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE) //
	        .build();

	private ScriptEngine engine;

	private MetricFinder metricFinder;

	public static PostProcessing load(Settings settings, MetricFinder metricFinder) {
		String language = settings.getString(TUSAR_POST_PROCESSING_LANGUAGE_PROPERTY.key());
		String script = settings.getString(TUSAR_POST_PROCESSING_SCRIPT_PROPERTY.key());
		if (script != null && !script.trim().isEmpty()) {
			try {
				return new PostProcessing(language, script, metricFinder);
			} catch (ScriptException e) {
				AbstractSensor.logger.error("When compiling post-processing script", e);
			}
		}
		return null;
	}

	private PostProcessing(String language, String script, MetricFinder metricFinder) throws ScriptException {
		this.metricFinder = metricFinder;
		ScriptEngineFactory factory = SCRIPT_ENGINES.get(language);
		if (factory != null) {
			engine = factory.getScriptEngine();
			engine.eval(script);
		} else {
			throw new ScriptException("No scripting engine found for language " + language);
		}
	}

	public void execute(final SensorContext context, final Resource resource,
	        final Map<Report, Set<Measure>> newMeasures) throws ScriptException {

		PostProcessor processor = new PostProcessor() {

			@Override
			public Resource getResource() {
				return resource;
			}

			@Override
			public Metric findMetric(String metricKey) {
				return metricFinder.findByKey(metricKey);
			}

			@Override
			public Measure findCurrentMeasure(String metricKey) {
				Metric metric = metricFinder.findByKey(metricKey);
				if (metric != null) {
					if (resource == null) {
						return context.getMeasure(metric);
					} else {
						return context.getMeasure(resource, metric);
					}
				} else {
					return null;
				}
			}

			@Override
			public void setMeasure(String metricKey, UpdateMeasureAction action) {
				Metric metric = metricFinder.findByKey(metricKey);
				if (metric != null && action != null) {
					Measure currentMeasure = context.getMeasure(resource, metric);

					Measure resultingMeasure = currentMeasure;
					Map<Report, Measure> injectedMeasures = new HashMap<Report, Measure>();
					for (Map.Entry<Report, Set<Measure>> entry : newMeasures.entrySet()) {
						Report report = entry.getKey();
						Set<Measure> measures = entry.getValue();
						for (Measure measure : measures) {
							if (measure.getMetric().getKey().equals(metricKey)) {
								injectedMeasures.put(report, measure);
								resultingMeasure = measure;
								measures.remove(measure);
								break;
							}
						}
					}

					action.update(metric, currentMeasure, injectedMeasures, resultingMeasure);
					Set<Measure> resultingMeasures = newMeasures.get(null);
					if (resultingMeasures == null) {
						resultingMeasures = new HashSet<Measure>();
						newMeasures.put(null, resultingMeasures);
					}
					resultingMeasures.add(resultingMeasure);
				}
			}
		};

		Bindings bindings = engine.createBindings();
		bindings.put("processor", processor);

		engine.eval("postProcess(processor) ", bindings);
	}
}

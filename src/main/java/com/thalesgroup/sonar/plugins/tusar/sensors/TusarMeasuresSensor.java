package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.tusar.duplications.v1.DuplicationsComplexType;
import com.thalesgroup.tusar.measures.v7.MeasuresComplexType;
import com.thalesgroup.tusar.memory.v1.MemoryComplexType;
import com.thalesgroup.tusar.size.v2.SizeComplexType;
import com.thalesgroup.tusar.v12.Tusar;

public class TusarMeasuresSensor extends AbstractSensor {

	private static Logger logger = LoggerFactory.getLogger(TusarMeasuresSensor.class);

	private static final Metric LI_METRIC = new Metric.Builder("LI", "LI", ValueType.INT).create();

	/*
	 * TUSAR constraints size metric values to the domain {LINES, NCLOC,
	 * INSTRUCTIONS, COMPLEXITY, LI} in v1, to the domain {LOC, NCLOC, COMMENTS,
	 * MIXED, INSTRUCTIONS, COMPLEXITY} in v2 and stop constraining them in v4
	 * and higher.
	 */
	private static final Map<String, Metric> TUSAR_METRIC_TRANSLATIONS = new HashMap<String, Metric>();
	static {
		// Obvious.
		TUSAR_METRIC_TRANSLATIONS.put("LINES", CoreMetrics.LINES);
		TUSAR_METRIC_TRANSLATIONS.put("NCLOC", CoreMetrics.NCLOC);
		TUSAR_METRIC_TRANSLATIONS.put("COMPLEXITY", CoreMetrics.COMPLEXITY);
		// Not so obvious.
		TUSAR_METRIC_TRANSLATIONS.put("INSTRUCTIONS", CoreMetrics.STATEMENTS);
		TUSAR_METRIC_TRANSLATIONS.put("LOC", CoreMetrics.LINES);
		TUSAR_METRIC_TRANSLATIONS.put("COMMENTS", CoreMetrics.COMMENT_LINES);
		// It's... complicated (see MeasuresSensor.ppt).
		TUSAR_METRIC_TRANSLATIONS.put("LI", LI_METRIC);
		TUSAR_METRIC_TRANSLATIONS.put("MIXED", LI_METRIC);
	}

	public static ReferenceExtractor refExtractor = new ReferenceExtractor() {

		@Override
		public Collection<? extends String> getReferencedResourcePaths(Tusar model) {
			List<String> paths = new LinkedList<String>();
			MeasuresComplexType measures = model.getMeasures();
			if (measures != null) {

				SizeComplexType size = model.getMeasures().getSize();
				if (size != null && size.getResource() != null) {
					for (SizeComplexType.Resource resource : size.getResource()) {
						if (!PROJECT_RESSOURCE_TYPE.equalsIgnoreCase(resource.getType())) {
							paths.add(resource.getValue());
						}
					}
				}

				MemoryComplexType memory = model.getMeasures().getMemory();
				if (memory != null && memory.getResource() != null) {
					for (MemoryComplexType.Resource resource : memory.getResource()) {
						if (!PROJECT_RESSOURCE_TYPE.equalsIgnoreCase(resource.getType())) {
							paths.add(resource.getValue());
						}
					}
				}

				DuplicationsComplexType duplications = model.getMeasures().getDuplications();
				if (duplications != null && duplications.getSet() != null) {
					for (DuplicationsComplexType.Set duplicationSet : duplications.getSet()) {
						for (DuplicationsComplexType.Set.Resource resource : duplicationSet.getResource()) {
							paths.add(resource.getPath());
						}
					}
				}
			}
			return paths;
		}
	};

	private MetricFinder metricFinder;

	private Set<Metric> silentlyIgnoredMetrics = new HashSet<Metric>();

	public TusarMeasuresSensor(Settings settings, MetricFinder metricFinder) {
		super(settings, metricFinder);
		this.metricFinder = metricFinder;
	}

	@Override
	protected void internalAnalyse(Context context) {
		silentlyIgnoredMetrics.clear();
		for (ReportExtractor.Report report : context.getReports()) {
			logger.debug("Injecting data from report '{}'", report.location);
			context.report(report);
			MeasuresComplexType measures = report.model.getMeasures();
			if (measures != null) {

				SizeComplexType size = measures.getSize();
				if (size != null) {
					try {
						processSize(context, size);
					} catch (ParseException e) {
						logger.error("Failed to extract size data", e);
					}
				}

				MemoryComplexType memory = measures.getMemory();
				if (memory != null) {
					try {
						processMemory(context, memory);
					} catch (ParseException e) {
						logger.error("Failed to extract memory data", e);
					}
				}

				DuplicationsComplexType duplications = measures.getDuplications();
				if (duplications != null) {
					try {
						processDuplications(context, duplications);
					} catch (ParseException e) {
						logger.error("Failed to extract duplication data", e);
					}
				}
			}
		}
	}

	private void injectMeasure(Context context, String key, String value) {
		Metric metric = metricFinder.findByKey(key);
		if (metric != null) {
			context.injectMeasure(createMeasure(metric, value));
		} else {
			if (silentlyIgnoredMetrics.add(metric)) {
				logger.warn("Skipped measure using unknown metric '{}'", key);
			}
		}
	}

	// TODO Use org.sonar.api.utils.ParsingUtils? Language dependency?
	private Measure createMeasure(Metric metric, String value) {
		Measure measure;

		switch (metric.getType()) {

		case STRING:
			measure = new Measure(metric, value).setPersistenceMode(PersistenceMode.DATABASE);
			break;

		case LEVEL:
			measure = new Measure(metric, Level.valueOf(value));
			break;

		case FLOAT:
		case PERCENT:
		case INT:
		case MILLISEC:
		case RATING:
			measure = new Measure(metric, Double.valueOf(value));
			break;

		default:
			throw new IllegalArgumentException("Unknown type for metric: " + metric.getType());
		}

		return measure;
	}

	/**
	 * The TUSAR content is labelled as "size" , but any metric domain will be
	 * accepted. It was already the case in the former versions thanks to the
	 * "new measures" mechanism, it is now becoming the rule as we are not
	 * performing any kind of filering anymore: if a metric exists (native,
	 * manual, new, etc.), you can add measures for it.
	 */
	private void processSize(Context context, SizeComplexType size) throws ParseException {
		for (SizeComplexType.Resource element : size.getResource()) {
			assert ReferenceExtractor.FILE_RESSOURCE_TYPE.equalsIgnoreCase(element.getType());
			Resource resource = context.resolveResource(element.getType(), element.getValue());
			if (resource != null) {
				context.resource(resource);
				for (SizeComplexType.Resource.Measure measure : element.getMeasure()) {
					injectMeasure(context.resource(resource), translate(measure), measure.getValue());
				}
			}
		}
	}

	private String translate(SizeComplexType.Resource.Measure measure) {
		Metric metric = TUSAR_METRIC_TRANSLATIONS.get(measure.getKey());
		if (metric != null) {
			if (metric == LI_METRIC) {
				if (silentlyIgnoredMetrics.add(metric)) {
					logger.warn("TUSAR report contains measures for the unsupported {} metric.", measure.getKey());
				}
			}
			return metric.getKey();
		} else {
			return measure.getKey();
		}
	}

	private void processMemory(Context context, MemoryComplexType memory) throws ParseException {
		for (MemoryComplexType.Resource element : memory.getResource()) {
			assert ReferenceExtractor.FILE_RESSOURCE_TYPE.equalsIgnoreCase(element.getType());
			Resource resource = context.resolveResource(element.getType(), element.getValue());
			if (resource != null) {
				context.resource(resource);
				for (MemoryComplexType.Resource.Measure measure : element.getMeasure()) {
					// TODO Constraint keys?
					injectMeasure(context.resource(resource), measure.getKey(), measure.getValue());
				}
			}
		}
	}

	private void processDuplications(Context context, DuplicationsComplexType duplications) throws ParseException {

		Map<Resource, DuplicationData> duplicationDataPerFile = new HashMap<Resource, DuplicationData>();

		for (DuplicationsComplexType.Set duplicationSet : duplications.getSet()) {
			List<DuplicationsComplexType.Set.Resource> duplicationResources = duplicationSet.getResource();

			boolean isSingleFile = true;
			out: for (DuplicationsComplexType.Set.Resource dr1 : duplicationResources) {
				for (DuplicationsComplexType.Set.Resource dr2 : duplicationResources) {
					if (!(dr1.getPath().equalsIgnoreCase(dr2.getPath()))) {
						isSingleFile = false;
						break out;
					}
				}
			}

			for (DuplicationsComplexType.Set.Resource dr1 : duplicationResources) {
				Resource r1 = context.resolveResource(dr1.getPath());
				if (r1 != null) {
					DuplicationData duplicationData = getDuplicationData(context, duplicationDataPerFile, r1,
					        dr1.getPath());
					HashSet<DuplicationData.Part> parts = new HashSet<DuplicationData.Part>();
					for (DuplicationsComplexType.Set.Resource dr2 : duplicationResources) {
						if (dr1 != dr2) {
							Resource r2 = context.resolveResource(dr2.getPath());
							if (r2 != null) {
								parts.add(new DuplicationData.Part( //
								        context.context().getResource(r2).getEffectiveKey(), dr2.getLine()));
							}
						}
					}
					duplicationData.cumulate(parts, //
					        Integer.parseInt(dr1.getLine()), //
					        Integer.parseInt(duplicationSet.getLines()));
				}

				if (isSingleFile) {
					break;
				}
			}
		}

		for (Map.Entry<Resource, DuplicationData> entry : duplicationDataPerFile.entrySet()) {
			saveData(context, entry.getValue(), entry.getKey());
		}
	}

	private DuplicationData getDuplicationData(Context context, Map<Resource, DuplicationData> fileContainer,
	        Resource file, String resourcePath) {
		DuplicationData data = fileContainer.get(file);
		if (data == null) {
			data = new DuplicationData(context.context(), file, resourcePath);
			fileContainer.put(file, data);
		}
		return data;
	}

	private void saveData(Context context, DuplicationData data, Resource targetResource) {
		context.resource(targetResource);
		context.injectMeasure(CoreMetrics.DUPLICATED_FILES, 1d);
		context.injectMeasure(CoreMetrics.DUPLICATED_LINES, data.getOverallDuplicatedLineCount());
		context.injectMeasure(CoreMetrics.DUPLICATED_BLOCKS, data.getDuplicatedBlockCount());
		context.injectMeasure(CoreMetrics.DUPLICATIONS_DATA, data.getDuplicationXMLData());
	}
}

package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor.Report;

public abstract class AbstractSensor implements Sensor {

	static final Logger logger = LoggerFactory.getLogger(AbstractSensor.class);

	public static class Context {

		// TODO Memory cost?
		private final Map<Resource, Map<Report, Set<Measure>>> injectedMeasures = new HashMap<Resource, Map<Report, Set<Measure>>>();

		private final Project project;

		private final SensorContext sensorContext;

		private Report report;

		private Resource resource;

		public Context(Project project, SensorContext sensorContext) {
			if (project == null) {
				throw new IllegalArgumentException("No project defined");
			}
			this.project = project;
			if (sensorContext == null) {
				throw new IllegalArgumentException("No sensor context defined");
			}
			this.sensorContext = sensorContext;
		}

		public Context report(Report report) {
			this.report = report;
			return this;
		}

		public Context resource(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Project project() {
			return project;
		}

		public SensorContext context() {
			return sensorContext;
		}

		public Report report() {
			return report;
		}

		public Resource resource() {
			return resource;
		}

		public Collection<? extends Report> getReports() {
			return getExtractor(project).getReports();
		}

		public Resource resolveResource(String type, String path) {
			if (ReferenceExtractor.PROJECT_RESSOURCE_TYPE.equalsIgnoreCase(type)) {
				return project;
			} else {
				return resolveResource(path);
			}
		}

		public Resource resolveResource(String path) {
			java.io.File file = getExtractor(project).getCanonicalFile(path);
			File resource;
			if (file != null) {
				File reference = File.fromIOFile(file, project);
				resource = sensorContext.getResource(reference);
				if (resource == null) {
					logger.warn("TUSAR resource does exist, but it is not indexed: {}", path);
				}
			} else {
				logger.warn("TUSAR resource doesn't exists: {}", path);
				resource = null;
			}
			return resource;
		}

		private ReportExtractor getExtractor(Project project) {
			ReportExtractor reportExtractor = ReportExtractor.getInstance(project);
			if (reportExtractor != null) {
				return reportExtractor;
			} else {
				throw new RuntimeException("No report extractor instance!");
			}
		}

		public void injectMeasure(Measure measure) {
			if (report == null) {
				throw new IllegalArgumentException("No report defined");
			}
			if (resource == null) {
				throw new IllegalArgumentException("No resource defined");
			}

			Map<Report, Set<Measure>> measuresByReport = injectedMeasures.get(resource);
			if (measuresByReport == null) {
				measuresByReport = new HashMap<Report, Set<Measure>>();
				injectedMeasures.put(resource, measuresByReport);
			}

			Set<Measure> measures = measuresByReport.get(report);
			if (measures == null) {
				measures = new HashSet<Measure>();
				measuresByReport.put(report, measures);
			}

			measures.add(measure);
		}

		public void injectMeasure(Metric metric, double value) {
			injectMeasure(new Measure(metric, value));
		}

		public void injectMeasure(Metric metric, int value) {
			injectMeasure(new Measure(metric, (double) value));
		}

		public void injectMeasure(Metric metric, String value) {
			injectMeasure(new Measure(metric, value).setPersistenceMode(PersistenceMode.DATABASE));
		}
	}

	private Settings settings;

	private MetricFinder metricFinder;

	public AbstractSensor(Settings settings, MetricFinder metricFinder) {
		this.settings = settings;
		this.metricFinder = metricFinder;
	}

	@Override
	public boolean shouldExecuteOnProject(Project project) {
		return true;
	}

	@Override
	public void analyse(Project project, SensorContext sensorContext) {
		Context context = new Context(project, sensorContext);
		internalAnalyse(context);

		PostProcessing postProcessing = null;
		if (metricFinder != null) {
			postProcessing = PostProcessing.load(settings, metricFinder);
		}

		for (Map.Entry<Resource, Map<Report, Set<Measure>>> entry : context.injectedMeasures.entrySet()) {
			Resource resource = entry.getKey();
			Map<Report, Set<Measure>> newMeasures = entry.getValue();

			if (postProcessing != null) {
				try {
					postProcessing.execute(sensorContext, resource, newMeasures);
				} catch (ScriptException e) {
					logger.error("When executing post-processing script", e);
				}
			}

			// Report grouping is only relevant for post-processing.
			for (Set<Measure> measures : newMeasures.values()) {
				for (Measure measure : measures) {
					saveMeasure(sensorContext, resource, measure);
				}
			}
		}
	}

	protected abstract void internalAnalyse(Context context);

	protected void saveMeasure(SensorContext context, Resource resource, Measure measure) {
		Measure oldMeasure = context.getMeasure(resource, measure.getMetric());
		Measure newMeasure;
		if (oldMeasure != null) {
			newMeasure = oldMeasure;
		} else {
			newMeasure = measure;
		}

		if (logger.isDebugEnabled()) {
			String designation = resource.getName() != null ? resource.getName() : resource.getPath();
			String operation = newMeasure == measure ? "+" : "~";
			logger.debug("[{}] measure: {} {} / {} -> value = {}, data = {}", operation, resource.getQualifier(),
			        designation, newMeasure.getMetricKey(), newMeasure.getValue(), newMeasure.getData());
		}

		if (oldMeasure != null) {
			oldMeasure.setValue(measure.getValue());
			oldMeasure.setData(measure.getData());
		} else {
			context.saveMeasure(resource, measure);
		}

	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}

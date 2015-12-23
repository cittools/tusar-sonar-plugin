package com.thalesgroup.sonar.plugins.tusar;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * Debug decorator to dump measures and issues on every indexed resource.
 */
@DependsUpon(value = DecoratorBarriers.END_OF_TIME_MACHINE)
public class ResourceDumpDecorator implements Decorator {

	private static final Logger logger = LoggerFactory.getLogger(ResourceDumpDecorator.class);

	public static void dumpSettings(Settings settings) {
		StringBuilder dump = new StringBuilder("Settings:").append('\n');
		for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
			dump.append('\t');
			dump.append(entry.getKey()).append("\t-> ");
			dump.append(entry.getValue()).append('\n');
		}
		logger.error(dump.toString());
	}

	private ResourcePerspectives perspectives;

	public ResourceDumpDecorator(ResourcePerspectives perspectives) {
		this.perspectives = perspectives;
		if (logger.isDebugEnabled()) {
			logger.debug(dumpMetrics());
		}
	}

	@Override
	public boolean shouldExecuteOnProject(Project project) {
		return true;
	}

	@Override
	public void decorate(final Resource resource, final DecoratorContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug(dumpResource(resource, context));
		}
	}

	private String dumpResource(Resource resource, DecoratorContext context) {
		StringBuilder text = new StringBuilder();

		text.append("Project: ").append(context.getProject().getName()).append('\n');
		text.append("\tResource: ").append(resource).append('\n');

		text.append("\tMeasures:\n");
		Collection<Measure> measures = context.getMeasures(new MeasuresFilter<Collection<Measure>>() {
			public Collection<Measure> filter(Collection<Measure> measures) {
				return measures;
			}
		});
		for (Measure measure : measures) {
			text.append("\t\t").append(measure.getMetric().getKey());
			text.append(" (").append(measure.getMetric().getName());
			text.append(") value = ").append(measure.getValue());
			text.append(", data = ").append(measure.getData()).append('\n');
		}

		Issuable issuable = perspectives.as(Issuable.class, resource);
		if (issuable != null) {
			text.append("\tIssues:\n");
			for (Issue issue : issuable.issues()) {
				text.append("\t\t").append(issue.ruleKey());
				text.append(" (").append(issue.message()).append(")");
				text.append(" <").append(issue.severity()).append(">");
				text.append(" @ line ").append(issue.line());
				text.append(" => ").append(issue.effortToFix()).append("\n");
			}
		}

		return text.toString();
	}

	private String dumpMetrics() {
		String[] columns = {
		        //
		        "Key", //
		        "Name", //
		        "Domain", //
		        "Description", //
		        "Origin", //
		        "Type", //
		        "Qualitative", //
		        "Formula", //
		        "Direction", //
		        "BestValue", //
		        "WorstValue" //
		};
		Map<String, Method> getters = new HashMap<String, Method>();
		for (String column : columns) {
			try {
				getters.put(column, Metric.class.getMethod("get" + column));
			} catch (NoSuchMethodException e) {
				logger.error("When retreiving getter for " + columns, e);
			} catch (SecurityException e) {
				logger.error("When retreiving getter for " + columns, e);
			}
		}

		StringBuilder text = new StringBuilder("All metrics:\n");
		for (String column : columns) {
			text.append(column).append(",");
		}
		text.append("Deprecated");
		text.append("\n");
		for (Field field : CoreMetrics.class.getFields()) {
			if (Metric.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
				try {
					Metric metric = (Metric) field.get(null);
					for (String column : columns) {
						try {
							text.append(getters.get(column).invoke(metric)).append(",");
						} catch (InvocationTargetException e) {
							logger.error("When invoking getter for " + column, e);
						}
					}
					text.append(field.getAnnotation(Deprecated.class) != null ? "true" : "false");
					text.append("\n");
				} catch (IllegalArgumentException e) {
					logger.error("When accessing field " + field, e);
				} catch (IllegalAccessException e) {
					logger.error("When accessing field " + field, e);
				}
			}
		}
		return text.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}

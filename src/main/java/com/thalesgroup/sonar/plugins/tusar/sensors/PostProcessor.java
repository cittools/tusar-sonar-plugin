package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.Map;

import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;

import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor.Report;

/**
 * Post-processing API to ease script action writing.
 */
public interface PostProcessor {

	Resource getResource();

	/**
	 * Search a metric using its case sensitive key.
	 * 
	 * @param metricKey
	 *            The key of the metric (case sensitive).
	 */
	Metric findMetric(String metricKey);

	/**
	 * Search the existing measure for a metric using its metric's key.
	 * 
	 * @param metricKey
	 *            The key of the measure's metric (case sensitive).
	 */
	Measure findCurrentMeasure(String keymetricKey);

	/**
	 * Set the measure for a given metric using a action. The provided action
	 * won't be invoked if the metric doesn't exist.
	 */
	void setMeasure(String metricKey, PostProcessor.UpdateMeasureAction action);

	interface UpdateMeasureAction {

		void update(Metric metric, Measure currentMeasure, Map<Report, Measure> injectedMeasures,
		        Measure resultingMeasure);
	}
}

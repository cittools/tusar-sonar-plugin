package com.thalesgroup.sonar.plugins.tusar;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

import com.thalesgroup.sonar.plugins.tusar.metrics.AcceptanceMetrics;
import com.thalesgroup.sonar.plugins.tusar.metrics.AcceptanceWidget;
import com.thalesgroup.sonar.plugins.tusar.metrics.MemoryMetrics;
import com.thalesgroup.sonar.plugins.tusar.metrics.MemoryWidget;
import com.thalesgroup.sonar.plugins.tusar.metrics.NewMetrics;
import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.sonar.plugins.tusar.rules.TusarCommonRulesEngine;
import com.thalesgroup.sonar.plugins.tusar.rules.TusarProfileDefinition;
import com.thalesgroup.sonar.plugins.tusar.rules.TusarRuleDefinitions;
import com.thalesgroup.sonar.plugins.tusar.sensors.PostProcessing;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarCoverageSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarMeasuresSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarTestsSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarViolationsSensor;

public class TusarPlugin extends SonarPlugin {

	public TusarPlugin() {
		for (ReferenceExtractor refExtractor : Arrays.asList( //
		        TusarMeasuresSensor.refExtractor, //
		        TusarTestsSensor.refExtractor, //
		        TusarCoverageSensor.refExtractor, //
		        TusarViolationsSensor.refExtractor)) {
			ReportExtractor.registerReferenceExtractor(refExtractor);
		}
	}

	@Override
	public List<?> getExtensions() {
		return Arrays.asList( //
		        ReportExtractor.TUSAR_REPORTS_PATHS_PROPERTY, //
		        ReportExtractor.TUSAR_USE_PLACEHOLDER_PROPERTY, //
		        NewMetrics.TUSAR_NEW_METRICS_PROPERTY, //
		        PostProcessing.TUSAR_POST_PROCESSING_LANGUAGE_PROPERTY, //
		        PostProcessing.TUSAR_POST_PROCESSING_SCRIPT_PROPERTY, //

		        TusarProjectBuilder.class, //
		        TusarLanguage.class, //

		        AcceptanceMetrics.class, //
		        AcceptanceWidget.class, //
		        MemoryMetrics.class, //
		        MemoryWidget.class, //
		        NewMetrics.class, //

		        TusarProfileDefinition.class, //
		        TusarRuleDefinitions.class, //
		        TusarCommonRulesEngine.class, //

		        TusarMeasuresSensor.class, //
		        TusarTestsSensor.class, //
		        TusarCoverageSensor.class, //
		        TusarViolationsSensor.class //
		        );
	}
}

package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;

import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.tusar.coverage.v5.CoverageComplexType;
import com.thalesgroup.tusar.generic_branch_coverage.v1.GenericBranchCoverageComplexType;
import com.thalesgroup.tusar.line_coverage.v1.LineCoverageComplexType;
import com.thalesgroup.tusar.v12.Tusar;

public class TusarCoverageSensor extends AbstractSensor {

	private static Logger logger = LoggerFactory.getLogger(TusarCoverageSensor.class);

	public static ReferenceExtractor refExtractor = new ReferenceExtractor() {

		@Override
		public Collection<? extends String> getReferencedResourcePaths(Tusar model) {
			List<String> paths = new LinkedList<String>();
			CoverageComplexType coverage = model.getCoverage();
			if (coverage != null) {

				LineCoverageComplexType lineCoverage = coverage.getLineCoverage();
				if (lineCoverage != null && lineCoverage.getFile() != null) {
					for (LineCoverageComplexType.File file : lineCoverage.getFile()) {
						paths.add(file.getPath());
					}
				}

				// Only one conditional coverage can be processed at a time
				// Order of processing : MC/DC, Multi-condition,
				// Condition-decision, Condition, Branch, Decision
				List<GenericBranchCoverageComplexType> genericBranchCoverages = getGenericBranchCoverages(coverage);
				for (GenericBranchCoverageComplexType genericBranchCoverage : genericBranchCoverages) {
					if (genericBranchCoverage != null && genericBranchCoverage.getResource() != null) {
						for (GenericBranchCoverageComplexType.Resource resource : genericBranchCoverage.getResource()) {
							if (!PROJECT_RESSOURCE_TYPE.equalsIgnoreCase(resource.getType())) {
								paths.add(resource.getFullname());
							}
						}
						break;
					}
				}
			}
			return paths;
		}
	};

	private boolean lineCoverageInTusar;

	public TusarCoverageSensor(Settings settings, MetricFinder metricFinder) {
		super(settings, metricFinder);
	}

	@Override
	public void analyse(Project project, SensorContext sensorContext) {
		super.analyse(project, sensorContext);

		// Set the coverage of files without coverage metrics
		if (lineCoverageInTusar) {
			setLineCoverageToZero(project, sensorContext);
		}

		// We have to execute this method all the time because if the metric
		// "conditions_to_cover" have been previously injected,
		// Sonar will automatically set to 100% the branch coverage
		setGenericBranchCoverageToZero(project, sensorContext);
	}

	@Override
	protected void internalAnalyse(Context context) {
		for (ReportExtractor.Report report : context.getReports()) {
			logger.debug("Injecting data from report '{}'", report.location);
			context.report(report);
			try {
				CoverageComplexType coverage = report.model.getCoverage();
				if (coverage != null) {

					LineCoverageComplexType lineCoverage = coverage.getLineCoverage();
					if (lineCoverage != null) {
						processLineCoverage(context, lineCoverage);
					}

					// Only one conditional coverage can be processed at a time
					// Order of processing : MC/DC, Multi-condition,
					// Condition-decision, Condition, Branch, Decision
					List<GenericBranchCoverageComplexType> genericBranchCoverages = getGenericBranchCoverages(coverage);
					for (GenericBranchCoverageComplexType genericBranchCoverage : genericBranchCoverages) {
						if (genericBranchCoverage != null) {
							processGenericBranchCoverage(context, genericBranchCoverage);
							break;
						}
					}
				}
			} catch (ParseException e) {
				logger.error("Failed to extract coverage data", e);
			}
		}
	}

	private void processGenericBranchCoverage(Context context, GenericBranchCoverageComplexType genericBranchCoverage)
	        throws ParseException {
		for (GenericBranchCoverageComplexType.Resource path : genericBranchCoverage.getResource()) {
			if (ReferenceExtractor.FILE_RESSOURCE_TYPE.equalsIgnoreCase(path.getType())) {
				Resource resource = context.resolveResource(path.getFullname());
				if (resource != null) {
					context.resource(resource);
					double conditionsToCover = 0;
					double uncoveredConditions = 0;
					PropertiesBuilder<String, Integer> conditionsByLine = new PropertiesBuilder<String, Integer>(
					        CoreMetrics.CONDITIONS_BY_LINE);
					PropertiesBuilder<String, Integer> coveredConditionsByLine = new PropertiesBuilder<String, Integer>(
					        CoreMetrics.COVERED_CONDITIONS_BY_LINE);
					PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(
					        CoreMetrics.COVERAGE_LINE_HITS_DATA);

					for (GenericBranchCoverageComplexType.Resource.Line line : path.getLine()) {
						int numberOfBranches = (int) ParsingUtils.parseNumber(line.getNumberOfBranches(),
						        Locale.ENGLISH);
						int uncoveredBranches = (int) ParsingUtils.parseNumber(line.getUncoveredBranches(),
						        Locale.ENGLISH);

						conditionsToCover += numberOfBranches;
						uncoveredConditions += uncoveredBranches;

						conditionsByLine.add(line.getNumber(), numberOfBranches);
						coveredConditionsByLine.add(line.getNumber(), numberOfBranches - uncoveredBranches);
						lineHitsBuilder.add(line.getNumber(), 1);

					}

					Measure coverageLineHitsData = context.context().getMeasure(resource,
					        CoreMetrics.COVERAGE_LINE_HITS_DATA);

					context.injectMeasure(CoreMetrics.CONDITIONS_TO_COVER, conditionsToCover);
					context.injectMeasure(CoreMetrics.UNCOVERED_CONDITIONS, uncoveredConditions);
					context.injectMeasure(CoreMetrics.BRANCH_COVERAGE,
					        calculatePercent(conditionsToCover - uncoveredConditions, conditionsToCover));
					context.injectMeasure(conditionsByLine.build().setPersistenceMode(PersistenceMode.DATABASE));
					context.injectMeasure(coveredConditionsByLine.build().setPersistenceMode(PersistenceMode.DATABASE));
					if (coverageLineHitsData == null) {
						context.injectMeasure(lineHitsBuilder.build());
					}
				} else {
					logger.info(path.getFullname() + " null");
				}
			}
		}
		logger.info("End process gbc");
	}

	private void setGenericBranchCoverageToZero(Resource resource, SensorContext sensorContext) {
		if (resource != null) {
			for (Resource child : sensorContext.getChildren(resource)) {
				setGenericBranchCoverageToZero(child, sensorContext);
				if (child.getScope() == Qualifiers.FILE) {

					Measure conditionsToCover = sensorContext.getMeasure(child, CoreMetrics.CONDITIONS_TO_COVER);
					Measure branchCoverage = sensorContext.getMeasure(child, CoreMetrics.BRANCH_COVERAGE);
					Measure uncoveredConditions = sensorContext.getMeasure(child, CoreMetrics.UNCOVERED_CONDITIONS);

					if (branchCoverage == null && uncoveredConditions == null && conditionsToCover != null) {
						double conditionsToCoverValue = conditionsToCover.getValue();
						saveMeasure(sensorContext, child, new Measure(CoreMetrics.BRANCH_COVERAGE, 0.0));
						saveMeasure(sensorContext, child, new Measure(CoreMetrics.UNCOVERED_CONDITIONS,
						        conditionsToCoverValue));
					}
				}
			}
		}
	}

	private void processLineCoverage(Context context, LineCoverageComplexType lineCoverage) throws ParseException {
		for (LineCoverageComplexType.File file : lineCoverage.getFile()) {
			lineCoverageInTusar = true;
			Resource resource = context.resolveResource(file.getPath());
			if (resource != null) {
				context.resource(resource);

				double lines = 0;
				double coveredLines = 0;
				PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(
				        CoreMetrics.COVERAGE_LINE_HITS_DATA);

				for (LineCoverageComplexType.File.Line line : file.getLine()) {
					lines++;
					int hits = (int) ParsingUtils.parseNumber(line.getHits(), Locale.ENGLISH);
					if (hits > 0) {
						coveredLines++;
					}
					lineHitsBuilder.add(line.getNumber(), hits);
				}

				context.injectMeasure(CoreMetrics.LINES_TO_COVER, lines);
				context.injectMeasure(CoreMetrics.LINE_COVERAGE, calculatePercent(coveredLines, lines));
				context.injectMeasure(CoreMetrics.UNCOVERED_LINES, lines - coveredLines);
				context.injectMeasure(lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
			}
		}
	}

	private void setLineCoverageToZero(Resource resource, SensorContext sensorContext) {
		if (resource != null) {
			for (Resource child : sensorContext.getChildren(resource)) {
				setLineCoverageToZero(child, sensorContext);
				if (child.getScope() == Qualifiers.FILE) {

					Measure linesToCover = sensorContext.getMeasure(child, CoreMetrics.LINES_TO_COVER);
					Measure lineCoverage = sensorContext.getMeasure(child, CoreMetrics.LINE_COVERAGE);
					Measure uncoveredLines = sensorContext.getMeasure(child, CoreMetrics.UNCOVERED_LINES);
					Measure statements = sensorContext.getMeasure(child, CoreMetrics.STATEMENTS);

					if (lineCoverage == null && linesToCover == null && uncoveredLines == null && statements != null) {
						double statementsValue = statements.getValue();
						saveMeasure(sensorContext, child, new Measure(CoreMetrics.LINE_COVERAGE, 0.0));
						saveMeasure(sensorContext, child, new Measure(CoreMetrics.LINES_TO_COVER, statementsValue));
						saveMeasure(sensorContext, child, new Measure(CoreMetrics.UNCOVERED_LINES, statementsValue));
					}
				}
			}
		}
	}

	private static List<GenericBranchCoverageComplexType> getGenericBranchCoverages(CoverageComplexType coverage) {
		List<GenericBranchCoverageComplexType> genericBranchCoverages = new ArrayList<GenericBranchCoverageComplexType>(
		        6);
		genericBranchCoverages.add(coverage.getModifiedConditionDecisionCoverage());
		genericBranchCoverages.add(coverage.getMultiConditionCoverage());
		genericBranchCoverages.add(coverage.getConditionDecisionCoverage());
		genericBranchCoverages.add(coverage.getConditionCoverage());
		genericBranchCoverages.add(coverage.getBranchCoverage());
		genericBranchCoverages.add(coverage.getDecisionCoverage());

		return genericBranchCoverages;
	}

	private double calculatePercent(double coveredElements, double elements) {
		if (elements > 0) {
			return ParsingUtils.scaleValue(100.0 * coveredElements / elements);
		} else {
			return 0;
		}
	}
}

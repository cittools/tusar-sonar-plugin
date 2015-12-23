package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;

import com.thalesgroup.sonar.plugins.tusar.metrics.AcceptanceMetrics;
import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.tusar.tests.v5.TestsComplexType;
import com.thalesgroup.tusar.v12.Tusar;

public class TusarTestsSensor extends AbstractSensor {

	private static final Logger logger = LoggerFactory.getLogger(TusarTestsSensor.class);

	public static ReferenceExtractor refExtractor = new ReferenceExtractor() {

		@Override
		public Collection<? extends String> getReferencedResourcePaths(Tusar model) {
			List<String> paths = new LinkedList<String>();
			TestsComplexType tests = model.getTests();
			if (tests != null) {
				for (TestsComplexType.Testsuite testSuite : tests.getTestsuite()) {
					paths.add(testSuite.getName());
				}
			}
			return paths;
		}
	};

	@Override
	protected void internalAnalyse(Context context) {
		for (ReportExtractor.Report report : context.getReports()) {
			logger.debug("Injecting data from report '{}'", report.location);
			context.report(report);
			TestsComplexType tests = report.model.getTests();
			if (tests != null) {
				String toolname = tests.getToolname();
				try {
					// TODO Koundousserie to be refactored one day.
					if ("fitnesse".equalsIgnoreCase(toolname)) {
						processAcceptanceTestsData(context, extractFitnesseTestsData(tests));
					} else {
						processTestsData(context, extractTestsData(tests));
					}
				} catch (XMLStreamException e) {
					logger.error("Failed to extract test data for tool " + toolname, e);
				}
			}
		}
	}

	public TusarTestsSensor(Settings settings, MetricFinder metricFinder) {
		super(settings, metricFinder);
	}

	private Collection<TestSuiteReport> extractFitnesseTestsData(TestsComplexType tests) throws XMLStreamException {
		Map<String, TestSuiteReport> reportsPerPath = new HashMap<String, TestSuiteReport>();

		for (TestsComplexType.Testsuite testSuite : tests.getTestsuite()) {
			TestSuiteReport testSuiteReport = new TestSuiteReport(testSuite.getName());

			testSuiteReport.setTestSuite(true);
			testSuiteReport.setErrors(Integer.parseInt(testSuite.getErrors()));
			testSuiteReport.setFailures(Integer.parseInt(testSuite.getFailures()));
			testSuiteReport.setSkipped(Integer.parseInt(testSuite.getSkipped()));
			testSuiteReport.setTests(Integer.parseInt(testSuite.getTests()));
			testSuiteReport.setTimeMS(Integer.parseInt(testSuite.getTime()));

			reportsPerPath.put(testSuite.getName(), testSuiteReport);
		}

		return reportsPerPath.values();
	}

	private Collection<TestSuiteReport> extractTestsData(TestsComplexType tests) throws XMLStreamException {
		Map<String, TestSuiteReport> reportsPerPath = new HashMap<String, TestSuiteReport>();

		for (TestsComplexType.Testsuite testSuite : tests.getTestsuite()) {
			for (TestsComplexType.Testsuite.Testcase testCase : testSuite.getTestcase()) {

				String testSuitePath = testSuite.getName();

				TestSuiteReport testSuiteReport = reportsPerPath.get(testSuitePath);
				if (testSuiteReport == null) {
					testSuiteReport = new TestSuiteReport(testSuitePath);
					reportsPerPath.put(testSuitePath, testSuiteReport);
				}

				TestCaseDetails testCaseDetails = new TestCaseDetails();

				testCaseDetails.setName(testCase.getTestname());
				if (testCase.getTime() == null) {
					testCaseDetails.setTimeMS(0);
				} else {
					testCaseDetails.setTimeMS(getTimeAttributeInMS(testCase.getTime()).intValue());
				}

				String testCaseStatus = TestCaseDetails.STATUS_OK;

				if (testCase.getFailure() != null) {
					testCaseStatus = TestCaseDetails.STATUS_FAILURE;
					testCaseDetails.setErrorMessage(testCase.getFailure().getMessage());
					testCaseDetails.setStackTrace(testCase.getFailure().getContent());

					// cumulate data for test suite
					testSuiteReport.setFailures(testSuiteReport.getFailures() + 1);
				} else if (testCase.getError() != null) {
					testCaseStatus = TestCaseDetails.STATUS_ERROR;
					testCaseDetails.setErrorMessage(testCase.getError().getMessage());
					testCaseDetails.setStackTrace(testCase.getError().getContent());

					// cumulate data for test suite
					testSuiteReport.setErrors(testSuiteReport.getErrors() + 1);
				} else if (testCase.getSkipped() != null) {
					testCaseStatus = TestCaseDetails.STATUS_SKIPPED;

					// cumulate data for test suite
					testSuiteReport.setSkipped(testSuiteReport.getSkipped() + 1);
				}

				testCaseDetails.setStatus(testCaseStatus);

				testSuiteReport.setTests(testSuiteReport.getTests() + 1);
				testSuiteReport.setTimeMS(testSuiteReport.getTimeMS() + testCaseDetails.getTimeMS());
				testSuiteReport.getDetails().add(testCaseDetails);
			}
		}

		return reportsPerPath.values();
	}

	private void processTestsData(Context context, Collection<TestSuiteReport> testSuiteReports) {
		for (TestSuiteReport testSuiteReport : testSuiteReports) {
			Resource resource = context.resolveResource(testSuiteReport.getPath());
			if (resource != null) {
				context.resource(resource);

				double tests = testSuiteReport.getTests();
				double errors = testSuiteReport.getErrors();
				double failures = testSuiteReport.getFailures();
				double skipped = testSuiteReport.getSkipped();
				double passed = tests - errors - failures;

				context.injectMeasure(CoreMetrics.TESTS, tests);
				context.injectMeasure(CoreMetrics.TEST_ERRORS, errors);
				context.injectMeasure(CoreMetrics.TEST_FAILURES, failures);
				context.injectMeasure(CoreMetrics.SKIPPED_TESTS, skipped);
				context.injectMeasure(CoreMetrics.TEST_EXECUTION_TIME, testSuiteReport.getTimeMS());
				if (tests > 0) {
					double percentage = ParsingUtils.scaleValue(passed * 100 / tests);
					context.injectMeasure(CoreMetrics.TEST_SUCCESS_DENSITY, percentage);
				}

				String testCaseDetails = generateTestsDetails(testSuiteReport);
				context.injectMeasure(new Measure(CoreMetrics.TEST_DATA, testCaseDetails));
			}
		}
	}

	private void processAcceptanceTestsData(Context context, Collection<TestSuiteReport> testSuiteReports) {
		for (TestSuiteReport testSuiteReport : testSuiteReports) {
			if (testSuiteReport.isTestSuite()) {

				double tests = testSuiteReport.getTests();
				double errors = testSuiteReport.getErrors();
				double failures = testSuiteReport.getFailures();
				double skipped = testSuiteReport.getSkipped();
				double passed = tests - errors - failures;

				context.resource(context.project());
				context.injectMeasure(AcceptanceMetrics.TESTS, tests);
				context.injectMeasure(AcceptanceMetrics.TEST_ERRORS, errors);
				context.injectMeasure(AcceptanceMetrics.TEST_FAILURES, failures);
				context.injectMeasure(AcceptanceMetrics.SKIPPED_TESTS, skipped);
				context.injectMeasure(AcceptanceMetrics.PASSED_TESTS, passed);
				if (tests > 0) {
					double percentage = ParsingUtils.scaleValue(passed * 100 / tests);
					context.injectMeasure(AcceptanceMetrics.TEST_SUCCESS_DENSITY, percentage);
				}
			}
		}
	}

	private String generateTestsDetails(TestSuiteReport fileReport) {
		StringBuilder testCaseDetails = new StringBuilder();
		testCaseDetails.append("<tests-details>");
		List<TestCaseDetails> details = fileReport.getDetails();

		for (TestCaseDetails detail : details) {
			testCaseDetails.append("<testcase status=\"").append(detail.getStatus()).append("\" time=\"")
			        .append(detail.getTimeMS()).append("\" name=\"").append(detail.getName()).append("\"");

			boolean isError = detail.getStatus().equals(TestCaseDetails.STATUS_ERROR);
			boolean isFailure = detail.getStatus().equals(TestCaseDetails.STATUS_FAILURE);
			if (isError || isFailure) {
				testCaseDetails.append(">").append(isError ? "<error message=\"" : "<failure message=\"")
				        .append(StringEscapeUtils.escapeXml(detail.getErrorMessage())).append("\">")
				        .append("<![CDATA[").append(StringEscapeUtils.escapeXml(detail.getStackTrace())).append("]]>")
				        .append(isError ? "</error>" : "</failure>").append("</testcase>");
			} else {
				testCaseDetails.append("/>");
			}
		}
		testCaseDetails.append("</tests-details>");
		return testCaseDetails.toString();
	}

	/*
	 * hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
	 */
	private Double getTimeAttributeInMS(String stringTime) throws XMLStreamException {
		try {
			Double time = ParsingUtils.parseNumber(stringTime, Locale.ENGLISH);
			return !Double.isNaN(time) ? ParsingUtils.scaleValue(time * 1000, 3) : 0;
		} catch (ParseException e) {
			throw new XMLStreamException(e);
		}
	}
}

package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class AcceptanceMetrics implements Metrics {

	public static final String ACCEPTANCE_TESTING = "ACCEPTANCE_TESTING";

	public static final Metric TESTS = //
	new Metric.Builder("acceptance_tests", "acceptance_tests", Metric.ValueType.INT) //
	        .setDescription("Total number of acceptance tests") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(false) //
	        .create();

	public static final Metric TEST_SUCCESS_DENSITY = //
	new Metric.Builder("acceptance_success_ratio", "acceptance_success_ratio", Metric.ValueType.PERCENT) //
	        .setDescription("Percent of success related to total tests") //
	        .setDirection(Metric.DIRECTION_BETTER) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(true) //
	        .create();

	/**
	 * @deprecated At least, not used in TestsSensor.
	 */
	private static final Metric TEST_DATA = //
	new Metric.Builder("acceptance_testdata", "acceptance_testdata", Metric.ValueType.DATA) //
	        .setDescription("Total number of acceptance tests success") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(false) //
	        .create();

	public static final Metric TEST_FAILURES = //
	new Metric.Builder("acceptance_ko", "acceptance_ko", Metric.ValueType.INT) //
	        .setDescription("Total number of acceptance tests failed") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(true) //
	        .create();

	public static final Metric SKIPPED_TESTS = //
	new Metric.Builder("acceptance_ignores", "acceptance_ignores", Metric.ValueType.INT) //
	        .setDescription("Total number of acceptance tests ignored") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(false) //
	        .create();

	public static final Metric PASSED_TESTS = //
	new Metric.Builder("acceptance_passed", "acceptance_passed", Metric.ValueType.INT) //
	        .setDescription("Total number of acceptance tests passed") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(false) //
	        .create();

	public static final Metric TEST_ERRORS = //
	new Metric.Builder("acceptance_exceptions", "acceptance_exceptions", Metric.ValueType.INT) //
	        .setDescription("Total number of exceptions") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(true) //
	        .create();

	/**
	 * @deprecated At least, not used in TestsSensor.
	 */
	private static final Metric TEST_EXECUTION_TIME = //
	new Metric.Builder("acceptance_execution_time", "acceptance_execution_time", Metric.ValueType.INT) //
	        .setDescription("Total number of exceptions") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain(ACCEPTANCE_TESTING) //
	        .setQualitative(false) //
	        .create();

	@Override
	public List<Metric> getMetrics() {
		return Arrays.asList( //
		        TESTS, //
		        TEST_FAILURES, //
		        SKIPPED_TESTS, //
		        TEST_ERRORS, //
		        TEST_EXECUTION_TIME,//
		        TEST_SUCCESS_DENSITY, //
		        TEST_DATA, //
		        PASSED_TESTS);
	}
}

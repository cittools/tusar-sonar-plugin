package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.ArrayList;
import java.util.List;

class TestSuiteReport {

	private String path;
	private int errors;
	private int skipped;
	private int tests;
	private int timeMS;
	private int failures;
	private boolean isTestSuite;

	private List<TestCaseDetails> details = new ArrayList<TestCaseDetails>();

	public TestSuiteReport(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public int getSkipped() {
		return skipped;
	}

	public void setSkipped(int skipped) {
		this.skipped = skipped;
	}

	public int getTests() {
		return tests;
	}

	public void setTests(int tests) {
		this.tests = tests;
	}

	public int getTimeMS() {
		return timeMS;
	}

	public void setTimeMS(int timeMS) {
		this.timeMS = timeMS;
	}

	public int getFailures() {
		return failures;
	}

	public void setFailures(int failures) {
		this.failures = failures;
	}

	public List<TestCaseDetails> getDetails() {
		return details;
	}

	public void setDetails(List<TestCaseDetails> details) {
		this.details = details;
	}

	public boolean isTestSuite() {
		return isTestSuite;
	}

	public void setTestSuite(boolean isTestSuite) {
		this.isTestSuite = isTestSuite;
	}
}

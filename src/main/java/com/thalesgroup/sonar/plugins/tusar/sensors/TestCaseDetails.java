package com.thalesgroup.sonar.plugins.tusar.sensors;

class TestCaseDetails {

	public final static String STATUS_OK = "ok";
	public final static String STATUS_ERROR = "error";
	public final static String STATUS_FAILURE = "failure";
	public final static String STATUS_SKIPPED = "skipped";

	private String name;
	private String status;
	private String stackTrace;
	private String errorMessage;
	private int timeMS;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getTimeMS() {
		return timeMS;
	}

	public void setTimeMS(int timeMS) {
		this.timeMS = timeMS;
	}
}

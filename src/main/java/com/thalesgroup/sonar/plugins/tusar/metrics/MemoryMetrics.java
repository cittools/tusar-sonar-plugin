package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class MemoryMetrics implements Metrics {

	public static final String MEMORY = "MEMORY";

	public static final Metric MEMORY_ERRORS = //
	new Metric.Builder("memory_errors", "Memory_errors", Metric.ValueType.INT) //
	        .setDescription("The number of errors reported.") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain("MEMORY") //
	        .setQualitative(false) //
	        .create();

	public static final Metric BYTES_LOST = //
	new Metric.Builder("bytes_lost", "Bytes_lost", Metric.ValueType.INT) //
	        .setDescription("The number of bytes lost (memory leaks).") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain("MEMORY") //
	        .setQualitative(false) //
	        .create();

	public static final Metric MEMORY_LEAKS = //
	new Metric.Builder("memory_leaks", "Memory_leaks", Metric.ValueType.INT) //
	        .setDescription("The number of memory leaks.") //
	        .setDirection(Metric.DIRECTION_WORST) //
	        .setDomain("MEMORY") //
	        .setQualitative(false) //
	        .create();

	@Override
	public List<Metric> getMetrics() {
		return Arrays.asList(MEMORY_ERRORS, BYTES_LOST, MEMORY_LEAKS);
	}
}

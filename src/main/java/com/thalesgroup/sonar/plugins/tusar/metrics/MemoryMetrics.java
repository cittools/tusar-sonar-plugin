package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class MemoryMetrics implements Metrics {

 public static final String MEMORY = "MEMORY";

 
  public static final Metric MEMORY_ERRORS = new Metric.Builder("memory_errors", "Memory_errors", Metric.ValueType.INT)
  .setDescription("The number of errors reported.")
  .setDirection(Metric.DIRECTION_WORST)
  .setDomain("MEMORY")
  .setQualitative(false)
  .create();

 
  
  public static final Metric BYTES_LOST = new Metric.Builder("bytes_lost", "Bytes_lost", Metric.ValueType.INT)
  .setDescription("The number of bytes lost (memory leaks).")
  .setDirection(Metric.DIRECTION_WORST)
  .setDomain("MEMORY")
  .setQualitative(false)
  .create();
		  
  public static final Metric MEMORY_LEAKS =	new Metric.Builder("memory_leaks", "Memory_leaks", Metric.ValueType.INT)
  .setDescription("The number of memory leaks.")
  .setDirection(Metric.DIRECTION_WORST)
  .setDomain("MEMORY")
  .setQualitative(false)
  .create();
  
 
 /*

 public static final Metric MEMORY_ERRORS = new Metric("memory_errors", "Number of memory errors",
     "The number of memory errors", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, MemoryMetrics.MEMORY);

 public static final Metric BYTES_LOST = new Metric("bytes_lost", "Number of bytes lost",
     "The number of bytes lost (memory leaks)", Metric.ValueType.INT, Metric.DIRECTION_WORST, false,
     MemoryMetrics.MEMORY);

 public static final Metric MEMORY_LEAKS = new Metric("memory_leaks", "Number of memory leaks",
     "The number of memory leaks ", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, MemoryMetrics.MEMORY);
		  
*/
 
  // getMetrics() method is defined in the Metrics interface and is used by
  // Sonar to retrieve the list of new Metric
  public List<Metric> getMetrics() {
    return Arrays.asList(MEMORY_ERRORS, BYTES_LOST, MEMORY_LEAKS);
  }
  public static Metric getMetric(String key){
	  Metric value=null;
	  if(key.equalsIgnoreCase("MEMORY_ERRORS")){
		  value=MemoryMetrics.MEMORY_ERRORS;
	  }else if(key.equalsIgnoreCase("BYTES_LOST")){
		  value=MemoryMetrics.BYTES_LOST;
	  } else if(key.equalsIgnoreCase("MEMORY_LEAKS")){
		  value=MemoryMetrics.MEMORY_LEAKS;
	  }
	  return value;
  }
}

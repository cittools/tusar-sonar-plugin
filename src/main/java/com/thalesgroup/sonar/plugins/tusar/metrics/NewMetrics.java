/*******************************************************************************
 * Copyright (c) 2011 Thales Global Services SAS                                *
 *                                                                              *
 * Author : Aravindan Mahendran                                                 *
 *                                                                              *
 * The MIT license                                                              *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/


package com.thalesgroup.sonar.plugins.tusar.metrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.SumChildValuesFormula;
import org.sonar.api.measures.Metric.Builder;

import com.thalesgroup.sonar.plugins.tusar.utils.Constants;
import com.thalesgroup.sonar.plugins.tusar.utils.Utils;

public class NewMetrics implements Metrics {

	private static String csvFilePath;
	private static final List<Metric> metrics = new ArrayList<Metric>();

	public NewMetrics(Configuration configuration){
		csvFilePath = configuration.getString(Constants.TUSAR_INI_FILE_PATH_KEY);
	}

	public static void addNewMetric(Metric metric){
		metrics.add(metric);
	}

	public static String getCSVFilePath(){
		return csvFilePath;
	}

	// getMetrics() method is defined in the Metrics interface and is used by
	// Sonar to retrieve the list of new Metric
	public List<Metric> getMetrics(){
		addNewMetrics();
		return metrics;
	}

	public static Metric get(int index){
		return metrics.get(index);
	}

	public static Metric contains(String metric){
		for (Metric m : metrics){
			if (m.getKey().toUpperCase().equals(metric.toUpperCase())){
				return m;
			}
		}
		return null;
	}

	/**
	 * Read the embedded CSV file (or defined by the property) line and add the new metrics which are defined in it.
	 */
	private void addNewMetrics(){
		ClassLoader classLoader = getClass().getClassLoader();

		File configFile = NewMetrics.getCSVFilePath()==null ? null:new File(NewMetrics.getCSVFilePath());
		if (configFile!=null && configFile.exists()){
			List<String[]> csvData = null;
			try {
				csvData = Utils.parseIniFile(configFile);
			} catch (FileNotFoundException e) {
				throw new NullPointerException(e.getMessage());
			}

			for (String[] metricsData : csvData){
				Utils.getLogger().info(metricsData[Constants.METRIC_NAME] + " " + metricsData[Constants.METRIC_TYPE] + " " + metricsData[Constants.METRIC_DOMAIN]);
				NewMetrics.addNewMetric(new Builder(Utils.convertToKeyNorm(metricsData[Constants.METRIC_NAME]),metricsData[Constants.METRIC_NAME],Metric.ValueType.valueOf(metricsData[Constants.METRIC_TYPE].toUpperCase())).setDirection(0).setQualitative(false).setDomain(metricsData[Constants.METRIC_DOMAIN]).create().setFormula(new SumChildValuesFormula(false)));
			}
		}
		else {
			List<String[]> csvData = null;
			try {
				csvData = Utils.parseIniInputStream(classLoader.getResourceAsStream(Constants.DEFAULT_METRICS_CSV));
			} catch (FileNotFoundException e) {
				throw new NullPointerException(e.getMessage());
			}

			for (String[] metricsData : csvData){
				Utils.getLogger().info(metricsData[Constants.METRIC_NAME] + " " + metricsData[Constants.METRIC_TYPE] + " " + metricsData[Constants.METRIC_DOMAIN]);
				NewMetrics.addNewMetric(new Builder(Utils.convertToKeyNorm(metricsData[Constants.METRIC_NAME]),metricsData[Constants.METRIC_NAME],Metric.ValueType.valueOf(metricsData[Constants.METRIC_TYPE].toUpperCase())).setDirection(0).setQualitative(false).setDomain(metricsData[Constants.METRIC_DOMAIN]).create().setFormula(new SumChildValuesFormula(false)));
			}
		}

	}

}

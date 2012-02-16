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

package com.thalesgroup.sonar.plugins.tusar.newmeasures;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thalesgroup.sonar.lib.model.v4.SizeComplexType.Resource.Measure;
import com.thalesgroup.sonar.plugins.tusar.metrics.NewMetrics;
import com.thalesgroup.sonar.plugins.tusar.utils.Utils;

/**
 * Class that extracts and handles measures (which don't deal with sonar metrics) in TUSAR files.
 */
public class NewMeasuresExtractor {

	public static Map<Metric.ValueType, GenericSaveMeasure> genericSaveMeasure = new HashMap<Metric.ValueType, GenericSaveMeasure>();

	static {
		genericSaveMeasure.put(Metric.ValueType.FLOAT, GenericSaveMeasureFactory.saveMeasureDouble());
		genericSaveMeasure.put(Metric.ValueType.STRING, GenericSaveMeasureFactory.saveMeasureString());
		genericSaveMeasure.put(Metric.ValueType.LEVEL, GenericSaveMeasureFactory.saveMeasureLevel());
		genericSaveMeasure.put(Metric.ValueType.PERCENT, GenericSaveMeasureFactory.saveMeasureDouble());
		genericSaveMeasure.put(Metric.ValueType.INT, GenericSaveMeasureFactory.saveMeasureDouble());
		genericSaveMeasure.put(Metric.ValueType.MILLISEC, GenericSaveMeasureFactory.saveMeasureDouble());
		genericSaveMeasure.put(Metric.ValueType.RATING, GenericSaveMeasureFactory.saveMeasureDouble());

	}

	
	/**
	 * Save the measures present in the given measure element
	 * @param project The sonar project
	 * @param sensorContext The sensor context
	 * @param measure The measure tag currently treated
	 * @param resourceType The resource type ("FILE", "DIRECTORY", "PROJECT")
	 * @param fileName The name of the file/directory/project (if project, not taken into account)
	 */
	public static void treatMeasure(Project project, SensorContext sensorContext, Measure measure, String resourceType, String fileName){
		String type = measure.getKey();
		String value = measure.getValue();
		Metric metric = NewMetrics.contains(type);

		String resourceTypeUpperCase = resourceType.toUpperCase();

		//The contains method of NewMetrics could be less time greedy by using one or two sets : one for unknown metrics and one for known metrics
		if (metric != null){
			//TODO : Has to be more generic...
			if ("FILE".equals(resourceTypeUpperCase)){
				if (!saveMeasures(project,sensorContext, project, metric, value, fileName)){
					Resource resource = new org.sonar.api.resources.File(fileName);
					System.out.println("Absolute file non existing key");
					sensorContext.index(resource);
					//Saving source if file exists
					File source = new File(fileName);
					if (source != null && source.exists()){
						Utils.saveSource(sensorContext, source, resource, project.getFileSystem().getSourceCharset());
					}
					NewMeasuresExtractor.genericSaveMeasure.get(metric.getType()).saveMeasure(sensorContext, resource, metric, value);
				}
			}
			else if ("DIRECTORY".equals(resourceTypeUpperCase)){
				if (!saveMeasures(project,sensorContext, project, metric, value, fileName)){
					Resource resource = new Directory(fileName);
					System.out.println("Absolute dir non existing key");
					sensorContext.index(resource);
					NewMeasuresExtractor.genericSaveMeasure.get(metric.getType()).saveMeasure(sensorContext, resource, metric, value);
				}
			}
			else if ("PROJECT".equals(resourceTypeUpperCase)){
				NewMeasuresExtractor.genericSaveMeasure.get(metric.getType()).saveMeasure(sensorContext, project, metric, value);
				System.out.println(metric+" "+value);
			}
		}
	}

	/**
	 * Function that saves a measure (metric+value) into Sonar.
	 * @param project The sonar project
	 * @param sensorContext The sensor context
	 * @param resource The resource to be saved
	 * @param metric The metric kind which will be saved
	 * @param metricValue The value of the metric
	 * @param fileName The name of the file/directory/project (if project, not taken into account)
	 * @return true if a measure is saved, else false
	 */
	private static boolean saveMeasures(Project project, SensorContext sensorContext, Resource resource, Metric metric, String metricValue, String fileName){
		boolean result = false;
		for (Resource r : sensorContext.getChildren(resource)){
			result = saveMeasures(project, sensorContext, r, metric, metricValue, fileName);
			if(!result){
				if (r.getKey().equals(fileName)){
					NewMeasuresExtractor.genericSaveMeasure.get(metric.getType()).saveMeasure(sensorContext, r, metric, metricValue);
					return true;
				}
				else {
					String relativePath = DefaultProjectFileSystem.getRelativePath(new File(fileName), project.getFileSystem().getSourceDirs());
					if(relativePath!=null){
						if (r.getKey().equals(relativePath)){
							NewMeasuresExtractor.genericSaveMeasure.get(metric.getType()).saveMeasure(sensorContext, r, metric, metricValue);
							return true;
						}
					}
				}
			}
			else {
				return result;
			}
		}
		return result;
	}

}
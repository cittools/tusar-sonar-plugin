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

import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

/**
 * Factory which creates GenericSaveMeasure
 */
public abstract class GenericSaveMeasureFactory {

	public static GenericSaveMeasure saveMeasureDouble(){
		return new GenericSaveMeasure() {

			@Override
			public void saveMeasure(SensorContext sensorContext, Resource resource, Metric metric, String value) {
				try{
					Measure measure = new Measure(metric, Double.parseDouble(value));
					sensorContext.saveMeasure(resource, measure);
				}catch (SonarException e) {
					e.printStackTrace();
				}
			}
		};
	}

	public static GenericSaveMeasure saveMeasureString(){
		return new GenericSaveMeasure() {

			@Override
			public void saveMeasure(SensorContext sensorContext, Resource resource,
					Metric metric, String value) {
				Measure measure = new Measure(metric, value);
				sensorContext.saveMeasure(resource, measure);
			}
		};
	}

	public static GenericSaveMeasure saveMeasureLevel(){
		return new GenericSaveMeasure() {

			@Override
			public void saveMeasure(SensorContext sensorContext, Resource resource,
					Metric metric, String value) {
				try{
					Measure measure = new Measure(metric, Level.valueOf(value));
					sensorContext.saveMeasure(resource, measure);
				}catch (SonarException e) {
					e.printStackTrace();
				}
			}
		};
	}
}

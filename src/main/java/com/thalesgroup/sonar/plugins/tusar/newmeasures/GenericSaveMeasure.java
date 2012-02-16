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
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;

/**
 * This interface is used to have generic saveMeasure function (function available in sensorContext).
 * If you want to handle a new kind of sensorContext.saveMeasure, you have to :
 * - Add a new function in GenericSaveMeasure.java
 * - Fill the map in NewMeasuresExtractor.java
 * 
 * NOTE : Comment to be updated if modifications are done on above operations...
 */
public interface GenericSaveMeasure {
	public void saveMeasure(SensorContext sensorContext, Resource resource, Metric metric, String value);
}

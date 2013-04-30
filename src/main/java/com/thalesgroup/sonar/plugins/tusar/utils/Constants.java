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

package com.thalesgroup.sonar.plugins.tusar.utils;

public class Constants {

	//CSV file properties
	public static final int TOKENS_LENGTH = 3;
	public static final String COMMENTS = "#";
	public static final String CSV_SEPARATOR = ";";

	//Columns of CSV files
	public static final int METRIC_NAME = 0;
	public static final int METRIC_TYPE = 1;
	public static final int METRIC_DOMAIN = 2;

	//CSV file
	public static final String DEFAULT_METRICS_CSV = "com/thalesgroup/sonar/plugins/tusar/newmetrics.csv";
	public static final String DEFAULT_EXTENSIONS_FILE = "com/thalesgroup/sonar/plugins/tusar/file_extension";
	public static final String CSV_EXTENSION = ".csv";
	
	//XML
	public static final String XML_EXTENSION = ".xml";
	
	//Plugin properties
	public static final String USER_MEASURES_REPORTS_PATHS_KEY = "sonar.tusar.reportsPaths";
	public static final String TUSAR_INI_FILE_PATH_KEY = "sonar.tusar.iniFilePaths";
	public static final String TUSAR_EXTENSIONS_FILE_PATH_KEY = "sonar.tusar.extFilePaths";

}

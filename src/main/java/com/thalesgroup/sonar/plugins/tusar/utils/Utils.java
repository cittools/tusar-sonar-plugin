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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

public class Utils {

	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	/**
	 * Parse a csv file containing user metrics information (name, type, domain)
	 * @param csvFile The path of the initialisationFile
	 * @return a list of String[] which contains the name of the metrics, its type and its domain
	 * @throws FileNotFoundException
	 */
	public static List<String[]> parseFile(File csvFile) throws FileNotFoundException{
		List<String[]> metrics = new ArrayList<String[]>();
		if (!csvFile.getName().endsWith(Constants.CSV_EXTENSION)){
			logger.warn(csvFile + " is not a CSVFile");
			return metrics;
		}
		Scanner scanner = new Scanner(csvFile);
		scanContent(scanner, metrics);
		return metrics;
	}
	
	public static List<String[]> parseInputStream(InputStream csvFile) throws FileNotFoundException{
		List<String[]> metrics = new ArrayList<String[]>();
		Scanner scanner = new Scanner(csvFile);
		scanContent(scanner, metrics);
		return metrics;
	}
	
	
	
	private static void scanContent(Scanner scanner, List<String[]> metrics){
		while(scanner.hasNextLine()){
			String line = scanner.nextLine();
			if (!line.startsWith(Constants.COMMENTS)){
				String[] tokens = line.split(Constants.CSV_SEPARATOR);
				if (tokens.length>=Constants.TOKENS_LENGTH){
					metrics.add(tokens);
				}
				else {
					logger.warn(line +" contains less than three elements");
				}
			}
		}
	}
	
	
	
	/**
	 * Save the source of an existing file. The given parameters must not be null.
	 * @param sensorContext The actually used sensor context
	 * @param fileToRead The file to be read
	 * @param resourceToSave The resource associated to the file
	 * @param sourcesEncoding The source encoding of the file
	 */
	public static void saveSource(SensorContext sensorContext, File fileToRead, Resource resourceToSave, Charset sourcesEncoding){
		try {
			String source = FileUtils.readFileToString(fileToRead, sourcesEncoding.name());
			sensorContext.saveSource(resourceToSave, source);
		} catch (IOException e) {
			throw new SonarException("Unable to read and import the source file : '" + fileToRead.getAbsolutePath() + "' with the charset : '"
					+ sourcesEncoding.name() + "'. You should check the property " + CoreProperties.ENCODING_PROPERTY, e);
		}
	}
	
	public static Logger getLogger() {
		return logger;
	}

}

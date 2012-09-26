/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
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

package com.thalesgroup.sonar.plugins.tusar.sensors;

import com.thalesgroup.sonar.plugins.tusar.TUSARLanguage;
import com.thalesgroup.sonar.plugins.tusar.TUSARPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.XmlParserException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This Sensor uses the TUSAR format parser. It handles the analysis of TUSAR xml reports.
 */
public class TUSARSensor implements Sensor {

    private final String reportPathKey = TUSARPlugin.TUSAR_REPORTSPATHS_KEY;
    private final String reportPathDefault = "reportsTUSAR";
    private static Logger logger = LoggerFactory.getLogger(TUSARSensor.class);

    private TUSARViolationsDataExtractor dataExtractor;

    public TUSARSensor(TUSARViolationsDataExtractor dataExtractor) {
        this.dataExtractor = dataExtractor;
    }

    public void analyse(Project project, SensorContext context) {
        List<File> reportsDirs = getReportsDirs(project, getReportPathKey(), getReportPathDefault());
        if (reportsDirs.size() == 0) {
            logger.warn("No reports directory found. Abandoning...");
            return;
        }

        List<File> xmlReports = getXmlReports(reportsDirs);

        TUSARXmlParser parser = new TUSARXmlParser(dataExtractor);
        for (File xmlReport : xmlReports) {
            logger.info("parsing {}", xmlReport);
            try {
                parser.parse(xmlReport, context, project);
            } catch (XMLStreamException e) {
                throw new XmlParserException(e);
            }
        }
        
        //Set the coverage of files without coverage metrics
        if (TUSARCoverageDataExtractor.isLineCoverageInTusar()){
        	setLineCoverageToZero(project, context);
        }
    }

    public boolean shouldExecuteOnProject(Project project) {
        return TUSARLanguage.INSTANCE.equals(project.getLanguage());
    }

    protected List<File> getReportsDirs(Project project, String key, String defaultPath) {
        List<File> reportsDirs = getReportsDirsFromProperty(project, key);
        if (reportsDirs.size() == 0) {
            reportsDirs = getReportsDirFromDefaultPath(project, defaultPath);
        }
        return reportsDirs;
    }

    protected List<File> getReportsDirsFromProperty(Project project, String key) {

        List<File> reportsDirs = new ArrayList<File>();
        String concatenatedReportsPaths = (String) project.getProperty(key);
        if (concatenatedReportsPaths != null) {
            String[] reportsPaths = concatenatedReportsPaths.split(";");
            for (String reportsPath : reportsPaths) {
                File reportDir = project.getFileSystem().resolvePath(reportsPath);
                if (reportDir.exists() && reportDir.isDirectory()) {
                    reportsDirs.add(reportDir);
                } else {
                    logger.warn("Reports directory {} not found.", reportDir.getAbsolutePath());
                }
            }
        }
        return reportsDirs;
    }

    protected List<File> getReportsDirFromDefaultPath(Project project, String defaultPath) {
        List<File> reportsDirs = new ArrayList<File>();
        File reportDir = new File(project.getFileSystem().getBasedir(), defaultPath);
        if (reportDir.exists() && reportDir.isDirectory()) {
            reportsDirs.add(reportDir);
        }
        return reportsDirs;
    }

    protected List<File> getXmlReports(List<File> reportsDirs) {
        List<File> xmlReports = new ArrayList<File>();
        for (File reportDir : reportsDirs) {
            File[] dirList = reportDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });
            xmlReports.addAll(Arrays.asList(dirList));
        }

        return xmlReports;
    }

    protected String getReportPathKey() {
        return reportPathKey;
    }

    protected String getReportPathDefault() {
        return reportPathDefault;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    
    private void setLineCoverageToZero(Resource resource, SensorContext context){
    	if (resource == null){
			return;
		}
    	for (Resource children : context.getChildren(resource)){
    		setLineCoverageToZero(children, context);
    		if (children.getScope()==Qualifiers.FILE ){
    			
    			Measure linesToCover = context.getMeasure(children, CoreMetrics.LINES_TO_COVER);
    			Measure lineCoverage = context.getMeasure(children, CoreMetrics.LINE_COVERAGE);
    			Measure uncoveredLines = context.getMeasure(children, CoreMetrics.UNCOVERED_LINES);
    			Measure statements = context.getMeasure(children, CoreMetrics.STATEMENTS);

    			
    			if (lineCoverage == null && linesToCover == null && uncoveredLines == null && statements != null){
    				double statementsValue = statements.getValue();
    				context.saveMeasure(children, CoreMetrics.LINE_COVERAGE, 0.0);
    				context.saveMeasure(children, CoreMetrics.LINES_TO_COVER, statementsValue);
    				context.saveMeasure(children, CoreMetrics.UNCOVERED_LINES, statementsValue);
    			}
    		}
    	}
    }
}

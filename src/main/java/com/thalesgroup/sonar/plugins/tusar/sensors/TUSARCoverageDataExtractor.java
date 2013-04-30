/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 *                                                                              *
 * The MIT License                                                              *
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

import java.text.ParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;

import com.thalesgroup.sonar.lib.model.v5.BranchCoverageComplexType;
import com.thalesgroup.sonar.lib.model.v5.LineCoverageComplexType;
import com.thalesgroup.sonar.lib.model.v5.Sonar;
import com.thalesgroup.sonar.plugins.tusar.TUSARResource;

/**
 * Contains methods to extract TUSAR tests data.
 */
public class TUSARCoverageDataExtractor {

    private static Logger logger = LoggerFactory.getLogger(TUSARCoverageDataExtractor.class);
    private static String FILE_RESSOURCE_TYPE="File";
    
    private static boolean lineCoverageInTusar = false;

    public static boolean isLineCoverageInTusar() {
		return lineCoverageInTusar;
	}

	public static void saveToSonarCoverageData(Sonar model, SensorContext context, Project project) throws ParseException {

        processLineCoverage(model, context, project);
        processBranchCoverage(model, context, project);
    }
    
    private static void processBranchCoverage(Sonar model, SensorContext context, Project project) throws ParseException {
    	BranchCoverageComplexType branchCoverage = model.getCoverage().getBranchCoverage();
    	if (branchCoverage != null){
			for (BranchCoverageComplexType.Resource resource : branchCoverage.getResource()){
				if (resource.getType().equalsIgnoreCase(FILE_RESSOURCE_TYPE)){
					
					Resource res = TUSARResource.fromAbsOrRelativePath(resource.getFullname(), project, false);
					
					
					if (res == null) {
		                logger.debug("Path is not valid, resource {} does not exists.", resource.getFullname());
		            }
					else {
						int conditionsToCover = 0;
						int uncoveredConditions = 0;
						PropertiesBuilder<String, Integer> conditionsByLine = new PropertiesBuilder<String, Integer>(CoreMetrics.CONDITIONS_BY_LINE);
						PropertiesBuilder<String, Integer> coveredConditionsByLine = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
						
						for (BranchCoverageComplexType.Resource.Line line : resource.getLine()){
							int numberOfBranches = (int) ParsingUtils.parseNumber(line.getNumberOfBranches(), Locale.ENGLISH);
							int uncoveredBranches = (int) ParsingUtils.parseNumber(line.getUncoveredBranches(), Locale.ENGLISH);
							
							conditionsToCover += numberOfBranches;
							uncoveredConditions += uncoveredBranches;
							
							conditionsByLine.add(line.getNumber(), numberOfBranches);
							coveredConditionsByLine.add(line.getNumber(), numberOfBranches-uncoveredBranches);
							
						}
						
						context.saveMeasure(res, new Measure(CoreMetrics.CONDITIONS_TO_COVER,(double)conditionsToCover));
						context.saveMeasure(res, new Measure(CoreMetrics.UNCOVERED_CONDITIONS,(double)uncoveredConditions));
						context.saveMeasure(res, new Measure(CoreMetrics.BRANCH_COVERAGE,(double)calculateCoverage(conditionsToCover-uncoveredConditions, conditionsToCover)));
						context.saveMeasure(res,conditionsByLine.build().setPersistenceMode(PersistenceMode.DATABASE));
						context.saveMeasure(res,coveredConditionsByLine.build().setPersistenceMode(PersistenceMode.DATABASE));
					}
				}
			}
    	}
	}

	private static void processLineCoverage(Sonar model, SensorContext context, Project project) throws ParseException{
		LineCoverageComplexType lineCoverage = model.getCoverage().getLineCoverage();
		 logger.debug("processLineCoverag "+ model.getCoverage().getLineCoverage().toString());
		if (lineCoverage != null){
	    	for (LineCoverageComplexType.File file : model.getCoverage().getLineCoverage().getFile()) {
	    		
	    		lineCoverageInTusar = true;
	    		
	            Resource resource = TUSARResource.fromAbsOrRelativePath(file.getPath(), project, false);
	
	            if (resource == null) {
	                logger.debug("Path is not valid, resource {} does not exists.", file.getPath());
	            } else {
	                double lines = 0;
	                double coveredLines = 0;
	                PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);
	                //double coveredConditions;
	                //double conditions;
	
	                for (LineCoverageComplexType.File.Line line : file.getLine()) {
	
	                    lines++;
	                    int hits = (int) ParsingUtils.parseNumber(line.getHits(), Locale.ENGLISH);
	                    if (hits > 0) {
	                        coveredLines++;
	                    }
	                    lineHitsBuilder.add(line.getNumber(), hits);
	                }
	
	                //double coverage = calculateCoverage(coveredLines + coveredConditions, lines + conditions);
	                //context.saveMeasure(resource, new Measure(CoreMetrics.COVERAGE, coverage));
	                logger.debug("saving. LINES_TO_COVER", lines);
	                context.saveMeasure(resource, new Measure(CoreMetrics.LINES_TO_COVER, (double) lines));
	                logger.debug("saving. LINE_COVERAGE", calculateCoverage(coveredLines, lines));
	                context.saveMeasure(resource, new Measure(CoreMetrics.LINE_COVERAGE, calculateCoverage(coveredLines, lines)));
	                context.saveMeasure(resource, new Measure(CoreMetrics.UNCOVERED_LINES, (double) lines - coveredLines));
	                context.saveMeasure(resource, lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
	            }
	        }
		}
    }

    private static double calculateCoverage(double coveredElements, double elements) {
        if (elements > 0) {
            return ParsingUtils.scaleValue(100.0 * ((double) coveredElements / (double) elements));
        }
        return 0.0;
    }
    
}

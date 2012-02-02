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

import com.thalesgroup.sonar.lib.model.v3.CoverageComplexType;
import com.thalesgroup.sonar.lib.model.v3.Sonar;
import com.thalesgroup.sonar.plugins.tusar.TUSARResource;
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

import java.text.ParseException;
import java.util.Locale;

/**
 * Contains methods to extract TUSAR tests data.
 */
public class TUSARCoverageDataExtractor {

    private static Logger logger = LoggerFactory.getLogger(TUSARCoverageDataExtractor.class);

    public static void saveToSonarCoverageData(Sonar model, SensorContext context, Project project) throws ParseException {

        for (CoverageComplexType.File file : model.getCoverage().getFile()) {

            Resource resource = TUSARResource.fromAbsOrRelativePath(file.getPath(), project, false);

            if (resource == null) {
                logger.debug("Path is not valid, resource {} does not exists.", file.getPath());
            } else {
                double lines = 0;
                double coveredLines = 0;
                PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);
                //double coveredConditions;
                //double conditions;

                for (CoverageComplexType.File.Line line : file.getLine()) {

                    lines++;
                    int hits = (int) ParsingUtils.parseNumber(line.getHits(), Locale.ENGLISH);
                    if (hits > 0) {
                        coveredLines++;
                    }
                    lineHitsBuilder.add(line.getNumber(), hits);
                }

                //double coverage = calculateCoverage(coveredLines + coveredConditions, lines + conditions);
                //context.saveMeasure(resource, new Measure(CoreMetrics.COVERAGE, coverage));

                context.saveMeasure(resource, new Measure(CoreMetrics.LINES_TO_COVER, (double) lines));
                context.saveMeasure(resource, new Measure(CoreMetrics.LINE_COVERAGE, calculateCoverage(coveredLines, lines)));
                context.saveMeasure(resource, new Measure(CoreMetrics.UNCOVERED_LINES, (double) lines - coveredLines));
                context.saveMeasure(resource, lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
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

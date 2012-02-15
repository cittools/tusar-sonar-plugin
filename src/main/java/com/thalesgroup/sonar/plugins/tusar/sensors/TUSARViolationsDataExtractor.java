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

import com.thalesgroup.sonar.lib.model.v4.Sonar;
import com.thalesgroup.sonar.lib.model.v4.ViolationsComplexType;
import com.thalesgroup.sonar.plugins.tusar.TUSARResource;
import com.thalesgroup.sonar.plugins.tusar.rulesrepository.TUSARRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.ParsingUtils;

import java.text.ParseException;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Contains methods to extract TUSAR tests data.
 */
public class TUSARViolationsDataExtractor implements BatchExtension {

    private static Logger logger = LoggerFactory.getLogger(TUSARViolationsDataExtractor.class);

    private RuleFinder ruleFinder;

    public TUSARViolationsDataExtractor(RuleFinder ruleFinder) {
        this.ruleFinder = ruleFinder;
    }

    public void saveToSonarViolationsData(Sonar model, SensorContext context, Project project)
            throws ParseException {

        for (ViolationsComplexType.File file : model.getViolations().getFile()) {

            Resource resource = TUSARResource.fromAbsOrRelativePath(
                    file.getPath(), project, false);

            // File resourceFile = File.fromIOFile(new
            // java.io.File(file.getName()), project);

            if (resource == null) {
                logger.debug("Path is not valid, resource {} does not exists.",
                        file.getPath());
            } else {

                for (ViolationsComplexType.File.Violation error : file
                        .getViolation()) {

                    RuleQuery ruleQuery = RuleQuery
                            .create()
                            .withRepositoryKey(TUSARRuleRepository.REPOSITORY_KEY)
                            .withKey(error.getKey());
                    Rule rule = ruleFinder.find(ruleQuery);
                    if (rule != null) {
                        int line = parseLineIndex(error.getLine());
                        Violation violation = new Violation(rule, resource)
                                .setLineId(line).setMessage(error.getMessage());

                        context.saveViolation(violation);
                    }

                    /*
                          * Rule rule = rulesManager.getPluginRule(TUSARPlugin.KEY,
                          * error.getKey()); if (rule != null) { int line =
                          * parseLineIndex(error.getLine()); Violation violation =
                          * new Violation(rule, resource) .setLineId(line)
                          * .setMessage(error.getMessage());
                          *
                          * context.saveViolation(violation); }
                          */
                }
            }
        }
    }

    private Integer parseLineIndex(String line) throws ParseException {
        if (!isNotBlank(line) || line.indexOf('-') != -1) {
            return null;
        }
        return (int) ParsingUtils.parseNumber(line);
    }
}

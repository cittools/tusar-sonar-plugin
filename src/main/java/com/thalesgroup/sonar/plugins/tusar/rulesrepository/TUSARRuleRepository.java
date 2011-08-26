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

package com.thalesgroup.sonar.plugins.tusar.rulesrepository;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import com.thalesgroup.sonar.plugins.tusar.TUSARLanguage;


/** 
 * This repository stores all the coding rules used by the Violations sensor
 * It uses the rules.xml file provided in the dedicated jar (sonar-tusarplugin-rules)
 */
public class TUSARRuleRepository extends RuleRepository {	

	  public static final String REPOSITORY_KEY = "tusar";
	  public static final String REPOSITORY_NAME = "Tusar rules";

	  // for user extensions
	  private ServerFileSystem fileSystem;
	  private XMLRuleParser parser;

	  public TUSARRuleRepository(ServerFileSystem fileSystem, XMLRuleParser parser) {
	    super(REPOSITORY_KEY, TUSARLanguage.KEY);
	    setName(REPOSITORY_NAME);
	    this.fileSystem = fileSystem;
	    this.parser = parser;
	  }

	  /**
	   * @see org.sonar.api.rules.RuleRepository#createRules()
	   */
	  @Override
	  public List<Rule> createRules() {
	    List<Rule> rules = new ArrayList<Rule>();
	    InputStream inputStream = getClass().getResourceAsStream("/com/thalesgroup/sonar/plugins/tusar/default-rules.xml");
	    List<Rule> parsedRules = parser.parse(inputStream);
	    rules.addAll(parsedRules);
	    // Gives the ability for the user to extends system rules withs its own rules.
	    // user rules must be set in SONAR_HOME/extensions/rules/REPOSITORY_KEY/*.xml
	    for (File userExtensionXml : fileSystem.getExtensions(REPOSITORY_KEY, "xml")) {
	      List<Rule> userRules = parser.parse(userExtensionXml);
	      rules.addAll(userRules);
	    }
	    return rules;
	  }
}

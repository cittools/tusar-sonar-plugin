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

package com.thalesgroup.sonar.plugins.tusar;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.Extension;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;

import com.thalesgroup.sonar.plugins.tusar.metrics.NewMetrics;
import com.thalesgroup.sonar.plugins.tusar.rulesrepository.TUSARRuleRepository;
import com.thalesgroup.sonar.plugins.tusar.sensors.TUSARSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TUSARViolationsDataExtractor;
import com.thalesgroup.sonar.plugins.tusar.utils.Constants;

/**
 * This class is the container for all others extensions
 */
@Properties({@Property(key = TUSARPlugin.TUSAR_REPORTSPATHS_KEY, name = "TUSAR reports path", description = "Path (absolute or relative) to TUSAR xml reports directories. Separate paths by ';'", project = true, global = false),
		@Property(key=Constants.TUSAR_INI_FILE_PATH_KEY, name="Initialisation file path", project=true)})
public class TUSARPlugin implements Plugin {

    public static final String KEY = "tusarplugin";
    public static final String NAME = "TUSAR Format Plugin";
    public static final String DESCRIPTION = "This is a generic plugin allowing Sonar to display data coming from any tool. This uses the 'light-mode' only. Unit tests, coverage, violations and others metrics come to Sonar through the TUSAR reports format.";

    public static final String FILTERS_DEFAULT_VALUE = "xml";
    public static final String FILTERS_KEY = "sonar.tusar.filters";

    public static final String TUSAR_REPORTSPATHS_KEY = "sonar.tusar.reportsPaths";

    // The key which uniquely identifies your plugin among all others Sonar
    // plugins
    public String getKey() {
        return KEY;
    }

    public String getName() {
        return NAME;
    }

    // This description will be displayed in the Configuration > Settings web
    // page
    public String getDescription() {
        return DESCRIPTION;
    }

    // This is where you're going to declare all your Sonar extensions
    public List<Class<? extends Extension>> getExtensions() {
        List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();

        // Declare the language used by the plugin
        list.add(TUSARLanguage.class);

        // Declare the source importer module
        list.add(SourceImporter.class);

        list.add(TUSARViolationsDataExtractor.class);

        list.add(TUSARRuleRepository.class);
        //list.add(TUSARProfileExporter.class);
        //list.add(TUSARProfileImporter.class);

        // Declare the sensors : these classes are executed if their method
        // "shouldExecuteOnProject" returns true.
        list.add(TUSARSensor.class);
        
        //Getting the new metrics
        list.add(NewMetrics.class);

        // Declare the decorators. These are executed after the sensors.
        // The decorators uses raw data from sensor, and calculate other
        // metrics.
        // ie : Number of lines of code for the whole project is an addition of
        // lines of code of each file.
        // GBO - REMOVE unuse Directory
        /*
           * list.add(DirectoryDecorator.class); list.add(FilesDecorator.class);
           */

        // Declare the coverage viewer. This is a gwt page adding a new tab for
        // coverage.
        // We can not use the standard viewer, because this one displays only
        // resources with qualifier QUALIFIER_CLASS.

        return list;
    }

    @Override
    public String toString() {
        return getKey();
    }
}

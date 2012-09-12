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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.resources.AbstractLanguage;

import com.thalesgroup.sonar.plugins.tusar.utils.Constants;
import com.thalesgroup.sonar.plugins.tusar.utils.Utils;

/**
 * This class describes the language used by the plugin.
 */
public class TUSARLanguage extends AbstractLanguage {

    public static final TUSARLanguage INSTANCE = new TUSARLanguage();

    public static final String KEY = "tusar";
    public static final String NAME = "TUSAR format - All languages";
    
    //AMA : adding a new attribute to store the path of the file containing the managed extensions path.
    private String extFilePath;

    public TUSARLanguage(Configuration configuration){
    	this();
    	extFilePath = configuration.getString(Constants.TUSAR_EXTENSIONS_FILE_PATH_KEY);
	}
    
    public TUSARLanguage() {
        super(KEY, NAME);
    }

    @Override
    public String[] getFileSuffixes() {
    	String[] suffixes = new String[0];
    	ClassLoader classLoader = getClass().getClassLoader();
    	
    	File extensionsFile = extFilePath==null ? null:new File(extFilePath);
    	Scanner scanner = null;
		if (extensionsFile!=null && extensionsFile.exists()){
			try {
				scanner = new Scanner(extensionsFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			scanner = new Scanner(classLoader.getResourceAsStream(Constants.DEFAULT_EXTENSIONS_FILE));
		}
        return Utils.getExtensions(scanner);
    }
    
    
}

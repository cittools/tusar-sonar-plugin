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
import java.util.List;

import org.sonar.api.batch.AbstractSourceImporter;
import org.sonar.api.resources.Resource;

/**dd
 * The sourceImporter only extends AbstractSourceImporter. 
 * It parses the source directories and imports source and tests files described in TUSARLanguage class.
 * The source will then be available in sonar dashboard in the tab "Sources" and in other tabs that uses the source (ie : violations, coverage...)
 * Deactivate the source importing with "sonar.importSources=false" property.
 */
public class SourceImporter extends AbstractSourceImporter {
	
	public SourceImporter() {
		super(TUSARLanguage.INSTANCE);
	}
	
	@Override
	protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
		TUSARResource resource = TUSARResource.fromIOFile(file, sourceDirs, unitTest);
	    if (resource != null) {
	      resource.setLanguage(TUSARLanguage.INSTANCE);
	    }
	    return resource;
	  }
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}

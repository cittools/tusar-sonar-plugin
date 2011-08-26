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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.xml.sax.SAXException;

import com.thalesgroup.dtkit.util.converter.ConversionServiceFactory;
import com.thalesgroup.sonar.lib.model.v2.Sonar;
import com.thalesgroup.tusar.lib.convertor.Convertor;

/**
 * Used to parse an xml report in TUSAR format. It is based on jaxb2.
 */
public class TUSARXmlParser {
	
	private Logger logger = LoggerFactory.getLogger(TUSARXmlParser.class);	
	
	
	private TUSARViolationsDataExtractor dataExtractor;
	
	public TUSARXmlParser(TUSARViolationsDataExtractor dataExtractor) {
		this.dataExtractor=dataExtractor;
	}
	
	private Sonar parseXMLFile(java.io.File file) throws SAXException, JAXBException, IOException {
		ClassLoader cl = 
				com.thalesgroup.sonar.lib.model.v2.ObjectFactory.class.getClassLoader();
		
		
		JAXBContext jc = JAXBContext.newInstance("com.thalesgroup.sonar.lib.model.v2", cl);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		
		Thread.currentThread().setContextClassLoader(ConversionServiceFactory.class.getClassLoader());
		File newFile = Convertor.getInstance().convert(file);		
		Sonar model = (Sonar) unmarshaller.unmarshal(newFile);
		try{
			newFile.delete();
		}
		catch (Throwable e){
			logger.error("Can't delete the file " + newFile);
		}
		return model;
               
	}
	
	/**
	 * Main parsing method
	 */
	protected void parse(java.io.File xmlFile, final SensorContext context, final Project project) throws XMLStreamException{

		
		try {
			Sonar model = parseXMLFile(xmlFile);
			
			// <tests> tag exists, so there is tests data
			if (model.getTests() != null) {				
				Collection<TestSuiteReport> testSuiteReports = TUSARTestsDataExtractor.extractTestsData(model, context);
				TUSARTestsDataExtractor.saveToSonarTestsData(testSuiteReports, context, project);
				logger.debug("Tests data extracted from {} and saved into Sonar", xmlFile.getAbsolutePath());
			}
			
			// <coverage> tag exists, so there is coverage data
			if (model.getCoverage() != null) {				
				TUSARCoverageDataExtractor.saveToSonarCoverageData(model, context, project);				
				logger.debug("Coverage data extracted from {} and saved into Sonar", xmlFile.getAbsolutePath());
			}
			
			// <violations> tag exists, so there is violations data
			if (model.getViolations() != null) {				
				dataExtractor.saveToSonarViolationsData(model, context, project);
				logger.debug("Violations data extracted from {} and saved into Sonar", xmlFile.getAbsolutePath());
			}
			
			// <measures> tag exists, so there is measures data
			if (model.getMeasures() != null) {				
				TUSARMeasuresDataExtractor.saveToSonarMeasuresData(model, context, project);				
				logger.debug("Measures data extracted from {} and saved into Sonar", xmlFile.getAbsolutePath());
			}
		}
		catch (JAXBException e) {
			throw new XMLStreamException("Can not parse TUSAR report : ", e);
		}
		catch (SAXException e) {
			throw new XMLStreamException("Can not parse TUSAR report : ", e);
		}
		catch (ParseException e) {
			throw new XMLStreamException("Can not parse TUSAR report : ", e);
		}
		catch (IOException e) {
			throw new XMLStreamException("Can not parse TUSAR report : ", e);
		}


	}
}

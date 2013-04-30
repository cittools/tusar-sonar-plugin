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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.SonarException;

//<<<<<<< HEAD
import com.google.common.collect.Sets;
//=======
//>>>>>>> 3365beeca93509bebbd22339dd35003a86c7ebc1
import com.thalesgroup.sonar.lib.model.v5.DuplicationsComplexType;
import com.thalesgroup.sonar.lib.model.v5.MemoryComplexType;
import com.thalesgroup.sonar.lib.model.v5.SizeComplexType;
import com.thalesgroup.sonar.lib.model.v5.Sonar;
import com.thalesgroup.sonar.plugins.tusar.TUSARLanguage;
import com.thalesgroup.sonar.plugins.tusar.TUSARResource;
import com.thalesgroup.sonar.plugins.tusar.metrics.MemoryMetrics;
import com.thalesgroup.sonar.plugins.tusar.metrics.NewMetrics;
import com.thalesgroup.sonar.plugins.tusar.newmeasures.NewMeasuresExtractor;
import com.thalesgroup.sonar.plugins.tusar.utils.Utils;


/**
 * Contains methods to extract TUSAR tests data.
 */
public class TUSARMeasuresDataExtractor {

	private static Logger logger = LoggerFactory
	.getLogger(TUSARMeasuresDataExtractor.class);

	private static HashMap<String, Metric> metricsMapping = constructMetricsMapping();
	
	private static void processMemory(MemoryComplexType memory, SensorContext context, Project project) throws ParseException {
		for (com.thalesgroup.sonar.lib.model.v5.MemoryComplexType.Resource element : memory.getResource()) {
			Resource<?> resource = constructResource(element.getType(), element.getValue(), project);
			if (resource == null) {
				logger.debug(
						"Path is not valid, {} resource {} does not exists.",
						element.getType(), element.getValue());
			} else {
				for (com.thalesgroup.sonar.lib.model.v5.MemoryComplexType.Resource.Measure measure : element.getMeasure()) {
					String measureKey = measure.getKey().toUpperCase();
					/* create the sonar metric */
					//Metric sonarMetric = metricsMapping.get(measureKey);
					Metric sonarMetric = MemoryMetrics.getMetric(measureKey);
					/********** ***********/
					if (sonarMetric != null) {
						
							double measureValue = ParsingUtils.parseNumber(measure.getValue());
							try{
								Measure m= new Measure(sonarMetric,measureValue);
								context.saveMeasure(resource,m);
							}catch (SonarException e) {
								logger.warn("The measure "+sonarMetric.getName()+" (key:"+sonarMetric.getKey()+") has already been added for resource : "+resource.getLongName());
							}
						
					}
					else {
						Metric unmanagedMetric = NewMetrics.contains(measureKey);
						if (unmanagedMetric!=null){
							NewMeasuresExtractor.treatMeasure(project, context, measure, resource);
						}
					}
				}
			}
		}
	}
	

	private static void processSize(SizeComplexType size, SensorContext context, Project project) throws ParseException {
		
		for (com.thalesgroup.sonar.lib.model.v5.SizeComplexType.Resource element : size
				.getResource()) {
			Resource<?> resource = constructResource(element.getType(),
					element.getValue(), project);
			if (resource == null) {
				logger.debug(
						"Path is not valid, {} resource {} does not exists.",
						element.getType(), element.getValue());
			} else {
				for (com.thalesgroup.sonar.lib.model.v5.SizeComplexType.Resource.Measure measure : element
						.getMeasure()) {
					String measureKey = measure.getKey().toUpperCase();
					Metric sonarMetric = metricsMapping.get(measureKey);
					if (sonarMetric != null) {
						if(measureKey.equalsIgnoreCase("class_complexity_distribution") || measureKey.equalsIgnoreCase("function_complexity_distribution") || measureKey.equalsIgnoreCase("file_complexity_distribution")){
							String measureValue = measure.getValue();
							try{
								context.saveMeasure(resource, new Measure(sonarMetric,measureValue));
							}catch (SonarException e) {
								logger.warn("The measure "+sonarMetric.getName()+" (key:"+sonarMetric.getKey()+") has already been added for resource : "+resource.getLongName());
							}
						}else{
							double measureValue = ParsingUtils.parseNumber(measure.getValue());
							try{
								context.saveMeasure(resource, new Measure(sonarMetric,measureValue));
							}catch (SonarException e) {
								logger.warn("The measure "+sonarMetric.getName()+" (key:"+sonarMetric.getKey()+") has already been added for resource : "+resource.getLongName());
							}
						}
					}
					else {
						Metric unmanagedMetric = NewMetrics.contains(Utils.convertToKeyNorm(measureKey));
						if (unmanagedMetric!=null){
							NewMeasuresExtractor.treatMeasure(project, context, measure, resource);
						}
					}
				}
			}
		}
	}

	static Map<Resource, ResourceData> duplicationsData = new HashMap<Resource, ResourceData>();
	
	static HashSet<String> duplicates=new HashSet<String>();
	private static void addDuplicates(String path1, String startLine1, String path2, String startLine2){
		String sig1=path1+"*"+startLine1+"*"+path2+"*"+startLine2;
		duplicates.add(sig1);
	}
	private static boolean isNewBlock(String path1, String startLine1, String path2, String startLine2){
		String sig1=path1+"*"+startLine1+"*"+path2+"*"+startLine2;
		String sig2=path2+"*"+startLine2+"*"+path1+"*"+startLine1;
		if(duplicates.contains(sig1) /*|| duplicates.contains(sig2)*/){
			return false;
		}else if(path1.equalsIgnoreCase(path2) && (duplicates.contains(sig1) || duplicates.contains(sig2))){
			return false;
		}
		else {
			return true;
		}
		
		
	}
	
	static class ResourceData {

		/*private double duplicatedLines;*/
		private final Set<Integer> duplicatedLines=Sets.newHashSet();
		private double duplicatedBlocks;
		private final List<XmlEntry> duplicationXMLEntries = new ArrayList<XmlEntry>();

		private SensorContext context;
		private Resource resource;
		private String key;
		

		public ResourceData(SensorContext context, Resource resource, String path) {
			this.context = context;
			this.resource = resource;
			Resource resolverdResource=context.getResource(resource);
			
			this.key=resolverdResource.getEffectiveKey();
		}
		
		public void cumulate(HashSet<Part> parts, int duplicationStartLine, int duplicatedLines) {
			
			this.incrementDuplicatedBlock();
			
			duplicationXMLEntries.add(new XmlEntry(duplicationStartLine, duplicatedLines,parts));
		    for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
		      this.duplicatedLines.add(duplicatedLine);
		    }
			
		  }

		  public void incrementDuplicatedBlock() {
		    duplicatedBlocks++;
		  }
		  private String getDuplicationXMLData() {
			    Collections.sort(duplicationXMLEntries, COMPARATOR);
			    StringBuilder duplicationXML = new StringBuilder("<duplications>");
			    for (XmlEntry xmlEntry : duplicationXMLEntries) {
			      duplicationXML.append(xmlEntry.toString());
			    }
			    duplicationXML.append("</duplications>");
			   // logger.info(duplicationXML.toString());
			    return duplicationXML.toString();
			 }

			  private static final Comparator<XmlEntry> COMPARATOR = new Comparator<XmlEntry>() {
			    public int compare(XmlEntry o1, XmlEntry o2) {
			      if (o1.startLine == o2.startLine) {
			        return o1.lines - o2.lines;
			      }
			      return o1.startLine - o2.startLine;
			    }
			  };

			  private final class XmlEntry {
			   
			    private final int startLine;
			    private final int lines;
			    private HashSet<Part> parts;//resource key,string startline
			  
			   
			    private XmlEntry(int startLine, int lines,HashSet<Part> parts){
			    	 this.startLine = startLine;
				     this.lines = lines;
				     this.parts=parts;
			    }
			  

			    @Override
			    public String toString() {
			      StringBuilder str= new StringBuilder();
			          str.append("<g>")
			          .append("<b s=\"").append(startLine).append("\" l=\"").append(lines).append("\" r=\"").append(key).append("\" />");
			          for(Part ekey:parts){
			        	  String targetStartLine=ekey.startLine;
			        	  str.append("<b s=\"").append(targetStartLine).append("\" l=\"").append(lines).append("\" r=\"").append(ekey.key).append("\" />");
			          }			         
			          str.append("</g>");
			
			      
			      return str.toString();
			    }
		  
			  }
		  
		  
		public void saveUsing(SensorContext context, Resource targetResource) {
			logger.info("**************"+targetResource+"***********");
			context.saveMeasure(targetResource, CoreMetrics.DUPLICATED_FILES, 1d);
			logger.info("**** dupliacted files "+1d);
			context.saveMeasure(targetResource, CoreMetrics.DUPLICATED_LINES,(double)duplicatedLines.size());
			logger.info("**** dupliacted lines "+(double)duplicatedLines.size());
			context.saveMeasure(targetResource, CoreMetrics.DUPLICATED_BLOCKS,duplicatedBlocks);
			logger.info("**** dupliacted blocks "+duplicatedBlocks);
			
			 Measure data = new Measure(CoreMetrics.DUPLICATIONS_DATA, getDuplicationXMLData()).setPersistenceMode(PersistenceMode.DATABASE);
		   
			 
			 logger.info(" *****************  \n * "+data.getData());
			try{ 
			 	context.saveMeasure(targetResource, data);
			}catch(Exception e){
				logger.error(e.getMessage());
			}
			 logger.info("**** data \n"+data+"\n");
		}
	}

	private static void processDuplications(DuplicationsComplexType duplications, SensorContext context, Project project) throws ParseException {
		
		Map<Resource, ResourceData> duplicationsData=new HashMap<Resource, ResourceData>();
		
		for (DuplicationsComplexType.Set duplicationSet : duplications.getSet()) {
			
			
			try {

				// Record for each resource which is a file
				List<DuplicationsComplexType.Set.Resource> resources = duplicationSet.getResource();
				
			   boolean isSingleFile=true;
			   
			   search:
				for (int i =0; i<resources.size(); i++){
					
					DuplicationsComplexType.Set.Resource duplicationResourcei = resources.get(i);
					for (int j =0; j<resources.size(); j++){
						DuplicationsComplexType.Set.Resource duplicationResourcej = resources.get(j);
						if(!(duplicationResourcei.getPath().equalsIgnoreCase(duplicationResourcej.getPath()))){
							isSingleFile=false;
							break search;
						}
					}
					
				}
			   
				loo:
				for (int i=0; i<resources.size(); i++){
					
					if(i>0 && isSingleFile==true){
						break loo;
					}

					DuplicationsComplexType.Set.Resource duplicationResource1 = resources.get(i);
					Resource<?> resource1 = constructResource("FILE",duplicationResource1.getPath(), project);
					if(resource1==null){
						logger.warn("resource 1 null");
						continue;
					}
					
					ResourceData firstFileData=getDuplicationsData(context,duplicationsData,resource1,duplicationResource1.getPath());
					
					
					HashSet<Part> parts=new HashSet<Part>();
					
					for (int j =0;j<resources.size();j++) {
						
						DuplicationsComplexType.Set.Resource duplicationResource2 = resources.get(j);
						if ((duplicationResource1.getPath() == duplicationResource2.getPath()) && (Double.parseDouble(duplicationResource1.getLine())==Double.parseDouble(duplicationResource2.getLine()))){
							continue;
						}
						
						Resource<?> resource2 = constructResource("FILE",duplicationResource2.getPath(), project);
						if(resource2==null){
							logger.warn("resource 2 null");
							continue;
						}
						
						Resource resolvResource=context.getResource(resource2);
						String key2=resolvResource.getEffectiveKey();
						String start2=duplicationResource2.getLine();
						Part part=new Part(key2,start2);
						parts.add(part);

						//addDuplicates(duplicationResource1.getPath(),duplicationResource1.getLine(),duplicationResource2.getPath(),duplicationResource2.getLine());
					
					}
					
					firstFileData.cumulate(parts, Integer.parseInt(duplicationResource1.getLine()), Integer.parseInt(duplicationSet.getLines()));
				
				}
						
					
			} catch (NumberFormatException nfe) {
				
				logger.error(nfe.getMessage());
			}

		}
		
		for(Map.Entry<Resource, ResourceData> entry:duplicationsData.entrySet()){
			entry.getValue().saveUsing(context, entry.getKey());
		}
		
	}
	
	private static ResourceData getDuplicationsData(SensorContext context,Map<Resource, ResourceData> fileContainer, Resource file, String resourcePath){
		ResourceData data=fileContainer.get(file);
		if(data==null){
			data = new ResourceData(context,file,resourcePath);
			fileContainer.put(file, data);
		}
		return data;
	}
	
	

	public static void saveToSonarMeasuresData(Sonar model,
			SensorContext context, Project project) throws ParseException {

		// Size
		SizeComplexType size = model.getMeasures().getSize();
		if (size != null) {
			processSize(size, context, project);
		}

		// Duplications
		DuplicationsComplexType duplications = model.getMeasures()
		.getDuplications();
		if (duplications != null) {
			processDuplications(duplications, context, project);
			duplicationsData = new HashMap<Resource, ResourceData>();
			duplicates=new HashSet<String>();
		}

		// Memory
		MemoryComplexType memory = model.getMeasures().getMemory();
		if (memory != null) {
			processMemory(memory, context, project);
		}

	
	}

	private static HashMap<String, Metric> constructMetricsMapping() {
		HashMap<String, Metric> map = new HashMap<String, Metric>();
		for (Metric m : CoreMetrics.getMetrics()){
			map.put(m.getKey().toUpperCase(), m);
		}
		return map;
	}

	private static Resource<?> constructResource(String resourceType,
			String resourcePath, Project project) {

		if ("PROJECT".equals(resourceType.toUpperCase())) {

			return project;
		} else if ("DIRECTORY".equals(resourceType.toUpperCase())) {
			return new Directory(resourcePath, new TUSARLanguage());
		} else if ("FILE".equals(resourceType.toUpperCase())) {
		
			return TUSARResource.fromAbsOrRelativePath(resourcePath, project,
					false);
		}

	
		else {
			return null;
		}
	}
	
	public static void main(String[] args){
		
		ArrayList<String> parts=null;
		
		ArrayList<String> bloc;
		ArrayList<ArrayList<String>> dupSets=new ArrayList<ArrayList<String>>();
		bloc=new ArrayList<String>();
		bloc.add("file1");
		bloc.add("file1");
		bloc.add("file1");
		dupSets.add(bloc);
		bloc=new ArrayList<String>();
		bloc.add("file2");
		bloc.add("file3");
		bloc.add("file4");
		dupSets.add(bloc);
		bloc=new ArrayList<String>();
		bloc.add("file5");
		bloc.add("file5");
		dupSets.add(bloc);
				
		
		for (int k=0; k<dupSets.size();k++) {
			
			ArrayList<String> set=dupSets.get(k);
			
			
			boolean isSingleFile=true;
			search:				
				for (int i=0;i<set.size();i++){
					
					
					String duplicationResourcei = set.get(i);
					
					for (int j=0;j<set.size();j++){
						
						String duplicationResourcej = set.get(j);
						
						if(duplicationResourcej!=duplicationResourcei){
							isSingleFile=false;
							break search;
						}
					}
					
				}
			
				loo:
					for (int i=0;i<set.size();i++){
						parts=new ArrayList<String>();
						String duplicationResourcei = set.get(i);
						if(isSingleFile && i==1){
							
							break loo;
						}
						
						for (int j=0;j<set.size();j++){
							String duplicationResourcej = set.get(j);
							parts.add(duplicationResourcej);
						}
						
						System.out.println("*****Bloc:  "+duplicationResourcei+"*********");
						for(String file:parts){
							System.out.println("-- "+file);
							}
				}
				
				
		}
	}
	
	
	
}

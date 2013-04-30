package com.thalesgroup.sonar.plugins.tusar.sensors;



import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DuplicationsData {

	
	
	private static Logger logger = LoggerFactory.getLogger(DuplicationsData.class);

  private String resourcePath;
  private final Set<Integer> duplicatedLines = Sets.newHashSet();
  private final List<XmlEntry> duplicationXMLEntries = Lists.newArrayList();

  private double duplicatedBlocks;
  
 /* private SensorContext context;*/
  
  private Resource<?> resource;
	
  public DuplicationsData(String resourcePath) {
 
    this.resourcePath= resourcePath;
  }

  public void cumulate(String targetPath, int targetDuplicationStartLine, int duplicationStartLine, int duplicatedLines) {
    
	duplicationXMLEntries.add(new XmlEntry(targetPath, targetDuplicationStartLine, duplicationStartLine, duplicatedLines));
    for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
      this.duplicatedLines.add(duplicatedLine);
    }
  }

  public void incrementDuplicatedBlock() {
    duplicatedBlocks++;
  }

  public void save(SensorContext context, Resource resource) {
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, (double) duplicatedLines.size());
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, duplicatedBlocks);

    Measure data = new Measure(CoreMetrics.DUPLICATIONS_DATA, getDuplicationXMLData())
        .setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(resource, data);
  }

  private String getDuplicationXMLData() {
    Collections.sort(duplicationXMLEntries, COMPARATOR);
    StringBuilder duplicationXML = new StringBuilder("<duplications>");
    for (XmlEntry xmlEntry : duplicationXMLEntries) {
      duplicationXML.append(xmlEntry.toString());
    }
    duplicationXML.append("</duplications>");
    logger.info(duplicationXML.toString());
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
    private final String target;
    private final int targetStartLine;
    private final int startLine;
    private final int lines;

    private XmlEntry(String target, int targetStartLine, int startLine, int lines) {
      this.target = target;
      this.targetStartLine = targetStartLine;
      this.startLine = startLine;
      this.lines = lines;
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append("<g>")
          .append("<b s=\"").append(startLine).append("\" l=\"").append(lines).append("\" r=\"").append(resourcePath).append("\" />")
          .append("<b s=\"").append(targetStartLine).append("\" l=\"").append(lines).append("\" r=\"").append(target).append("\" />")
          .append("</g>")
          .toString();
    }
  }

}
package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;

import com.google.common.collect.Sets;

class DuplicationData {

	public static class Part {

		public final String key;
		public final String startLine;

		public Part(String key, String startLine) {
			this.key = key;
			this.startLine = startLine;
		}
	}

	private static class XmlEntry {

		private final String key;
		private final int startLine;
		private final int lines;
		private Set<Part> parts;

		private XmlEntry(String key, int startLine, int lines, Set<Part> parts) {
			this.key = key;
			this.startLine = startLine;
			this.lines = lines;
			this.parts = parts;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append("<g>").append("<b s=\"").append(startLine).append("\" l=\"").append(lines).append("\" r=\"")
			        .append(key).append("\" />");
			for (Part ekey : parts) {
				String targetStartLine = ekey.startLine;
				str.append("<b s=\"").append(targetStartLine).append("\" l=\"").append(lines).append("\" r=\"")
				        .append(ekey.key).append("\" />");
			}
			str.append("</g>");

			return str.toString();
		}
	}

	private static final Comparator<XmlEntry> XmlEntryComparator = new Comparator<XmlEntry>() {

		@Override
		public int compare(XmlEntry o1, XmlEntry o2) {
			if (o1.startLine == o2.startLine) {
				return o1.lines - o2.lines;
			}
			return o1.startLine - o2.startLine;
		}
	};

	private String key;

	private final List<XmlEntry> duplicationXMLEntries = new ArrayList<XmlEntry>();

	private double duplicatedBlockCount;

	private Set<Integer> overallDuplicatedLines = Sets.newHashSet();

	public DuplicationData(SensorContext context, Resource resource, String path) {
		Resource resolverdResource = context.getResource(resource);
		key = resolverdResource.getEffectiveKey();
	}

	public void cumulate(HashSet<Part> parts, int duplicationStartLine, int duplicatedLines) {
		duplicatedBlockCount++;
		duplicationXMLEntries.add(new XmlEntry(key, duplicationStartLine, duplicatedLines, parts));
		for (int duplicatedLine = duplicationStartLine; duplicatedLine < duplicationStartLine + duplicatedLines; duplicatedLine++) {
			overallDuplicatedLines.add(duplicatedLine);
		}
	}

	public double getOverallDuplicatedLineCount() {
		return overallDuplicatedLines.size();
	}

	public double getDuplicatedBlockCount() {
		return duplicatedBlockCount;
	}

	public String getDuplicationXMLData() {
		Collections.sort(duplicationXMLEntries, XmlEntryComparator);
		StringBuilder duplicationXML = new StringBuilder("<duplications>");
		for (XmlEntry xmlEntry : duplicationXMLEntries) {
			duplicationXML.append(xmlEntry);
		}
		duplicationXML.append("</duplications>");
		return duplicationXML.toString();
	}
}

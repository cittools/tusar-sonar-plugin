package com.thalesgroup.sonar.plugins.tusar.reports;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.thalesgroup.sonar.plugins.tusar.sensors.TusarCoverageSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarMeasuresSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarTestsSensor;
import com.thalesgroup.sonar.plugins.tusar.sensors.TusarViolationsSensor;

public class ReportExtractorTest {

	@Before
	public void setUp() {
		for (ReferenceExtractor refExtractor : Arrays.asList( //
		        TusarCoverageSensor.refExtractor, //
		        TusarMeasuresSensor.refExtractor, //
		        TusarTestsSensor.refExtractor, //
		        TusarViolationsSensor.refExtractor)) {
			ReportExtractor.registerReferenceExtractor(refExtractor);
		}
	}

	/*
	 * Note that X:\ is an absolute directory, but X: isn't.
	 */
	@Test
	public void testCollectFileSuffixes() {
		Set<String> suffixes;

		String tusarPath = "com/thalesgroup/sonar/plugins/tusar/reports";

		ReportExtractor extractor = new ReportExtractor(null, new String[0], new String[0], new String[] { tusarPath });
		suffixes = extractor.collectFileSuffixes();
		Assert.assertEquals(Collections.emptySet(), suffixes);

		String path = getClass().getResource("tusar.xml").getPath();
		Matcher matcher = Pattern.compile("/(\\w:/.*)/" + Pattern.quote(tusarPath) + "/tusar\\.xml").matcher(path);
		Assert.assertTrue(matcher.matches());
		String rootDir = matcher.group(1);

		extractor = new ReportExtractor(new File(rootDir), new String[] { tusarPath }, new String[0],
		        new String[] { tusarPath });
		/*
		 * Utiliser des références existantes !
		 */
		suffixes = extractor.collectFileSuffixes();
		Assert.assertEquals(Collections.emptySet(), suffixes);

		try {
			new ReportExtractor(new File(findNonExistentDriveLetter()), new String[] { "somewhere" }, new String[0],
			        new String[] { tusarPath });
			Assert.fail();
		} catch (IllegalArgumentException e) {
		}
	}

	private String findNonExistentDriveLetter() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVXYZ";
		for (int i = 0, iEnd = alphabet.length(); i < iEnd; ++i) {
			String root = alphabet.substring(i, 1);
			if (!new File(root).exists()) {
				return root;
			}
		}
		return null;
	}
}

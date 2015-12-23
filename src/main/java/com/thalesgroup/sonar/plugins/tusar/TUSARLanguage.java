package com.thalesgroup.sonar.plugins.tusar;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.resources.AbstractLanguage;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class TusarLanguage extends AbstractLanguage {

	private static final Logger logger = LoggerFactory.getLogger(TusarLanguage.class);

	public static final String KEY = "tusar";

	public static final String NAME = "TUSAR";

	private static final String IMPOSSIBLE_EXTENSION = "\0";

	public TusarLanguage() {
		super(KEY, NAME);
	}

	@Override
	public String[] getFileSuffixes() {
		String[] suffixes = TusarProjectBuilder.getDynamicFileSuffixes();
		if (suffixes == null) {
			/*
			 * Extensions for the TUSAR language are dynamic for each project
			 * and are not meant to be configured server-side.
			 */
			suffixes = new String[0];
		} else {
			logger.info("Dynamically TUSAR language suffixes: {}", Arrays.toString(suffixes));
			if (suffixes.length == 0) {
				/*
				 * As stated by the Language interface: [the return value,] if
				 * empty, then all files in source directories are considered as
				 * sources. Since that's the exact opposite of what we want, we
				 * use a unmatchable extension instead.
				 */
				suffixes = new String[] { IMPOSSIBLE_EXTENSION };
			}
		}
		return suffixes;
	}
}

package com.thalesgroup.sonar.plugins.tusar.rulesrepository;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.utils.ValidationMessages;

public class SonarTusarProfile extends ProfileDefinition {

	private static final String SONAR_TUSAR_PROFILE_XML = "com/thalesgroup/sonar/plugins/tusar/sonar-tusar-profile.xml";
	/**
	 * XML profile parser.
	 */
	private XMLProfileParser parser;

	/**
	 * @param parser
	 */
	public SonarTusarProfile(XMLProfileParser parser) {
		this.parser = parser;
	}

	/**
	 * @see org.sonar.api.profiles.ProfileDefinition#createProfile(org.sonar.api.utils.ValidationMessages)
	 */
	@Override
	public RulesProfile createProfile(ValidationMessages messages) {
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream stream = classLoader
				.getResourceAsStream(SONAR_TUSAR_PROFILE_XML);
		Reader reader = new InputStreamReader(stream);
		return parser.parse(reader, messages);
	}

}

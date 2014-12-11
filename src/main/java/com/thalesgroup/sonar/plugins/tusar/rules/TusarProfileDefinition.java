package com.thalesgroup.sonar.plugins.tusar.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;

public class TusarProfileDefinition extends ProfileDefinition {

	private static Logger logger = LoggerFactory.getLogger(TusarProfileDefinition.class);

	public static final String PROFILE_NAME = "TUSAR way";

	private Language language;

	private RuleFinder ruleFinder;

	public TusarProfileDefinition(TusarLanguage language, RuleFinder ruleFinder) {
		this.language = language;
		this.ruleFinder = ruleFinder;
	}

	/**
	 * This method is always called at startup but the returned profile will
	 * only be used if no other profil exist for the targeted language.
	 */
	@Override
	public RulesProfile createProfile(ValidationMessages messages) {
		RulesProfile profile = RulesProfile.create(PROFILE_NAME, language.getKey());
		profile.setDefaultProfile(true);
		for (Rule rule : ruleFinder.findAll(RuleQuery.create().withRepositoryKey(TusarRuleDefinitions.REPOSITORY_KEY))) {
			profile.activateRule(rule, null);
		}
		logger.info("Created default profile s({}, '{}')", language.getKey(), PROFILE_NAME);
		return profile;
	}
}

package com.thalesgroup.sonar.plugins.tusar.decorators;

import org.sonar.api.profiles.RulesProfile;
import org.sonar.commonrules.api.CommonRulesDecorator;

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;

public class TusarCommonRulesDecorator extends CommonRulesDecorator {

	@SuppressWarnings("deprecation")
	public TusarCommonRulesDecorator(org.sonar.api.resources.ProjectFileSystem fs, RulesProfile qProfile) {
		super(TusarLanguage.KEY, fs, qProfile);
	}
}

package com.thalesgroup.sonar.plugins.tusar.rules;

import org.sonar.commonrules.api.CommonRulesEngine;
import org.sonar.commonrules.api.CommonRulesRepository;

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;

public class TusarCommonRulesEngine extends CommonRulesEngine {

	public TusarCommonRulesEngine(TusarLanguage language) {
		super(language.getKey());
	}

	@Override
	protected void doEnableRules(CommonRulesRepository repository) {
		/*
		 * null parameters -> keep default values as hardcoded in
		 * sonar-common-rules
		 */
		repository //
		        .enableDuplicatedBlocksRule() //
		        .enableSkippedUnitTestsRule() //
		        .enableFailedUnitTestsRule() //
		        .enableInsufficientBranchCoverageRule(null) //
		        .enableInsufficientCommentDensityRule(null) //
		        .enableInsufficientLineCoverageRule(null);
	}
}

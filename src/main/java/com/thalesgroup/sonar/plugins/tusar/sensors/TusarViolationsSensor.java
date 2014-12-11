package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ParsingUtils;

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;
import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.tusar.v12.Tusar;
import com.thalesgroup.tusar.violations.v4.ViolationsComplexType;

public class TusarViolationsSensor extends AbstractSensor {

	private static final Logger logger = LoggerFactory.getLogger(TusarViolationsSensor.class);

	public static ReferenceExtractor refExtractor = new ReferenceExtractor() {

		@Override
		public Collection<? extends String> getReferencedResourcePaths(Tusar model) {
			List<String> paths = new LinkedList<String>();
			ViolationsComplexType violations = model.getViolations();
			if (violations != null) {
				for (ViolationsComplexType.File file : violations.getFile()) {
					paths.add(file.getPath());
				}
			}
			return paths;
		}
	};

	private RuleFinder ruleFinder;

	private ActiveRules activeRules;

	private ResourcePerspectives perspectives;

	public TusarViolationsSensor(Settings settings, RuleFinder ruleFinder, ActiveRules activeRules,
	        ResourcePerspectives perspectives) {
		super(settings, null);
		this.ruleFinder = ruleFinder;
		this.activeRules = activeRules;
		this.perspectives = perspectives;
	}

	@Override
	protected void internalAnalyse(Context context) {
		for (ReportExtractor.Report report : context.getReports()) {
			logger.debug("Injecting data from report '{}'", report.location);
			context.report(report);
			try {
				ViolationsComplexType violations = report.model.getViolations();
				if (violations != null) {
					for (ViolationsComplexType.File file : violations.getFile()) {
						Resource resource = context.resolveResource(file.getPath());
						if (resource != null) {
							context.resource(resource);
							for (ViolationsComplexType.File.Violation violation : file.getViolation()) {
								Rule rule = findActiveTusarRule(violation.getKey());
								if (rule != null) {
									/*
									 * TODO Add a dedicated preprocessing stage
									 * for issues?
									 */
									Issuable issuable = perspectives.as(Issuable.class, resource);
									if (issuable != null) {
										Issue issue = issuable.newIssueBuilder().ruleKey(rule.ruleKey()) //
										        .line(parseLineIndex(violation.getLine())) //
										        .build();
										issuable.addIssue(issue);
									}
								}
							}
						}
					}
				}
			} catch (ParseException e) {
				logger.error("Failed to extract violation data", e);
			}
		}
	}

	private Integer parseLineIndex(String line) throws ParseException {
		if (StringUtils.isNotBlank(line) && line.indexOf('-') == -1) {
			return (int) ParsingUtils.parseNumber(line);
		} else {
			return null;
		}
	}

	private Map<String, Rule> ruleCache = new WeakHashMap<String, Rule>();

	private Rule findActiveTusarRule(String key) {
		Rule rule = null;
		if (!ruleCache.containsKey(key)) {
			List<Rule> matchingRules = new LinkedList<Rule>();
			for (ActiveRule activeRule : activeRules.findAll()) {
				if (activeRule.ruleKey().rule().equals(key)) {
					Rule potentialRule = ruleFinder.findByKey(activeRule.ruleKey());
					if (potentialRule.getLanguage().equalsIgnoreCase(TusarLanguage.KEY)) {
						matchingRules.add(potentialRule);
						break;
					}
				}
			}
			int matchCount = matchingRules.size();
			if (matchCount == 0) {
				rule = null;
				logger.warn("Discarding violations refering to the unkown or unactive TUSAR rule '{}'.", key);
			} else if (matchCount == 1) {
				rule = matchingRules.get(0);
			} else {
				rule = matchingRules.get(0);
				if (logger.isWarnEnabled()) {
					Set<String> repositories = new HashSet<String>();
					for (Rule matchingRule : matchingRules) {
						repositories.add(matchingRule.ruleKey().repository());
					}
					logger.warn(
					        "Arbitrary rule resolution of key '{}' between the following TUSAR repositories: '{}'.",
					        key, repositories);
				}
			}
			ruleCache.put(key, rule);
		} else {
			rule = ruleCache.get(key);
		}
		return rule;
	}
}

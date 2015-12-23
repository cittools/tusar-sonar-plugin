package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.lang.reflect.Field;
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

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;
import com.thalesgroup.sonar.plugins.tusar.reports.ReferenceExtractor;
import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;
import com.thalesgroup.tusar.v12.Tusar;
import com.thalesgroup.tusar.violations.v4.ViolationsComplexType;

public class TusarViolationsSensor extends AbstractSensor {

	private static final String RESOLUTION_FIELD = "resolution";
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
										if (violation.getSeverity().equals("false positive")) {
											setIssueToFalsePositive(issue);
										}
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
	
	private Issue setIssueToFalsePositive(Issue issue) {
		Class<? extends Issue> defaultIssueClass = issue.getClass();
		try {
			Field declaredField = defaultIssueClass.getDeclaredField(RESOLUTION_FIELD);
			declaredField.setAccessible(true);
			declaredField.set(issue, Issue.RESOLUTION_FALSE_POSITIVE);
			declaredField.setAccessible(false);
			
		} catch (NoSuchFieldException e) {
			logger.error("Issue don't have resolution field", e);
		} catch (SecurityException e) {
			logger.error("Security problem when managing resolution field of an issue", e);
		} catch (IllegalArgumentException e) {
			logger.error("Illegal argument when setting the resolution field for issue", e);
		} catch (IllegalAccessException e) {
			logger.error("Can't access resolution field for the issue", e);
		}
		return issue;
	}

	private Integer parseLineIndex(String line) throws ParseException {
		if (StringUtils.isNotBlank(line)) {
			try {
				int value = Integer.parseInt(line);
				if (value > 0) {
					return value;
				} else {
					/*
					 * Null or negative values are no longer valid values to
					 * indicate the whole file. The proper way to do so with
					 * SonarQube 4.x is to provide an empty line value.
					 */
					return null;
				}
			} catch (NumberFormatException e) {
				logger.warn("Unvalid '{}' value provided for a line number will be interpreted as an empty value.",
				        line);
				return null;
			}
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

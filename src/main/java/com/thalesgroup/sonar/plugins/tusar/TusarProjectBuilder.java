package com.thalesgroup.sonar.plugins.tusar;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.events.ProjectAnalysisHandler;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;

import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;

public class TusarProjectBuilder extends ProjectBuilder implements ProjectAnalysisHandler {

	private static final Logger logger = LoggerFactory.getLogger(TusarProjectBuilder.class);

	private Settings batchSettings;

	private Languages languages;

	private static Set<String> dynamicFileSuffixes;

	public TusarProjectBuilder(Settings batchSettings, Languages languages) {
		this.batchSettings = batchSettings;
		this.languages = languages;
	}

	@Override
	public void build(Context context) {
		ReportExtractor.createRootInstance(context.projectReactor().getRoot(), batchSettings);
		super.build(context);
	}

	/**
	 * This method is both important and problematic. From SonarQube point of
	 * view, a language is a value object and, as such, is not expected to
	 * change over time. Fortunately, the actual implementation of SonarQube
	 * (4.3) seems to call {@link Language#getFileSuffixes()} (quite)
	 * frequently, without caching the returned value. It is fortunate because
	 * the TUSAR language does change and have a dynamic file suffix set defined
	 * by calling this method. On each project (modules are projects), the file
	 * suffix set is calculated to have every TUSAR file caught (and indexed) by
	 * SonarQube. Let's hope that SonarQube implementation does not change
	 * regarding this behavior.
	 */
	static String[] getDynamicFileSuffixes() {
		logger.info("Dynamically TUSAR language suffixes: {}", dynamicFileSuffixes);
		return dynamicFileSuffixes.toArray(new String[0]);
	}

	@Override
	public void onProjectAnalysis(ProjectAnalysisEvent event) {
		Project project = event.getProject();
		ReportExtractor tusarExtractor = ReportExtractor.getInstance(project);

		if (event.isStart()) {
			/*
			 * We truly need this information, that is knowing the
			 * 'sonar.language' value at a given project level.
			 */
			@SuppressWarnings("deprecation")
			Language projectLanguage = event.getProject().getLanguage();

			boolean multiLanguages = Project.NONE_LANGUAGE.getKey().equals(projectLanguage.getKey());
			if (multiLanguages) {
				Set<String> tusarOnlySuffixes = tusarExtractor.collectFileSuffixes();
				for (Language language : languages.all()) {
					if (!TusarLanguage.KEY.equals(language.getKey())) {
						for (String suffix : language.getFileSuffixes()) {
							tusarOnlySuffixes.remove(suffix);
						}
					}
				}
				/*
				 * The TUSAR language shall only catch the files not already
				 * caught by other existing languages (otherwise there will be
				 * conflict).
				 */
				dynamicFileSuffixes = tusarOnlySuffixes;
			} else if (TusarLanguage.KEY.equals(projectLanguage.getKey())) {
				/*
				 * The TUSAR language is alone and need to catch every file
				 * referenced in the TUSAR reports.
				 */
				dynamicFileSuffixes = tusarExtractor.collectFileSuffixes();
			} else {
				/*
				 * Note that an empty set doesn't mean there will be no TUSAR
				 * data injected, simply that only data for files caught by the
				 * project language will be taken into account.
				 */
				dynamicFileSuffixes = Collections.emptySet();
			}

			if (!dynamicFileSuffixes.isEmpty()) {
				tusarExtractor.createPlaceholderResources();
			}
		} else if (event.isEnd()) {
			if (!dynamicFileSuffixes.isEmpty()) {
				tusarExtractor.destroyPlaceholderResources();
			}
			dynamicFileSuffixes = null;
		}
	}
}

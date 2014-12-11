package com.thalesgroup.sonar.plugins.tusar.rules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.resources.Language;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

import com.thalesgroup.sonar.plugins.tusar.TusarLanguage;

public class TusarRuleDefinitions implements RulesDefinition {

	private static Logger logger = LoggerFactory.getLogger(TusarRuleDefinitions.class);

	public static final String REPOSITORY_KEY = "tusar";

	public static final String REPOSITORY_NAME = "TUSAR rules";

	public static final String TUSAR_TAG = "tusar-import";

	private Language language;

	private ServerFileSystem serverFileSystem;

	private RulesDefinitionXmlLoader loader;

	public TusarRuleDefinitions(TusarLanguage language, ServerFileSystem serverFileSystem,
	        RulesDefinitionXmlLoader loader) {
		this.language = language;
		this.serverFileSystem = serverFileSystem;
		this.loader = loader;
	}

	@Override
	public void define(Context context) {
		/*
		 * Gives the ability for the user to extends system rules withs its own
		 * rules. user rules must be set in
		 * SONAR_HOME/extensions/rules/REPOSITORY_KEY/*.xml
		 */
		for (File rulesetFile : getRulesetFiles()) {
			try {
				String name = rulesetFile.getName();
				name = name.substring(0, name.length() - 4);
				String key = REPOSITORY_KEY + "." + name.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
				NewRepository childRepository = context.createRepository(key, language.getKey()).setName(name);
				loadXml(childRepository, new FileInputStream(rulesetFile));
				for (NewRule rule : childRepository.rules()) {
					rule.addTags(TUSAR_TAG);
				}
				childRepository.done();
				logger.info("Loaded rule set ({}, '{}') from file '{}'", key, name, rulesetFile);
			} catch (IOException e) {
				logger.error("When loading ruleset " + rulesetFile, e);
			}
		}
	}

	private File[] getRulesetFiles() {
		File path = new File(serverFileSystem.getHomeDir(), "extensions/rules/" + REPOSITORY_KEY);
		if (path.exists()) {
			return path.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
		} else {
			logger.info("No TUSAR rules extensions defined.");
			return new File[0];
		}
	}

	private void loadXml(NewRepository repository, InputStream inputStream) throws IOException {
		try {
			loader.load(repository, inputStream, Charset.defaultCharset().name());
		} finally {
			inputStream.close();
		}
	}
}

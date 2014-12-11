package com.thalesgroup.sonar.plugins.tusar.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jenkinsci.lib.dtkit.util.converter.ConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import com.thalesgroup.sonar.plugins.tusar.TusarProjectBuilder;
import com.thalesgroup.tusar.lib.convertor.Convertor;
import com.thalesgroup.tusar.v12.Tusar;

/**
 * Extract TUSAR content for a whole project.
 * 
 * This is a singleton initialiazed at startup with the root
 * {@link ProjectDefinition} which collects every TUSAR report for each project
 * (or module, or sub-module...) using the {@link #TUSAR_REPORTS_PATHS_PROPERTY}
 * .
 * 
 * An instance of this class is required quite early by the
 * {@link TusarProjectBuilder} when the project definition is built which
 * happens before BatchExtension are made available. That's the reason why this
 * class is not a BatchExtension and need to be accessed through
 * {@link #getInstance(Settings)}. That's also the reason why the related
 * properties can't be persisted in database and need to be provided explicitly.
 */
public class ReportExtractor {

	private static final Logger logger = LoggerFactory.getLogger(ReportExtractor.class);

	/**
	 * Paths to the TUSAR report directories. It's a multi-values property using
	 * the SonarQube standard mechanism (with ',' as a delimiter), but the old
	 * ';' delimiter is still supported. So, from a code point of view, the
	 * value is an array of values which contains many paths delimited by ';'.
	 */
	public static final PropertyDefinition TUSAR_REPORTS_PATHS_PROPERTY = PropertyDefinition
	        .builder("sonar.tusar.reportsPaths")
	        .subCategory("Reports")
	        .name("Reports paths")
	        .description(
	                "List of paths to TUSAR XML reports directories. "
	                        + "Paths can be absolute or relative to the project's base directory.") //
	        .type(PropertyType.STRING) //
	        .multiValues(true) //
	        .hidden() // Can't be set through the UI (and persisted in database)
	        .build();

	public static final PropertyDefinition TUSAR_USE_PLACEHOLDER_PROPERTY = PropertyDefinition
	        .builder("sonar.tusar.usePlaceholderResources")
	        .subCategory("Reports")
	        .name("Use placeholder resources")
	        .description(
	                "Virtual resources will be automatically created "
	                        + "for unresolvable resources referenced in provided TUSAR reports.") //
	        .type(PropertyType.BOOLEAN) //
	        .hidden() // Can't be set through the UI (and persisted in database)
	        .build();

	/*
	 * This folder is required to be relative to the base directory. It would be
	 * better to use a generated name which doesn't exist, but keeping it the
	 * same is better to preserve the SonarQube history.
	 */
	private static final String PLACEHOLDER_SOURCE_DIR = ".tusar";

	private static final String PLACEHOLDER_EXT = ".placeholder";

	private static Pattern ILLEGAL_FILE_CHARACTER = Pattern.compile("[^A-Za-z0-9()\\\\[\\\\]{}°%$@#^!,_+\\\\-\\\\='.]");

	public static class Report {

		public final File location;

		public final Tusar model;

		public Report(File location, Tusar Tusar) {
			this.location = location;
			this.model = Tusar;
		}
	}

	protected static Collection<ReferenceExtractor> extractors = new LinkedList<ReferenceExtractor>();

	/**
	 * As stated above, the TUSAR extraction is a preprocessing which takes
	 * place early which prevents us to benefit from the SonarQube injection of
	 * dependencies. That's why we need to do it by hand.
	 */
	public static void registerReferenceExtractor(ReferenceExtractor extractor) {
		logger.debug("Register static reference extractor: {}", extractor.getClass());
		extractors.add(extractor);
	}

	private static ReportExtractor rootInstance;

	public static ReportExtractor createRootInstance(ProjectDefinition projectDefinition, Settings batchSettings) {
		rootInstance = new ReportExtractor(projectDefinition, batchSettings);
		return rootInstance;
	}

	/**
	 * @param projectKey
	 *            A project or module or sub-module key. SonarQube terminology
	 *            somewhat lack clarity for the project hierarchy
	 */
	public static ReportExtractor getInstance(Project project) {
		if (rootInstance != null) {
			String projectKey = project.getKey();
			String branch = project.getBranch();
			/*
			 * Remove the branch suffix if it exists since project definitions
			 * used to create instances of this class doesn't take into account
			 * the branch either.
			 */
			if (branch != null) {
				projectKey = projectKey.substring(0, projectKey.length() - branch.length() - 1);
			}
			return rootInstance.findProject(projectKey);
		} else {
			throw new RuntimeException("No report extractor instance!");
		}
	}

	/**
	 * The project key (or module, sub-module...).
	 */
	private final String key;

	private File placeholderSourceDir;

	private Collection<Report> reports;

	private List<ReportExtractor> moduleReportExtractors = new LinkedList<ReportExtractor>();

	/**
	 * File here means Java File, a file which is a file or a file which is a
	 * directory. I do know that everything is a file on Unix, but still... we
	 * should have a better terminology.
	 */
	private Map<String, File> pathToExistingCanonicalFile = new HashMap<String, File>();

	/**
	 * It's important to note that only batch settings are available when
	 * project definitions can be tweaked. Project settings including properties
	 * from the database would be useful, but are made available later when it
	 * is too late. It is a reason why our two properties are hidden, can't be
	 * set at UI level (i.e. persisted in database) and need to be set when
	 * launching the analysis (sonar-runner property file or Maven POM).
	 */
	private ReportExtractor(ProjectDefinition projectDefinition, Settings batchSettings) {
		key = projectDefinition.getKey();

		File baseDir = projectDefinition.getBaseDir();
		String[] sourceDirs = projectDefinition.getSourceDirs().toArray(new String[0]);
		String[] testDirs = projectDefinition.getTestDirs().toArray(new String[0]);

		/*
		 * Note: "default" values for undefined properties are false and empty
		 * array.
		 */
		String prefix = getPrefix(projectDefinition);
		String[] reportsPaths = splitAgain(batchSettings.getStringArray(prefix + TUSAR_REPORTS_PATHS_PROPERTY.key()));
		boolean usePlaceholderResources = batchSettings.getBoolean(prefix + TUSAR_USE_PLACEHOLDER_PROPERTY.key());

		if (usePlaceholderResources) {
			/*
			 * The directory shall exist in be relative to the base directory to
			 * be taken into account.
			 */
			placeholderSourceDir = new File(projectDefinition.getBaseDir(), PLACEHOLDER_SOURCE_DIR);
			if (placeholderSourceDir.exists() || placeholderSourceDir.mkdirs()) {
				projectDefinition.addSourceDirs(placeholderSourceDir);
			} else {
				placeholderSourceDir = null;
				logger.error("Cannot create the TUSAR placeholder directory");
			}
		}

		if (logger.isDebugEnabled()) {
			StringBuilder message = new StringBuilder("-------------  Extracting TUSAR reports\n");
			message.append("\tprojectKey:              ").append(key).append('\n');
			message.append("\tbaseDir:                 ").append(baseDir).append('\n');
			message.append("\tsourceDirs:              ").append(Arrays.toString(sourceDirs)).append('\n');
			message.append("\ttestDirs:                ").append(Arrays.toString(testDirs)).append('\n');
			message.append("\treportsPaths:            ").append(Arrays.toString(reportsPaths)).append('\n');
			message.append("\tusePlaceholderResources: ").append(usePlaceholderResources);
			logger.debug(message.toString());
		}
		init(baseDir, sourceDirs, testDirs, reportsPaths);

		for (ProjectDefinition subProjectDefinition : projectDefinition.getSubProjects()) {
			ReportExtractor moduleReportExtractor = new ReportExtractor(subProjectDefinition, batchSettings);
			moduleReportExtractors.add(moduleReportExtractor);
		}
	}

	/**
	 * Meant for test only.
	 */
	ReportExtractor(File baseDir, String[] sourceDirs, String[] testDirs, String[] reportsPaths) {
		key = null;
		init(baseDir, sourceDirs, testDirs, reportsPaths);
	}

	private void init(File baseDir, String[] sourceDirs, String[] testDirs, String[] reportsPaths) {
		reports = loadReports(baseDir, reportsPaths);
		if (reports.isEmpty()) {
			logger.info("No TUSAR reports found for project: {}", key);
		}
		resolvePaths(baseDir, sourceDirs, testDirs);
	}

	private ReportExtractor findProject(String projectKey) {
		if (same(key, projectKey)) {
			return this;
		} else {
			for (ReportExtractor moduleReportExtractor : moduleReportExtractors) {
				ReportExtractor hit = moduleReportExtractor.findProject(projectKey);
				if (hit != null) {
					return hit;
				}
			}
		}
		return null;
	}

	/**
	 * Resolve file paths referenced in TUSAR reports against the project source
	 * directories (as configured using "sonar.Sources"). Unresolved paths will
	 * be ignored later and corresponding resource measures won't be injected
	 * into SonarQube. It is not necessarily a problem because, in some use
	 * cases, we wan't to filter out resources which exist but analysed in
	 * another project or module.
	 */
	protected void resolvePaths(File baseDir, String[] sourceDirs, String[] testDirs) {

		List<String> allSourceDirs = new ArrayList<String>(sourceDirs.length + testDirs.length);
		for (String sourceDir : sourceDirs) {
			allSourceDirs.add(sourceDir);
		}
		for (String testDir : testDirs) {
			allSourceDirs.add(testDir);
		}

		Map<String, File> canonicalRoots = new HashMap<String, File>();
		for (String sourceDir : allSourceDirs) {
			File dir = getAbsolutePath(baseDir, sourceDir);
			if (dir.exists() && dir.isDirectory()) {
				try {
					canonicalRoots.put(dir.getCanonicalPath(), dir.getCanonicalFile());
				} catch (IOException e) {
					throw new IllegalArgumentException(
					        "Provided source directory does exist but can't be canonized (removed meanwhile?): " + dir);
				}
			} else {
				throw new IllegalArgumentException("Provided source directory does not exist "
				        + "(remember that SonarQube requires source directories "
				        + "to be relative to the project's base directory): " + dir);
			}
		}

		for (Report report : reports) {
			for (ReferenceExtractor extractor : extractors) {
				for (String path : extractor.getReferencedResourcePaths(report.model)) {
					if (!pathToExistingCanonicalFile.containsKey(path)) {
						File file = new File(path);
						File root = null;
						if (!file.isAbsolute()) {
							for (File canonicalRoot : canonicalRoots.values()) {
								file = new File(canonicalRoot, path);
								if (file.exists()) {
									root = canonicalRoot;
									break;
								}
							}
						} else {
							if (file.exists()) {
								for (Map.Entry<String, File> entry : canonicalRoots.entrySet()) {
									if (path.startsWith(entry.getKey())) {
										root = entry.getValue();
										break;
									}
								}
							}
						}
						if (root == null) {
							file = null;
						}
						logger.trace("In TUSAR report '{}', path '{}' has been resolved to '{}' against root '{}'.",
						        report.location, path, file, root);
						pathToExistingCanonicalFile.put(path, file);
					}
				}
			}
		}
	}

	public Set<String> collectFileSuffixes() {
		Set<String> suffixes = new HashSet<String>();

		if (placeholderSourceDir != null) {
			suffixes.add(PLACEHOLDER_EXT);
		}

		for (File file : pathToExistingCanonicalFile.values()) {
			if (file != null) {
				String fileName = file.getName();
				int suffixIndex = fileName.lastIndexOf('.');
				if (suffixIndex != -1) {
					String suffix = fileName.substring(suffixIndex);
					suffixes.add(suffix.toLowerCase());
				}
			}
		}

		for (ReportExtractor moduleReportExtractor : moduleReportExtractors) {
			suffixes.addAll(moduleReportExtractor.collectFileSuffixes());
		}

		return suffixes;
	}

	public void createPlaceholderResources() {
		if (placeholderSourceDir != null) {
			for (Map.Entry<String, File> entry : pathToExistingCanonicalFile.entrySet()) {
				if (entry.getValue() == null) {
					String name = entry.getKey();
					name = ILLEGAL_FILE_CHARACTER.matcher(name).replaceAll("_");
					name = name.replace('\\', '/');
					File file = new File(placeholderSourceDir, name + PLACEHOLDER_EXT);
					for (int i = 2; file.exists(); ++i) {
						file = new File(placeholderSourceDir, name + " (" + i + ")" + PLACEHOLDER_EXT);
					}
					try {
						File parentFile = file.getParentFile();
						if (parentFile.exists() || parentFile.mkdirs()) {
							if (file.createNewFile()) {
								BufferedWriter writer = new BufferedWriter(new FileWriter(file));
								writer.write("Placeholder for unresolvable resource:");
								writer.newLine();
								writer.write('\t');
								writer.write(entry.getKey());
								writer.close();
								logger.debug("Successfully created placeholder '{}' for resource '{}'", file.getPath(),
								        entry.getKey());
							} else {
								throw new IOException("Cannot create placeholder resource: " + file);
							}
						} else {
							throw new IOException("Cannot create placeholder resource path: " + parentFile);
						}
						entry.setValue(file.getCanonicalFile());
					} catch (IOException e) {
						logger.error("When creating placeholder resource", e);
					}
				}
			}
		}
	}

	public void destroyPlaceholderResources() {
		if (placeholderSourceDir != null) {
			deleteDirectory(placeholderSourceDir);
		}
	}

	/**
	 * Resolves the file for a path which need to be:
	 * <ul>
	 * <li>a resource referenced into a TUSAR report for this project
	 * (sub-projects not included).</li>
	 * <li>a file located into one of the source directory for this project
	 * (sub-projects not included).</li>
	 * </ul>
	 */
	public File getCanonicalFile(String referencedFilePath) {
		return pathToExistingCanonicalFile.get(referencedFilePath);
	}

	/**
	 * @return The TUSAR reports for this project, sub-projects reports not
	 *         included.
	 */
	public Collection<? extends Report> getReports() {
		return reports;
	}

	private Collection<Report> loadReports(File baseDir, String[] reportsPaths) {
		LinkedList<Report> reports = new LinkedList<Report>();
		for (File xmlReport : getXmlReports(getReportDirs(baseDir, reportsPaths))) {
			logger.info("For project '{}', found TUSAR report: '{}'", key, xmlReport);
			try {
				URL url = xmlReport.toURI().toURL();
				Tusar tusar = Convertor.getInstance().upgradeToLastVersionModel(url);
				reports.add(new Report(xmlReport, tusar));
			} catch (MalformedURLException e) {
				throw new ConversionException(e);
			}
		}
		return reports;
	}

	private static List<File> getXmlReports(List<? extends File> reportsDirs) {
		List<File> allXmlReports = new ArrayList<File>();
		for (File reportDir : reportsDirs) {
			File[] xmlReports = reportDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
			allXmlReports.addAll(Arrays.asList(xmlReports));
		}
		return allXmlReports;
	}

	private static List<? extends File> getReportDirs(File baseDir, String[] reportsPaths) {
		List<File> reportsDirs = new LinkedList<File>();
		for (String reportsPath : reportsPaths) {
			File reportDir = getAbsolutePath(baseDir, reportsPath);
			if (reportDir.exists() && reportDir.isDirectory()) {
				reportsDirs.add(reportDir);
			} else {
				logger.warn("Report directory '{}' doesn't exist and will be ignored.", reportDir.getAbsolutePath());
			}
		}
		return reportsDirs;
	}

	/**
	 * If not absolute, the path is interpreted as relative to the base dir.
	 */
	private static File getAbsolutePath(File baseDir, String resourcePath) {
		File path = new File(resourcePath);
		if (path.isAbsolute()) {
			return path;
		} else {
			return new File(baseDir, resourcePath);
		}
	}

	private static boolean deleteDirectory(File path) {
		if (path.exists()) {
			for (File file : path.listFiles()) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		return path.delete();
	}

	private static String getPrefix(ProjectDefinition projectDefinition) {
		StringBuilder prefix = new StringBuilder();
		ProjectDefinition definition = projectDefinition;
		ProjectDefinition parentDefinition;
		while ((parentDefinition = definition.getParent()) != null) {
			prefix.insert(0, '.');
			prefix.insert(0, definition.getKey().substring(parentDefinition.getKey().length() + 1));
			definition = parentDefinition;
		}
		return prefix.toString();
	}

	private static String[] splitAgain(String[] multiValues) {
		List<String> allValues = new LinkedList<String>();
		if (multiValues != null) {
			for (String values : multiValues) {
				for (String value : values.split(";")) {
					allValues.add(value);
				}
			}
		}
		return allValues.toArray(new String[0]);
	}

	private static boolean same(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		} else {
			return o1.equals(o2);
		}
	}
}

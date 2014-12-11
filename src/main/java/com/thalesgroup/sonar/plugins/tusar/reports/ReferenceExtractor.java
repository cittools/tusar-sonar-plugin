package com.thalesgroup.sonar.plugins.tusar.reports;

import java.util.Collection;

import com.thalesgroup.tusar.v12.Tusar;

public interface ReferenceExtractor {

	String PROJECT_RESSOURCE_TYPE = "Project";

	String DIR_RESSOURCE_TYPE = "Dir";

	String FILE_RESSOURCE_TYPE = "File";

	/**
	 * Could be absolute or relative to a configured source directory (not the
	 * project's base directory itself). Shall also exist to be taken into
	 * account.
	 */
	Collection<? extends String> getReferencedResourcePaths(Tusar model);
}

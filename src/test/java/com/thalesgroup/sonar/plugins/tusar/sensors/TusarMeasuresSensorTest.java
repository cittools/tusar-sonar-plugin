package com.thalesgroup.sonar.plugins.tusar.sensors;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import com.thalesgroup.sonar.plugins.tusar.reports.ReportExtractor;

@SuppressWarnings("deprecation")
public class TusarMeasuresSensorTest {

	private static class SensorContextStub implements SensorContext {

		@Override
		public Event createEvent(Resource arg0, String arg1, String arg2, String arg3, Date arg4) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteEvent(Event arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void deleteLink(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Resource> getChildren(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Dependency> getDependencies() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Event> getEvents(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Dependency> getIncomingDependencies(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure getMeasure(Metric arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure getMeasure(Resource arg0, Metric arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <M> M getMeasures(MeasuresFilter<M> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <M> M getMeasures(Resource arg0, MeasuresFilter<M> arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Dependency> getOutgoingDependencies(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Resource getParent(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <R extends Resource> R getResource(R arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean index(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean index(Resource arg0, Resource arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isExcluded(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isIndexed(Resource arg0, boolean arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dependency saveDependency(Dependency arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveLink(ProjectLink arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(Measure arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(Metric arg0, Double arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(Resource arg0, Measure arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(InputFile arg0, Measure arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(Resource arg0, Metric arg1, Double arg2) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Measure saveMeasure(InputFile arg0, Metric arg1, Double arg2) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String saveResource(Resource arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveSource(Resource arg0, String arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveViolation(Violation arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveViolation(Violation arg0, boolean arg1) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveViolations(Collection<Violation> arg0) {
			throw new UnsupportedOperationException();
		}
	}

	private static class MetricFinderStub implements MetricFinder {

		@Override
		public Metric findByKey(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Metric findById(int arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Metric> findAll(List<String> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Metric> findAll() {
			throw new UnsupportedOperationException();
		}
	};

	@Before
	public void setUp() {
		ProjectDefinition projectDefinition = ProjectDefinition.create();
		projectDefinition.setKey("test");
		Settings batchSettings = new Settings();
		ReportExtractor.createRootInstance(projectDefinition, batchSettings);
	}

	@Test
	public void testAnalyse() {
		Settings settings = new Settings();
		MetricFinder finder = new MetricFinderStub();
		TusarMeasuresSensor sensor = new TusarMeasuresSensor(settings, finder);
		SensorContextStub context = new SensorContextStub();
		sensor.analyse(new Project("test"), context);
	}
}

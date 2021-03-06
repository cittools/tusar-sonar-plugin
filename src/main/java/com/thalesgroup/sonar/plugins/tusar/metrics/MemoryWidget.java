package com.thalesgroup.sonar.plugins.tusar.metrics;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.Description;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.WidgetCategory;

@NavigationSection(NavigationSection.RESOURCE)
@UserRole(UserRole.USER)
@WidgetCategory("memory_widget")
@Description("Memory Errors")
public class MemoryWidget extends AbstractRubyTemplate implements RubyRailsWidget {

	@Override
	public String getId() {
		return "memory_widget";
	}

	@Override
	public String getTitle() {
		return "Memory";
	}

	@Override
	protected String getTemplatePath() {
		return "/com/thalesgroup/sonar/plugins/tusar/memory_widget.html.erb";
	}
}

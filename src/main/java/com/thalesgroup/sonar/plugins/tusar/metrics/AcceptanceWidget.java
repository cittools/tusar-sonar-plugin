package com.thalesgroup.sonar.plugins.tusar.metrics;

import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE)
@UserRole(UserRole.USER)
public class AcceptanceWidget extends AbstractRubyTemplate implements RubyRailsWidget {

	@Override
	public String getId() {
		return "acceptance_widget";
	}

	@Override
	public String getTitle() {
		return "Acceptance Tests";
	}

	@Override
	public String getTemplatePath() {
		return "/com/thalesgroup/sonar/plugins/tusar/acceptance_widget.html.erb";
	}
}

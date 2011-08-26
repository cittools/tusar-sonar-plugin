/*******************************************************************************
* Copyright (c) 2010 Thales Corporate Services SAS                             *
*                                                                              *
* Permission is hereby granted, free of charge, to any person obtaining a copy *
* of this software and associated documentation files (the "Software"), to deal*
* in the Software without restriction, including without limitation the rights *
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
* copies of the Software, and to permit persons to whom the Software is        *
* furnished to do so, subject to the following conditions:                     *
*                                                                              *
* The above copyright notice and this permission notice shall be included in   *
* all copies or substantial portions of the Software.                          *
*                                                                              *
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
* THE SOFTWARE.                                                                *
*******************************************************************************/ 

package com.thalesgroup.sonar.plugins.tusar.rulesrepository;

import org.sonar.api.rules.RulePriority;


public class TUSARRulePriorityMapper {
	
	public static RulePriority from(String priority) {
		if (priority.equalsIgnoreCase("CRITICAL")) {
			return RulePriority.CRITICAL;
		}
		if (priority.equalsIgnoreCase("MAJOR")) {
			return RulePriority.MAJOR;
		}
		if (priority.equalsIgnoreCase("MINOR")) {
			return RulePriority.MINOR;
		}
		if (priority.equalsIgnoreCase("INFO")) {
			return RulePriority.INFO;
		}
		throw new IllegalArgumentException("Priority not supported: " + priority);
	}

	public static String to(RulePriority priority) {
		if (priority.equals(RulePriority.BLOCKER) || priority.equals(RulePriority.CRITICAL)) {
			return "CRITICAL";
		}
		if (priority.equals(RulePriority.MAJOR)) {
			return "MAJOR";
		}
		if (priority.equals(RulePriority.MINOR)) {
			return "MINOR";
		}
		if (priority.equals(RulePriority.INFO)) {
			return "INFO";
		}
		throw new IllegalArgumentException("Priority not supported: " + priority);
	}
}

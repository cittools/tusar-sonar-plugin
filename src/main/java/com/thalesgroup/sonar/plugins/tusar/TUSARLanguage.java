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

package com.thalesgroup.sonar.plugins.tusar;

import org.sonar.api.resources.AbstractLanguage;

/**
 * This class describes the language used by the plugin.
 */
public class TUSARLanguage extends AbstractLanguage {

	public static final TUSARLanguage INSTANCE = new TUSARLanguage();

	public static final String KEY = "tusar";
	public static final String NAME = "TUSAR format - All languages";

	// TODO GBO : fix the SUFFIXES as the language is a "generic" language
	// public static final String[] SUFFIXES = {"java", "php", "c", "cpp",
	// "cxx", "h", "hxx", "ads", "adb"};

	public TUSARLanguage() {
		super(KEY, NAME);
	}

	@Override
	public String[] getFileSuffixes() {
		return new String[0];
		// return SUFFIXES;
	}
}

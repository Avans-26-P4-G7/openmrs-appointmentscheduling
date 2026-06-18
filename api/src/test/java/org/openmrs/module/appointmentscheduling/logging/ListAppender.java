/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.appointmentscheduling.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal log4j appender used in tests to capture log messages in memory so
 * their content can be asserted against (e.g. NEN-7510 8.15 logging fields).
 */
public class ListAppender extends AppenderSkeleton {

	private final List<LoggingEvent> events = new ArrayList<LoggingEvent>();

	@Override
	protected void append(LoggingEvent event) {
		events.add(event);
	}

	public List<String> getMessages() {
		List<String> messages = new ArrayList<String>();
		for (LoggingEvent event : events) {
			messages.add(String.valueOf(event.getMessage()));
		}
		return messages;
	}

	public void clear() {
		events.clear();
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}
}

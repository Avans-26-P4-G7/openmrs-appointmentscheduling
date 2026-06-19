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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.appointmentscheduling.Appointment.AppointmentStatus;
import org.openmrs.module.appointmentscheduling.AppointmentType;
import org.openmrs.module.appointmentscheduling.TimeSlot;
import org.openmrs.module.appointmentscheduling.api.AppointmentService;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NEN-7510 8.15 compliance tests for logging of appointment CRUD operations.
 * <p>
 * Per a single log line is expected to contain:
 * - User-ID (Context.getAuthenticatedUser().getUserId())
 * - Timestamp in ISO 8601 format, e.g. 2026-06-15T10:30:00+02:00
 * - Event type: CREATE, UPDATE, DELETE, READ, LOGIN_SUCCESS, LOGIN_FAILURE, ACCESS_DENIED
 * - Outcome: SUCCESS or FAILURE
 * - Resource UUID of the appointment/patient (never a BSN or medical content)
 */
public class AppointmentLoggingTest extends BaseModuleContextSensitiveTest {

	private static final Pattern ISO_8601_TIMESTAMP = Pattern
			.compile(".*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.,]\\d+)?(Z|[+-]\\d{2}:?\\d{2}).*");

	private static final Pattern BSN_LIKE = Pattern.compile(".*\\b\\d{9}\\b.*");

	private AppointmentService service;

	private ListAppender appender;

	private Logger appointmentServiceLogger;

	@Before
	public void before() throws Exception {
		service = Context.getService(AppointmentService.class);
		executeDataSet("standardAppointmentTestDataset.xml");

		appender = new ListAppender();
		// the audit trail is expected to be emitted through the standard
		// commons-logging Log of AppointmentServiceImpl, which log4j backs
		appointmentServiceLogger = Logger
				.getLogger("org.openmrs.module.appointmentscheduling.api.impl.AppointmentServiceImpl");
		appointmentServiceLogger.addAppender(appender);
		appointmentServiceLogger.setLevel(Level.ALL);
	}

	@After
	public void after() throws Exception {
		appointmentServiceLogger.removeAppender(appender);
	}

	private void assertNenCompliantLogExists(String expectedEventType, String expectedUuid) {
		List<String> messages = appender.getMessages();
		assertFalse("Expected at least one log message to be written, but none were found. "
				+ "NEN-7510 8.15 requires a log entry for every CRUD operation.", messages.isEmpty());

		Integer expectedUserId = Context.getAuthenticatedUser().getUserId();

		for (String message : messages) {
			boolean hasEventType = message.contains(expectedEventType);
			boolean hasUuid = message.contains(expectedUuid);
			boolean hasUserId = message.contains(String.valueOf(expectedUserId));
			boolean hasTimestamp = ISO_8601_TIMESTAMP.matcher(message).matches();
			boolean hasOutcome = message.contains("SUCCESS") || message.contains("FAILURE");
			boolean hasBsnLikeNumber = BSN_LIKE.matcher(message).matches();

			if (hasEventType && hasUuid && hasUserId && hasTimestamp && hasOutcome) {
				assertFalse("Log message must not contain BSN-like content: " + message, hasBsnLikeNumber);
				return;
			}
		}

		fail("No log message satisfied all NEN-7510 8.15 fields (event type=" + expectedEventType
				+ ", resource UUID=" + expectedUuid + ", User-ID=" + expectedUserId
				+ ", ISO-8601 timestamp, outcome SUCCESS/FAILURE). Messages captured: " + messages);
	}

	/**
	 * Runs the given action and turns any unexpected exception into a clear JUnit
	 * failure that names the step that broke, instead of letting it surface as a
	 * raw stacktrace/test error with no context.
	 */
	private void runStep(String stepDescription, ThrowingRunnable action) {
		try {
			action.run();
		}
		catch (AssertionError e) {
			throw e;
		}
		catch (Exception e) {
			fail("Unexpected exception while " + stepDescription + ": " + e);
		}
	}

	private interface ThrowingRunnable {

		void run() throws Exception;
	}

	@Test
	public void saveAppointment_shouldLogCreateEvent() throws Exception {
		final Appointment[] savedHolder = new Appointment[1];

		runStep("creating and saving a new appointment", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				TimeSlot timeSlot = new TimeSlot();
				timeSlot.setStartDate(new java.util.Date());
				timeSlot.setEndDate(new java.util.Date());
				timeSlot.setAppointmentBlock(service.getAppointmentBlock(1));
				service.saveTimeSlot(timeSlot);

				AppointmentType appointmentType = service.getAppointmentType(1);
				Appointment appointment = new Appointment(timeSlot, null, new org.openmrs.Patient(1), appointmentType,
						AppointmentStatus.SCHEDULED);

				savedHolder[0] = service.saveAppointment(appointment);
			}
		});

		assertNenCompliantLogExists("CREATE", savedHolder[0].getUuid());
	}

	@Test
	public void saveAppointment_shouldLogUpdateEvent() throws Exception {
		final Appointment appointment = service.getAppointment(1);

		runStep("updating an existing appointment", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				appointment.setReason("Updated reason for NEN-7510 logging test");
				service.saveAppointment(appointment);
			}
		});

		assertNenCompliantLogExists("UPDATE", appointment.getUuid());
	}

	@Test
	public void voidAppointment_shouldLogCancelEvent() throws Exception {
		final Appointment appointment = service.getAppointment(1);

		runStep("voiding/cancelling an appointment", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				service.voidAppointment(appointment, "Patient cancelled");
			}
		});

		assertNenCompliantLogExists("CANCEL", appointment.getUuid());
	}

	@Test
	public void purgeAppointment_shouldLogDeleteEvent() throws Exception {
		final Appointment appointment = service.getAppointment(1);
		final String uuid = appointment.getUuid();

		runStep("purging/deleting an appointment", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				service.purgeAppointment(appointment);
			}
		});

		assertNenCompliantLogExists("DELETE", uuid);
	}

	@Test
	public void getAppointment_shouldLogReadEvent() throws Exception {
		final Appointment[] readHolder = new Appointment[1];

		runStep("reading an appointment by id", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				readHolder[0] = service.getAppointment(1);
			}
		});

		assertNenCompliantLogExists("READ", readHolder[0].getUuid());
	}

	@Test
	public void getAppointmentByUuid_shouldLogReadEvent() throws Exception {
		final Appointment[] readHolder = new Appointment[1];

		runStep("reading an appointment by uuid", new ThrowingRunnable() {

			@Override
			public void run() throws Exception {
				readHolder[0] = service.getAppointmentByUuid("c0c579b0-8e59-401d-8a4a-976a0b183601");
			}
		});

		assertNenCompliantLogExists("READ", readHolder[0].getUuid());
	}

	@Test
	public void unauthorizedAccess_shouldLogAccessDeniedEvent() throws Exception {
		try {
			runStep("logging out and calling a privileged method without authentication", new ThrowingRunnable() {

				@Override
				public void run() throws Exception {
					Context.logout();

					try {
						Context.getService(AppointmentService.class).getAllAppointments();
						fail("Expected an authentication/authorization exception when calling the service while logged out");
					}
					catch (APIAuthenticationException e) {
						// expected: access should be denied
					}
				}
			});

			List<String> messages = appender.getMessages();
			boolean foundAccessDeniedLog = false;
			for (String message : messages) {
				if (message.contains("ACCESS_DENIED") && (message.contains("FAILURE"))
						&& ISO_8601_TIMESTAMP.matcher(message).matches()) {
					foundAccessDeniedLog = true;
					break;
				}
			}

			assertTrue("Expected an ACCESS_DENIED log entry with ISO-8601 timestamp and outcome FAILURE "
					+ "when an unauthenticated/unauthorized call is rejected. Messages captured: " + messages,
					foundAccessDeniedLog);
		}
		finally {
			// restore an authenticated context for other tests / teardown,
			// even if an unexpected exception happened above
			try {
				authenticate();
			}
			catch (Exception e) {
				fail("Failed to restore an authenticated context after the access-denied test: " + e);
			}
		}
	}
}

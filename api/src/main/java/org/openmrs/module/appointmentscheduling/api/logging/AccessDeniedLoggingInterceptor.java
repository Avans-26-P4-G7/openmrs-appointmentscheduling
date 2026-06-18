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
package org.openmrs.module.appointmentscheduling.api.logging;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;

/**
 * Wraps AppointmentService method calls and writes a NEN-7510 8.15 compliant
 * ACCESS_DENIED log entry whenever the underlying authorization check
 * (AuthorizationAdvice, further down the interceptor chain) rejects a call.
 * <p>
 * Must be registered in preInterceptors BEFORE serviceInterceptors so that
 * this around-advice's try/catch can observe the APIAuthenticationException
 * thrown by AuthorizationAdvice's before-advice.
 */
public class AccessDeniedLoggingInterceptor implements MethodInterceptor {

	// logged under AppointmentServiceImpl's category so all NEN-7510 log
	// entries for this service (CRUD + access-denied) live in one log stream
	protected final Log log = LogFactory
			.getLog("org.openmrs.module.appointmentscheduling.api.impl.AppointmentServiceImpl");

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		}
		catch (APIAuthenticationException e) {
			writeAccessDeniedLog(invocation);
			throw e;
		}
	}

	private void writeAccessDeniedLog(MethodInvocation invocation) {
		String userId = Context.isAuthenticated() ? String.valueOf(Context.getAuthenticatedUser().getUserId())
				: "anonymous";

		log.info("ACCESS_DENIED User-ID=" + userId
				+ " Timestamp=" + new DateTime().toString()
				+ " Outcome=FAILURE Endpoint=" + invocation.getMethod().getName());
	}
}

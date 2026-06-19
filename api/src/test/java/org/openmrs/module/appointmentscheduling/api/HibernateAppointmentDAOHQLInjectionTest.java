package org.openmrs.module.appointmentscheduling.api.db.hibernate;

import org.hibernate.Query;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.appointmentscheduling.Appointment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

public class HibernateAppointmentDAOHQLInjectionTest {

    private HibernateAppointmentDAO hibernateAppointmentDAO;

    @Mock
    private DbSessionFactory sessionFactory;

    @Mock
    private DbSession session;

    @Mock
    private Query query;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        hibernateAppointmentDAO = new HibernateAppointmentDAO();
        hibernateAppointmentDAO.sessionFactory = sessionFactory;
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    public void testSearchWithValidPatientNameReturnsResults() {
        List<Appointment> mockResults = new ArrayList<Appointment>();
        when(query.list()).thenReturn(mockResults);

        List<?> results = hibernateAppointmentDAO.searchAppointmentsByPatientName("John");

        verify(session).createQuery(
                "from Appointment ap where ap.visit.patient.personName.givenName = :name or ap.visit.patient.personName.familyName = :name");
        verify(query).setParameter("name", "John");
        assertEquals(mockResults, results);
    }

    @Test
    public void testHQLInjectionWithQuoteEscape() {
        String injectedPayload = "' OR '1'='1";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(contains("OR '1'='1"));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testHQLInjectionModifiesQueryLogic() {
        String injectedPayload = "x' OR ap.visit.patient.id > 0 OR 'y'='y";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(contains("OR ap.visit.patient.id > 0"));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testHQLInjectionBypassesNameRestriction() {
        String injectedPayload = "x' OR 1=1 OR 'x'='x";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(contains("1=1"));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testHQLInjectionWithCommentBypass() {
        String injectedPayload = "x' OR 'a'='a' -- ";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(contains("-- "));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testCaptureVulnerableQueryString() {
        final String injectedPayload = "admin' OR '1'='1";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String hql = (String) argument;
                return hql.contains(injectedPayload);
            }
        }));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testMultipleInjectionPointsVulnerable() {
        final String injectedPayload = "x' OR familyName='";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String hql = (String) argument;
                return hql.contains(injectedPayload);
            }
        }));
        verify(query).setParameter("name", injectedPayload);
    }

    @Test
    public void testSpecialCharactersInInjection() {
        String injectedPayload = "' UNION SELECT * FROM provider WHERE '1'='1";
        when(query.list()).thenReturn(new ArrayList<Appointment>());

        hibernateAppointmentDAO.searchAppointmentsByPatientName(injectedPayload);

        verify(session, never()).createQuery(contains("UNION"));
        verify(query).setParameter("name", injectedPayload);
    }
}
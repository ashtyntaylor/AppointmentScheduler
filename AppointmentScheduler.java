import com.google.gson.Gson;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.*;

public class AppointmentScheduler {
  private static final String URL = "http://scheduling-interview-2021-265534043.us-west-2.elb.amazonaws.com/api/";
  private static final String AUTH_TOKEN = "?token=a5cd589d-59ff-4196-9438-f162e195d7e0";

  public static void main(String[] args) {
    try {
      // Start the test system
      System.out.println("Starting run");
      HttpHelpers.doPost(URL + "Scheduling/Start" + AUTH_TOKEN, null);

      // Retrieve the current appointment list
      System.out.println("Get initial appointments");
      List<AppointmentInfo> appointments = new ArrayList<>();
      appointments = getCurrentAppointments();
      System.out.println(appointments);

      AppointmentRequest appointmentRequest = null;

      // Process appointment requests
      while (true) {

        // Retrieve new appointment request from the queue
        System.out.println("Get appointment from queue");
        appointmentRequest = getNewAppointmentRequest();

        if (appointmentRequest == null) {
          // We've processed all requests
          break;
        }

        // Find availability
        AppointmentInfoRequest newAppointment = findAvailability(appointmentRequest, appointments);

        // Schedule the new appointment where available
        if (newAppointment != null) {
          boolean isSuccessful = bookAppointment(newAppointment);
          if (isSuccessful) {
            System.out.println("Booked successfully");
            AppointmentInfo addedAppointment = new AppointmentInfo(newAppointment.getDoctorId(), newAppointment.getPersonId(), newAppointment.getAppointmentTime(), newAppointment.isNewPatientAppointment());
            appointments.add(addedAppointment);
            System.out.println("Added appointment to local list");
          }
        }
        // No availability, print warning
        else {
          System.out.println("No available times/doctors for appointment request");
        }
      }

      System.out.println("All requests have been processed!");

      // Sanity check
      String response = HttpHelpers.doPost(URL + "Scheduling/Stop" + AUTH_TOKEN, null);
      List<AppointmentInfo> finalSchedule = createAppointmentList(response);

      for (int i = 0; i < finalSchedule.size(); i++) {
        if (!finalSchedule.get(i).equals(appointments.get(i))) {
          System.out.println("Found a discrepancy");
          System.out.println(finalSchedule.get(i));
          System.out.println(appointments.get(i));
          System.exit(-1);
        }
      }

      System.out.println("Appointment schedule validated with the server successfully");

    } catch (IOException | ParseException | InvalidTokenException | SchedulerException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  private static List<AppointmentInfo> getCurrentAppointments() throws IOException, ParseException, InvalidTokenException, SchedulerException {
    // Get list
    String response = HttpHelpers.doGet(URL + "Scheduling/Schedule" + AUTH_TOKEN);

    // Convert to Appointment object and save locally
    List<AppointmentInfo> appointmentsList = createAppointmentList(response);

    return appointmentsList;
  }

  private static AppointmentRequest getNewAppointmentRequest() throws InvalidTokenException, SchedulerException, IOException, ParseException {
    // Get next request
    String response = HttpHelpers.doGet(URL + "Scheduling/AppointmentRequest" + AUTH_TOKEN);

    // Check if we're done
    if (response == null) {
      return null;
    }

    // Convert to Appointment object and save locally
    AppointmentRequest appointmentRequest = createAppointmentRequest(response);

    return appointmentRequest;
  }

  private static AppointmentInfoRequest findAvailability(AppointmentRequest appointmentRequest, List<AppointmentInfo> currentAppointments) {
    System.out.println("Checking availability for request " + appointmentRequest.toString());

    // Check combinations of day, time, and doctor
    for (String day : appointmentRequest.getPreferredDays()) {

      // See if day is available
      if (isDayAvailable(day, appointmentRequest.getPersonId(), currentAppointments)) {

        // Look through valid times
        int startHour = appointmentRequest.isNew() ? 15 : 8;

        for (int hour = startHour; hour <= 16; hour++) {

          // See if a doctor is free during that time
          for (int doctor: appointmentRequest.getPreferredDocs()) {
            System.out.println("Checking if doctor " + doctor + " is available at " + hour);

            StringBuilder requestTime = new StringBuilder(day);
            if(hour > 9){
              requestTime.setCharAt(11, '1');
            }
            requestTime.setCharAt(12, (char) ('0' + (hour % 10)));

            if (isDoctorAvailable(doctor, requestTime.toString(), currentAppointments)) {
              System.out.println("Found available time and doctor");

              // Return new appointment to book
              return new AppointmentInfoRequest(doctor, appointmentRequest.getPersonId(), requestTime.toString(), appointmentRequest.isNew(), appointmentRequest.getRequestId());
            }
          }
        }
      }
    }

    return null;
  }

  private static Boolean isDayAvailable(String requestDay, int requestPersonId, List<AppointmentInfo> currentAppointments) {
    // Check time constraints
    ZonedDateTime requestDate = ZonedDateTime.parse(requestDay);
    System.out.println("Checking day " + requestDay);

    // Is appointment scheduled in 2021?
    if (requestDate.getYear() != 2021) {
      System.out.println("appointment not in 2021, in " + requestDate.getYear());
      return false;
    }

    // Is appointment scheduled in Nov or Dec?
    if (requestDate.getMonth() != Month.NOVEMBER && requestDate.getMonth() != Month.DECEMBER) {
      System.out.println("appointment not in Nov or Dec, in " + requestDate.getMonth());
      return false;
    }

    // Is appointment scheduled on a weekday?
    if (requestDate.getDayOfWeek() == DayOfWeek.SATURDAY || requestDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
      System.out.println("appointment not on weekday, on " + requestDate.getDayOfWeek());
      return false;
    }

    // Does patient already have an appointment this week?
    for (AppointmentInfo existingAppointment : currentAppointments) {

      // Found existing appointment booked by same person
      if (requestPersonId == existingAppointment.getPersonId()) {

        // Is the existing appointment in the same week as the requested appointment?
        ZonedDateTime existingDate = ZonedDateTime.parse(existingAppointment.getAppointmentTime());

        Calendar windowStart = Calendar.getInstance();
        windowStart.setTime(Date.from(existingDate.toInstant()));
        windowStart.add(Calendar.DATE, -6);

        Calendar windowEnd = Calendar.getInstance();
        windowEnd.setTime(Date.from(existingDate.toInstant()));
        windowEnd.add(Calendar.DATE, 6);

        if(Date.from(requestDate.toInstant()).after(windowStart.getTime()) && Date.from(requestDate.toInstant()).before(windowEnd.getTime())) {
          // Within a week
          System.out.println("appointment already scheduled for person " + requestPersonId + " this same week");
          return false;
        }
      }
    }

    return true;
  }

  private static Boolean isDoctorAvailable(int doctor, String requestTime, List<AppointmentInfo> currentAppointments) {
    // Check doctor constraints

    // Is doctor already booked at this time?
    for (AppointmentInfo existingAppointment : currentAppointments) {

      // Found existing appointment booked for same doctor
      if (doctor == existingAppointment.getDoctorId()) {

        // Is the existing appointment at the same time as the requested appointment?
        if (requestTime.equals(existingAppointment.getAppointmentTime())) {
          return false;
        }
      }
    }

    return true;
  }

  private static Boolean bookAppointment(AppointmentInfoRequest newAppointment) throws IOException, ParseException, InvalidTokenException, SchedulerException {
    Gson gson = new Gson();
    String newAppointmentRequestJson = gson.toJson(newAppointment);

    try {
      HttpHelpers.doPost(URL + "Scheduling/Schedule" + AUTH_TOKEN, newAppointmentRequestJson);
      return true;

    } catch (SchedulerException e){
      System.out.println(e.getMessage());
      return false;
    }
  }

  private static List<AppointmentInfo> createAppointmentList(String jsonString) {
    Gson gson = new Gson();

    // Parse json string to convert to AppointmentInfo list
    AppointmentInfo[] appointments = gson.fromJson(jsonString, AppointmentInfo[].class);
    List<AppointmentInfo> appointmentsList = new ArrayList<>(Arrays.asList(appointments));

    return appointmentsList;
  }

  private static AppointmentRequest createAppointmentRequest(String jsonString) {
    Gson gson = new Gson();

    // Parse json string to convert to AppointmentRequest
    AppointmentRequest appointmentRequest = gson.fromJson(jsonString, AppointmentRequest.class);

    return appointmentRequest;
  }
}
/**
 * 
 */
package com.intransit.merchant.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intransit.email.MailMail;
import com.intransit.merchant.api.dto.AirportDto;
import com.intransit.merchant.api.dto.FlightXmlPush;
import com.intransit.merchant.api.dto.LocationDto;
import com.intransit.merchant.api.dto.LocationRequestDto;
import com.intransit.merchant.api.dto.TripDto;
import com.intransit.merchant.api.flighxml.FlightXmlService;
import com.intransit.merchant.api.flighxml.LocalFlightXmlStub;
import com.intransit.merchant.api.webservice.BaseEndpoint;
import com.intransit.merchant.api.webservice.UserWrapper;
import com.intransit.merchant.helper.IntransitUtils;
import com.intransit.merchant.model.Airport;
import com.intransit.merchant.model.LogData;
import com.intransit.merchant.model.LoggingTrack;
import com.intransit.merchant.model.Terminal;
import com.intransit.merchant.model.TravelClass;
import com.intransit.merchant.model.TravelPurposeDemoGraphic;
import com.intransit.merchant.model.Trip;
import com.intransit.merchant.model.User;
import com.intransit.merchant.service.CampaignService;
import com.intransit.merchant.service.DemoGraphicsService;
import com.intransit.merchant.service.LocationService;
import com.intransit.merchant.service.PushNotificationService;
import com.intransit.merchant.service.TripService;
import com.intransit.merchant.service.UserService;
import com.intransit.merchant.status.TripFlightStatus;
import com.intransit.merchant.status.TripStatus;
import com.intransit.merchant.status.UserTripStatus;
import com.intransit.merchant.validator.UserValidator;

/**
 * @author kodelavg
 *
 */
@RestController
@RequestMapping("/api/trip")
public class UserTripController extends BaseEndpoint {

	@Resource
	protected TripService tripService;

	@Resource
	protected UserService userService;

	@Resource
	protected MailMail mail;

	@Resource
	protected UserWrapper userWrapper;

	@Resource
	protected LocationService locationService;

	@Resource
	protected DemoGraphicsService demoGraphicsService;

	@Autowired
	private UserValidator userValidator;

	@Resource
	protected FlightXmlService flightXmlService;

	@Resource
	protected CampaignService campaignService;

	@Resource
	protected PushNotificationService pushNotificationService;

	private static final Logger logger = LoggerFactory.getLogger(UserTripController.class);

	@RequestMapping(value = "/create", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTrips(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException,
			ParseException {
		JSONObject json = new JSONObject();
		List<TripDto> trips = trip.getTrips();
		String status = "";
		String message = "";
		for (TripDto tr : trips) {
			Trip findDuplicateTrip = tripService.findDuplicateTrip(tr.getUserid(), tr.getFlightNumber(), tr.getTravelDate());
			if (findDuplicateTrip == null) {
				Boolean saveTripDetails = tripService.saveTripDetails(tr);
				if (saveTripDetails == Boolean.FALSE) {
					status = "FAILURE";
					message = "Invalid Trip Details";
				} else {
					status = "SUCCESS";
					/* its for logging track details */

					Long userId = trip.getUserid();
					User userdata = userService.find(tr.getUserid());
					Date dateTime = new Date();
					String userSessionToken = userdata.getSessionToken();
					LoggingTrack logTrack = new LoggingTrack();
					logTrack.setUserId(userId);
					logTrack.setUserLogType("NOOFTRIPS");
					logTrack.setUpdateAt(dateTime);
					logTrack.setUserSessionType(userdata.getUserState());
					logTrack.setUserSessionToken(userSessionToken);
					userService.save(logTrack);

				}
			} else {
				status = "FAILURE";
				message = "You have already set up this trip";
				break;
			}
		}
		json.put("status", status);
		json.put("message", message);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/create", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripsV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException,
			ParseException {
		JSONObject json = new JSONObject();
		List<TripDto> trips = trip.getTrips();
		String status = "";
		String message = "";
		for (TripDto tr : trips) {
			Trip findDuplicateTrip = tripService.findDuplicateTrip(tr.getUserid(), tr.getFlightNumber(), tr.getTravelDate());
			if (findDuplicateTrip == null) {
				Boolean saveTripDetails = tripService.saveTripDetails(tr);
				if (saveTripDetails == Boolean.FALSE) {
					status = "FAILURE";
					message = "Invalid Trip Details";
				} else {
					status = "SUCCESS";
					/* its for logging track details */

					Long userId = trip.getUserid();
					User userdata = userService.find(tr.getUserid());
					Date dateTime = new Date();
					String userSessionToken = userdata.getSessionToken();
					LoggingTrack logTrack = new LoggingTrack();
					logTrack.setUserId(userId);
					logTrack.setUserLogType("NOOFTRIPS");
					logTrack.setUpdateAt(dateTime);
					logTrack.setUserSessionType(userdata.getUserState());
					logTrack.setUserSessionToken(userSessionToken);
					userService.save(logTrack);

				}
			} else {
				status = "FAILURE";
				message = "You have already set up this trip";
				break;
			}
		}
		json.put("status", status);
		json.put("message", message);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripsUpdate(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Trip trip1 = tripService.find(trip.getId());
		if (trip1 != null) {
			TripFlightStatus flightStatus = trip1.getFlightStatus();
			if (flightStatus != null && TripFlightStatus.IDLE.equals(flightStatus)) {
				tripService.updateTripDetails(trip, trip1);
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Cannot Update Already Started Trip ");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trip available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/update", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripsUpdateV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Trip trip1 = tripService.find(trip.getId());
		if (trip1 != null) {
			TripFlightStatus flightStatus = trip1.getFlightStatus();
			if (flightStatus != null && TripFlightStatus.IDLE.equals(flightStatus)) {
				tripService.updateTripDetails(trip, trip1);
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Cannot Update Already Started Trip ");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trip available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/reminder", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripReminder(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Trip trip1 = tripService.find(trip.getId());
		if (trip1 != null) {
			tripService.updateTripRemainder(trip1, trip.getReminder());
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trip available");
		}
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/reminder", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripReminderV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Trip trip1 = tripService.find(trip.getId());
		if (trip1 != null) {
			tripService.updateTripRemainder(trip1, trip.getReminder());
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trip available");
		}
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/alltrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripList(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.allTrips(trip.getUserid());
		User mobileUser = userService.find(trip.getUserid());
		if (tripsList != null) {
			/* its for logging track details */
			Date dateTime = new Date();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(trip.getUserid());
			logTrack.setUserLogType("ENTERTRIP");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionType(mobileUser.getUserState());
			logTrack.setUserSessionToken(mobileUser.getSessionToken());
			logTrack.setLatitude(trip.getLatitude());
			logTrack.setLongitude(trip.getLongitude());
			userService.save(logTrack);

			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got all trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/alltrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripListV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.allTrips(trip.getUserid());
		User mobileUser = userService.find(trip.getUserid());
		if (tripsList != null) {
			/* its for logging track details */
			Date dateTime = new Date();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(trip.getUserid());
			logTrack.setUserLogType("ENTERTRIP");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionType(mobileUser.getUserState());
			logTrack.setUserSessionToken(mobileUser.getSessionToken());
			logTrack.setLatitude(trip.getLatitude());
			logTrack.setLongitude(trip.getLongitude());
			userService.save(logTrack);

			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got all trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	public JSONArray getAlltrips(List<Trip> tripsList, JSONObject json) throws JSONException {
		TripDto dto = new TripDto(tripsList);
		List<TripDto> trips = dto.getTrips();
		Gson gson = new GsonBuilder().serializeNulls().create();
		String json2 = gson.toJson(trips, trips.getClass());
		JSONArray jsonObject = new JSONArray(json2);
		return jsonObject;
	}

	public JSONArray getAllUpcomingLocations(List<Trip> tripsList, JSONObject json) throws JSONException {
		LocationDto dto = new LocationDto(tripsList, Boolean.TRUE);
		List<LocationDto> locations = dto.getLocations();
		Gson gson = new GsonBuilder().serializeNulls().create();
		String json2 = gson.toJson(locations, locations.getClass());
		JSONArray jsonObject = new JSONArray(json2);
		return jsonObject;
	}

	@RequestMapping(value = "/pasttrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserPastTripList(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.pastTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got past trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/pasttrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserPastTripListV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.pastTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got past trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/upcomingtrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserUpcomingTripList(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.upcomingTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got upcoming trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/upcomingtrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserUpcomingTripListV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.upcomingTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("trips", getAlltrips(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got upcoming trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/details", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripDetails(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		Trip tripDb = tripService.find(trip.getId());
		// List<Trip> timeToFlight = tripService.findtimeToFlight(trip.getId());
		TripDto tripDto = null;
		String status = "";
		String message = "";
		if (tripDb != null) {
			tripDto = new TripDto(tripDb);
			status = "SUCCESS";
			Date date = new Date(tripDb.getDepatureTime() * 1000L);
			String period = IntransitUtils.getDifference(date);
			System.out.println(period);
			System.out.println(period);
			// System.out.println(PeriodFormat.wordBased(locale).print(period));
			tripDto.setTimeToFlight(period);

		} else {
			tripDto = new TripDto();
			status = "FAILURE";
			message = "No Trip Found";
		}
		tripDto.setStatus(status, message);

		Gson gson = new GsonBuilder().serializeNulls().create();
		String json2 = gson.toJson(tripDto, tripDto.getClass());
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json2);
		System.out.println(json2);
	}

	@RequestMapping(value = "/v1/details", method = RequestMethod.POST, produces = "application/json")
	public void apiUserTripDetailsV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		Trip tripDb = tripService.find(trip.getId());
		// List<Trip> timeToFlight = tripService.findtimeToFlight(trip.getId());
		TripDto tripDto = null;
		String status = "";
		String message = "";
		if (tripDb != null) {
			tripDto = new TripDto(tripDb);
			status = "SUCCESS";
			Date date = new Date(tripDb.getDepatureTime() * 1000L);
			String period = IntransitUtils.getDifference(date);
			System.out.println(period);
			System.out.println(period);
			// System.out.println(PeriodFormat.wordBased(locale).print(period));
			tripDto.setTimeToFlight(period);

		} else {
			tripDto = new TripDto();
			status = "FAILURE";
			message = "No Trip Found";
		}
		tripDto.setStatus(status, message);

		Gson gson = new GsonBuilder().serializeNulls().create();
		String json2 = gson.toJson(tripDto, tripDto.getClass());
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json2);
		System.out.println(json2);
	}

	@RequestMapping(value = "/usermadetrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserMadeTrips(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		BigInteger tripsList = tripService.madeTrips(trip.getUserid());
		if (!tripsList.equals(BigInteger.ZERO)) {
			json.put("status", "SUCCESS");
			json.put("tripsCount", "Your made trips are : " + tripsList);
			json.put("connections", "Your connections are : " + 3);
			json.put("deals", "Your deals are : " + 4);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are made by you");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/usermadetrips", method = RequestMethod.POST, produces = "application/json")
	public void apiUserMadeTripsV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		BigInteger tripsList = tripService.madeTrips(trip.getUserid());
		if (!tripsList.equals(BigInteger.ZERO)) {
			json.put("status", "SUCCESS");
			json.put("tripsCount", "Your made trips are : " + tripsList);
			json.put("connections", "Your connections are : " + 3);
			json.put("deals", "Your deals are : " + 4);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are made by you");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/cancel", method = RequestMethod.POST, produces = "application/json")
	@Transactional
	public void apiUserTripDelete(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println(trip.getId());
		Trip currentTrip = tripService.find(trip.getId());
		if (currentTrip != null) {
			// tripService.deleteTrip(trip.getId());
			currentTrip.setTripStatus(TripStatus.CANCELLED);
			tripService.update(currentTrip);
			json.put("status", "SUCCESS");
			json.put("message", "Your trip Deleted Successfully");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Matching trip is available to delete");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/cancel", method = RequestMethod.POST, produces = "application/json")
	@Transactional
	public void apiUserTripDeleteV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println(trip.getId());
		Trip currentTrip = tripService.find(trip.getId());
		if (currentTrip != null) {
			// tripService.deleteTrip(trip.getId());
			currentTrip.setTripStatus(TripStatus.CANCELLED);
			tripService.update(currentTrip);
			json.put("status", "SUCCESS");
			json.put("message", "Your trip Deleted Successfully");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Matching trip is available to delete");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/origins", method = RequestMethod.POST, produces = "application/json")
	public void getOriginAndDestination(@RequestBody TripDto tripDto, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		User userData = userService.find(tripDto.getUserid());
		JSONObject json = new JSONObject();
		Trip findDuplicateTrip = tripService.findDuplicateTrip(tripDto.getUserid(), tripDto.getFlightNumber(), tripDto.getTravelDate());
		if (findDuplicateTrip == null) {
			Map<String, List<LocationDto>> originAndDestinationBySchedules = flightXmlService.getOriginAndDestinationBySchedules(tripDto.getFlightNumber(),
					tripDto.getTravelDate());
			for (Entry<String, List<LocationDto>> entry : originAndDestinationBySchedules.entrySet()) {
				List<LocationDto> value = entry.getValue();
				if (CollectionUtils.isNotEmpty(value)) {
					Gson gson = new GsonBuilder().serializeNulls().create();
					String json2 = gson.toJson(value, value.getClass());
					JSONArray jsonObject = new JSONArray(json2);
					json.put(entry.getKey(), jsonObject);
					json.put("status", "SUCCESS");
					json.put("message", "");
				} else {
					LogData checkuser = tripService.checkExistedLogdata(tripDto.getFlightNumber());
					if (checkuser == null) {
						LogData log = new LogData();
						log.setCreateDate(new Date());
						log.setUserName(userData.getFirstName() + userData.getLastName());
						log.setUserEmail(userData.getEmail());
						log.setFlightNumber(tripDto.getFlightNumber());
						log.setTravelDate(tripDto.getTravelDate());
						log.setStatus("Flight Not Exist");
						Date travelDate = tripDto.getTravelDate();
						// Long startDate =
						// IntransitUtils.convertDateToEpoch(tripDto.getTravelDate());
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(travelDate);
						calendar.set(Calendar.HOUR_OF_DAY, 0);
						travelDate = calendar.getTime();
						calendar.add(Calendar.DATE, 1);
						Date time = calendar.getTime();
						Long startDate = IntransitUtils.convertDateToEpoch(travelDate);
						Long endDate = IntransitUtils.convertDateToEpoch(time);
						log.setStartDate(startDate);
						log.setEndDate(endDate);
						tripService.save(log);
					}
					json.put("status", "FAILURE");
					json.put("message", "No Flight Info Found");
				}
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "You have already set up this trip");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/origins", method = RequestMethod.POST, produces = "application/json")
	public void getOriginAndDestinationV1(@RequestBody TripDto tripDto, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		User userData = userService.find(tripDto.getUserid());
		JSONObject json = new JSONObject();
		Trip findDuplicateTrip = tripService.findDuplicateTrip(tripDto.getUserid(), tripDto.getFlightNumber(), tripDto.getTravelDate());
		if (findDuplicateTrip == null) {
			Map<String, List<LocationDto>> originAndDestinationBySchedules = flightXmlService.getOriginAndDestinationBySchedules(tripDto.getFlightNumber(),
					tripDto.getTravelDate());
			for (Entry<String, List<LocationDto>> entry : originAndDestinationBySchedules.entrySet()) {
				List<LocationDto> value = entry.getValue();
				if (CollectionUtils.isNotEmpty(value)) {
					Gson gson = new GsonBuilder().serializeNulls().create();
					String json2 = gson.toJson(value, value.getClass());
					JSONArray jsonObject = new JSONArray(json2);
					json.put(entry.getKey(), jsonObject);
					json.put("status", "SUCCESS");
					json.put("message", "");
				} else {
					LogData checkuser = tripService.checkExistedLogdata(tripDto.getFlightNumber());
					if (checkuser == null) {
						LogData log = new LogData();
						log.setCreateDate(new Date());
						log.setUserName(userData.getFirstName() + userData.getLastName());
						log.setUserEmail(userData.getEmail());
						log.setFlightNumber(tripDto.getFlightNumber());
						log.setTravelDate(tripDto.getTravelDate());
						log.setStatus("Flight Not Exist");
						Date travelDate = tripDto.getTravelDate();
						// Long startDate =
						// IntransitUtils.convertDateToEpoch(tripDto.getTravelDate());
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(travelDate);
						calendar.set(Calendar.HOUR_OF_DAY, 0);
						travelDate = calendar.getTime();
						calendar.add(Calendar.DATE, 1);
						Date time = calendar.getTime();
						Long startDate = IntransitUtils.convertDateToEpoch(travelDate);
						Long endDate = IntransitUtils.convertDateToEpoch(time);
						log.setStartDate(startDate);
						log.setEndDate(endDate);
						tripService.save(log);
					}
					json.put("status", "FAILURE");
					json.put("message", "No Flight Info Found");
				}
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "You have already set up this trip");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/upcomingtrip/locations", method = RequestMethod.POST, produces = "application/json")
	public void apiUserUpcomingTripLocations(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.upcomingTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("locations", getAllUpcomingLocations(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got upcoming trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/upcomingtrip/locations", method = RequestMethod.POST, produces = "application/json")
	public void apiUserUpcomingTripLocationsV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		List<Trip> tripsList = tripService.upcomingTrips(trip.getUserid());
		if (tripsList != null) {
			json.put("locations", getAllUpcomingLocations(tripsList, json));
			json.put("status", "SUCCESS");
			json.put("message", "You got upcoming trips data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No trips are available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/push/notifications", method = RequestMethod.POST, produces = "application/json")
	public void flighxmlPushNotifications(@RequestBody FlightXmlPush flightXmlPush, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		flightXmlService.saveFlightXmlPushDetails(flightXmlPush);

		List<Trip> tripsWithFlightid = tripService.getFlightidTrips();
		for (Trip trip : tripsWithFlightid) {
			if (trip.getFaFlightId().equals(flightXmlPush.getFlight().getFaFlightID())) {
				if (trip.getUser().getDeviceToken() != null && trip.getUser().getSessionToken() != null) {
					pushNotificationService.sendPushMessageForMobile(trip.getUser().getDeviceToken(), flightXmlPush.getLong_desc(), trip.getUser()
							.getDeviceType(), "Trip");
				}
			}
		}

		// TODO
		// FIND THE TRIPS WHICH HAS THE faFlightID FROM flightXmlPush
		// AND FIND ALL USERS OF THOSE TRIP
		// AND SEND THEM NOTIFICATIONS BY THEIR DEVICETOKEN IN DB WITH MESSAGE
		// AS long_desc from flightXmlPush
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/register", method = RequestMethod.GET, produces = "application/json")
	public void flighxmlRegister(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		flightXmlService.registerFlightXml();
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/alerts/all/destinations", method = RequestMethod.GET, produces = "application/json")
	public void flighxmlAlertsAllDestinations(@RequestParam("startDate") Integer startDate, @RequestParam("endDate") Integer endDate,
			HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		tripService.setAlertsForAllAirports(startDate, endDate);
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/get/alerts/all", method = RequestMethod.GET, produces = "application/json")
	public void flighxmlGetAllAlerts(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		flightXmlService.getPushAlerts();
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/delete/alerts/all", method = RequestMethod.GET, produces = "application/json")
	public void flighxmlDeleteAllAlerts(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		// flightXmlService.getAndDeletePushAlerts();
		flightXmlService.getAndDeletePushAlertsByRest();
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/flightxml/delete/alert", method = RequestMethod.GET, produces = "application/json")
	public void flighxmlDeleteAllAlerts(@RequestParam("alert_id") Integer alertId, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		flightXmlService.deletePushAlerts(LocalFlightXmlStub.getInstance(), alertId);
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/get/campaigns", method = RequestMethod.POST, produces = "application/json")
	public void getCampaignAds(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Long> matchedLiveCampaignForUser = campaignService.getMatchedLiveCampaignForUser(trip.getUserid());
		if (CollectionUtils.isNotEmpty(matchedLiveCampaignForUser)) {
			System.out.println("size=====" + matchedLiveCampaignForUser.size());
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(matchedLiveCampaignForUser, matchedLiveCampaignForUser.getClass());
			JSONArray jsonObject = new JSONArray(json2);
			json.put("campaigns", jsonObject);
			json.put("status", "SUCCESS");
			json.put("message", "You got Campaigns");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Campaigns Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/campaigns", method = RequestMethod.POST, produces = "application/json")
	public void getCampaignAdsV1(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Long> matchedLiveCampaignForUser = campaignService.getMatchedLiveCampaignForUser(trip.getUserid());
		if (CollectionUtils.isNotEmpty(matchedLiveCampaignForUser)) {
			System.out.println("size=====" + matchedLiveCampaignForUser.size());
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(matchedLiveCampaignForUser, matchedLiveCampaignForUser.getClass());
			JSONArray jsonObject = new JSONArray(json2);
			json.put("campaigns", jsonObject);
			json.put("status", "SUCCESS");
			json.put("message", "You got Campaigns");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Campaigns Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/travelpurposes", method = RequestMethod.GET, produces = "application/json")
	public void getTravelPurposesList(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelPurposeDemoGraphic> interests = demoGraphicsService.getTravelPurposeDemoGraphicList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (TravelPurposeDemoGraphic interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					array.put(object);
				}
				json.put("travelpurposes", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/travelpurposes", method = RequestMethod.GET, produces = "application/json")
	public void getTravelPurposesListV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelPurposeDemoGraphic> interests = demoGraphicsService.getTravelPurposeDemoGraphicList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (TravelPurposeDemoGraphic interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					array.put(object);
				}
				json.put("travelpurposes", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/travelclasses", method = RequestMethod.GET, produces = "application/json")
	public void getTravelClassesList(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelClass> interests = demoGraphicsService.getTravelClassList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (TravelClass interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					array.put(object);
				}
				json.put("travelclasses", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/travelclasses", method = RequestMethod.GET, produces = "application/json")
	public void getTravelClassesListV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelClass> interests = demoGraphicsService.getTravelClassList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (TravelClass interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					array.put(object);
				}
				json.put("travelclasses", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/travelvalues", method = RequestMethod.GET, produces = "application/json")
	public void getTravelvaluesList(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelClass> interests = demoGraphicsService.getTravelClassList();
		JSONArray travelclasses = new JSONArray();
		JSONArray travelpurposes = new JSONArray();
		if (interests != null) {
			if (CollectionUtils.isNotEmpty(interests)) {
				for (TravelClass interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					travelclasses.put(object);
				}
			}

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		List<TravelPurposeDemoGraphic> purposes = demoGraphicsService.getTravelPurposeDemoGraphicList();
		if (purposes != null) {
			if (CollectionUtils.isNotEmpty(purposes)) {
				for (TravelPurposeDemoGraphic interest : purposes) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					travelpurposes.put(object);
				}
			}
		}
		json.put("status", "SUCCESS");
		json.put("travelclasses", travelclasses);
		json.put("travelpurposes", travelpurposes);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/travelvalues", method = RequestMethod.GET, produces = "application/json")
	public void getTravelvaluesListV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<TravelClass> interests = demoGraphicsService.getTravelClassList();
		JSONArray travelclasses = new JSONArray();
		JSONArray travelpurposes = new JSONArray();
		if (interests != null) {
			if (CollectionUtils.isNotEmpty(interests)) {
				for (TravelClass interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					travelclasses.put(object);
				}
			}

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No interests data available");
		}
		List<TravelPurposeDemoGraphic> purposes = demoGraphicsService.getTravelPurposeDemoGraphicList();
		if (purposes != null) {
			if (CollectionUtils.isNotEmpty(purposes)) {
				for (TravelPurposeDemoGraphic interest : purposes) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getValue());
					travelpurposes.put(object);
				}
			}
		}
		json.put("status", "SUCCESS");
		json.put("travelclasses", travelclasses);
		json.put("travelpurposes", travelpurposes);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/getcurrentairport", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void getCurrentAirport(@RequestBody LocationRequestDto location, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		UserTripStatus status = null;
		Airport userAtAirport = locationService.getCloserAirportsByUserLocation(location.getLatitude(), location.getLongitude());
		if (userAtAirport != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You Are Located At An Airport");
			json.put("isAtAiport", Boolean.TRUE);
			AirportDto airportDto = new AirportDto(userAtAirport);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(airportDto, airportDto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("airport", jsonObject);
		} else {
			json.put("isAtAiport", Boolean.FALSE);
			json.put("message", "You Are Not Located At Any Airport");
		}
		Trip todayTripForUser = tripService.getTodayTripForUser(location.getUserId());
		Trip nextTripForUser = tripService.getNextTripForUser(location.getUserId());
		Boolean hasTrip = Boolean.FALSE;
		if (todayTripForUser != null) {
			hasTrip = Boolean.TRUE;
			Terminal terminalOrigin = todayTripForUser.getTerminalOrigin();
			Terminal terminalDestination = todayTripForUser.getTerminalDestination();
			if (terminalOrigin != null) {
				json.put("terminalOrigin", terminalOrigin.getId());
			}
			if (terminalDestination != null) {
				json.put("terminalDestination", terminalDestination.getId());
			}
			JSONArray airportsList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			object.put("iatacode", todayTripForUser.getOrigin().getIataCode());
			object.put("name", todayTripForUser.getOrigin().getName());
			object1.put("iatacode", todayTripForUser.getDestination().getIataCode());
			object1.put("name", todayTripForUser.getDestination().getName());
			airportsList.put(object);
			airportsList.put(object1);
			json.put("status", "SUCCESS");
			json.put("airportsList", airportsList);
		} else if (nextTripForUser != null) {
			Terminal terminalOrigin = nextTripForUser.getTerminalOrigin();
			Terminal terminalDestination = nextTripForUser.getTerminalDestination();
			if (terminalOrigin != null) {
				json.put("terminalOrigin", terminalOrigin.getId());
			}
			if (terminalDestination != null) {
				json.put("terminalDestination", terminalDestination.getId());
			}
			JSONArray airportsList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			object.put("iatacode", nextTripForUser.getOrigin().getIataCode());
			object.put("name", nextTripForUser.getOrigin().getName());
			object1.put("iatacode", nextTripForUser.getDestination().getIataCode());
			object1.put("name", nextTripForUser.getDestination().getName());
			airportsList.put(object);
			airportsList.put(object1);
			json.put("status", "SUCCESS");
			json.put("airportsList", airportsList);
		} else {
			json.put("status", "FAILURE");
		}
		if (userAtAirport == null && todayTripForUser == null) {
			Boolean hasFutureTripsForNext15Days = tripService.hasFutureTripsForNext15Days(location.getUserId());
			if (hasFutureTripsForNext15Days) {
				status = UserTripStatus.TRIP_NOT_AT_AIRPORT;
			} else {
				status = UserTripStatus.PRE_TRIP;
			}
		} else if (userAtAirport != null && todayTripForUser != null) {
			status = UserTripStatus.TRIP_AT_AIRPORT;
		}
		if (status == null) {
			status = UserTripStatus.UNDEFINED;
		}
		Date dateTime = new Date();
		User userData = userService.find(location.getUserId());
		String userSessionToken = userData.getSessionToken();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userData.getId());
		logTrack.setUserLogType("ENTERRETAIL");
		logTrack.setUpdateAt(dateTime);
		logTrack.setLatitude(location.getLatitude());
		logTrack.setLongitude(location.getLongitude());
		logTrack.setUserSessionToken(userSessionToken);
		userService.save(logTrack);
		json.put("hasTrip", hasTrip);
		json.put("state", status.getType());
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/getcurrentairport", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void getCurrentAirportV1(@RequestBody LocationRequestDto location, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		UserTripStatus status = null;
		Airport userAtAirport = locationService.getCloserAirportsByUserLocation(location.getLatitude(), location.getLongitude());
		if (userAtAirport != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You Are Located At An Airport");
			json.put("isAtAiport", Boolean.TRUE);
			AirportDto airportDto = new AirportDto(userAtAirport);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(airportDto, airportDto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("airport", jsonObject);
		} else {
			json.put("isAtAiport", Boolean.FALSE);
			json.put("message", "You Are Not Located At Any Airport");
		}
		Trip todayTripForUser = tripService.getTodayTripForUser(location.getUserId());
		Trip nextTripForUser = tripService.getNextTripForUser(location.getUserId());
		Boolean hasTrip = Boolean.FALSE;
		if (todayTripForUser != null) {
			hasTrip = Boolean.TRUE;
			Terminal terminalOrigin = todayTripForUser.getTerminalOrigin();
			Terminal terminalDestination = todayTripForUser.getTerminalDestination();
			if (terminalOrigin != null) {
				json.put("terminalOrigin", terminalOrigin.getId());
			}
			if (terminalDestination != null) {
				json.put("terminalDestination", terminalDestination.getId());
			}
			JSONArray airportsList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			Airport originairport = locationService.findAirportByCode(todayTripForUser.getOrigin().getIataCode());
			Airport destinationAirport = locationService.findAirportByCode(todayTripForUser.getDestination().getIataCode());
			object.put("iatacode", todayTripForUser.getOrigin().getIataCode());
			object.put("name", todayTripForUser.getOrigin().getName());
			object.put("isIntransitAirport", originairport.getIsintransitAirport());
			object1.put("iatacode", todayTripForUser.getDestination().getIataCode());
			object1.put("name", todayTripForUser.getDestination().getName());
			object1.put("isIntransitAirport", destinationAirport.getIsintransitAirport());
			airportsList.put(object);
			airportsList.put(object1);
			json.put("status", "SUCCESS");
			/*
			 * json.put("tripId", todayTripForUser.getId());
			 * json.put("originName",
			 * todayTripForUser.getOrigin().getIataCode());
			 * json.put("destinationName",
			 * todayTripForUser.getDestination().getIataCode());
			 */
			json.put("airportsList", airportsList);
		} else if (nextTripForUser != null) {
			Terminal terminalOrigin = nextTripForUser.getTerminalOrigin();
			Terminal terminalDestination = nextTripForUser.getTerminalDestination();
			if (terminalOrigin != null) {
				json.put("terminalOrigin", terminalOrigin.getId());
			}
			if (terminalDestination != null) {
				json.put("terminalDestination", terminalDestination.getId());
			}
			JSONArray airportsList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			Airport originairport = locationService.findAirportByCode(nextTripForUser.getOrigin().getIataCode());
			Airport destinationAirport = locationService.findAirportByCode(nextTripForUser.getDestination().getIataCode());
			object.put("iatacode", nextTripForUser.getOrigin().getIataCode());
			object.put("name", nextTripForUser.getOrigin().getName());
			object.put("isIntransitAirport", originairport.getIsintransitAirport());
			object1.put("iatacode", nextTripForUser.getDestination().getIataCode());
			object1.put("name", nextTripForUser.getDestination().getName());
			object1.put("isIntransitAirport", destinationAirport.getIsintransitAirport());
			airportsList.put(object);
			airportsList.put(object1);
			json.put("status", "SUCCESS");
			/*
			 * json.put("tripId", todayTripForUser.getId());
			 * json.put("originName",
			 * todayTripForUser.getOrigin().getIataCode());
			 * json.put("destinationName",
			 * todayTripForUser.getDestination().getIataCode());
			 */
			json.put("airportsList", airportsList);
		} else {
			json.put("status", "FAILURE");
		}
		if (userAtAirport == null && todayTripForUser == null) {
			Boolean hasFutureTripsForNext15Days = tripService.hasFutureTripsForNext15Days(location.getUserId());
			if (hasFutureTripsForNext15Days) {
				status = UserTripStatus.TRIP_NOT_AT_AIRPORT;
			} else {
				status = UserTripStatus.PRE_TRIP;
			}
		} else if (userAtAirport != null && todayTripForUser != null) {
			status = UserTripStatus.TRIP_AT_AIRPORT;
		}
		if (status == null) {
			status = UserTripStatus.UNDEFINED;
		}

		/* its for logging track details */
		Date dateTime = new Date();
		User userData = userService.find(location.getUserId());
		String userSessionToken = userData.getSessionToken();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userData.getId());
		logTrack.setUserLogType("ENTERRETAIL");
		logTrack.setUpdateAt(dateTime);
		logTrack.setLatitude(location.getLatitude());
		logTrack.setLongitude(location.getLongitude());
		logTrack.setUserSessionToken(userSessionToken);
		userService.save(logTrack);

		json.put("hasTrip", hasTrip);
		json.put("state", status.getType());
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/airportslist", method = RequestMethod.POST, produces = "application/json")
	@Transactional
	public void apiAirportsByTripid(@RequestBody TripDto trip, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println(trip.getId());
		Trip currentTrip = tripService.find(trip.getId());
		if (currentTrip != null) {
			JSONArray airportList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			object.put("iatacode", currentTrip.getOrigin().getIataCode());
			object.put("name", currentTrip.getOrigin().getName());
			object1.put("iatacode", currentTrip.getDestination().getIataCode());
			object1.put("name", currentTrip.getDestination().getName());
			airportList.put(object);
			airportList.put(object1);
			json.put("status", "SUCCESS");
			json.put("airportsList", airportList);
			json.put("message", "you got data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Matching trip is available to delete");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/airportslist", method = RequestMethod.POST, produces = "application/json")
	@Transactional
	public void apiAirportsByTripidV1(@RequestBody LocationRequestDto location, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		System.out.println(location.getTripId());
		Trip currentTrip = tripService.find(location.getTripId());
		UserTripStatus status = null;
		Airport userAtAirport = locationService.getCloserAirportsByUserLocation(location.getLatitude(), location.getLongitude());
		if (userAtAirport != null) {
			json.put("isAtAiport", Boolean.TRUE);
		}
		Trip todayTripForUser = tripService.getTodayTripForUser(location.getUserId());
		if (userAtAirport == null && todayTripForUser == null) {
			Boolean hasFutureTripsForNext15Days = tripService.hasFutureTripsForNext15Days(location.getUserId());
			if (hasFutureTripsForNext15Days) {
				status = UserTripStatus.TRIP_NOT_AT_AIRPORT;
			} else {
				status = UserTripStatus.PRE_TRIP;
			}
		} else if (userAtAirport != null && todayTripForUser != null) {
			status = UserTripStatus.TRIP_AT_AIRPORT;
		}
		if (status == null) {
			status = UserTripStatus.UNDEFINED;
		}
		json.put("state", status.getType());
		if (currentTrip != null) {
			JSONArray airportList = new JSONArray();
			JSONObject object = new JSONObject();
			JSONObject object1 = new JSONObject();
			object.put("iatacode", currentTrip.getOrigin().getIataCode());
			object.put("name", currentTrip.getOrigin().getName());
			object.put("isIntransitAirport", currentTrip.getOrigin().getIsintransitAirport());
			object1.put("iatacode", currentTrip.getDestination().getIataCode());
			object1.put("name", currentTrip.getDestination().getName());
			object1.put("isIntransitAirport", currentTrip.getDestination().getIsintransitAirport());
			airportList.put(object);
			airportList.put(object1);
			json.put("status", "SUCCESS");
			json.put("airportsList", airportList);
			json.put("message", "you got data");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Matching trip is available to delete");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	/* newly added api for add new trip */
	@RequestMapping(value = "/v1/addnewtrip", method = RequestMethod.POST, produces = "application/json")
	public void addNewTrip(@RequestBody LocationRequestDto location, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		Long userId = location.getUserId();
		User userdata = userService.find(location.getUserId());
		Date dateTime = new Date();
		String userSessionToken = userdata.getSessionToken();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userId);
		logTrack.setUserLogType("ENTERADDNEWTRIP");
		logTrack.setUpdateAt(dateTime);
		logTrack.setUserSessionType(userdata.getUserState());
		logTrack.setUserSessionToken(userSessionToken);
		logTrack.setLatitude(location.getLatitude());
		logTrack.setLongitude(location.getLongitude());
		userService.save(logTrack);
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

}

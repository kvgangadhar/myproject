/**
 * 
 */
package com.intransit.merchant.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intransit.email.MailMail;
import com.intransit.merchant.api.dto.SocialInviteRequestDto;
import com.intransit.merchant.api.dto.SocialInviteStoreRequestDto;
import com.intransit.merchant.api.dto.SocialNotificationDto;
import com.intransit.merchant.api.dto.StoreDto;
import com.intransit.merchant.api.dto.TripDto;
import com.intransit.merchant.api.dto.UserDto;
import com.intransit.merchant.api.dto.UserProfileDto;
import com.intransit.merchant.api.flighxml.FlightXmlService;
import com.intransit.merchant.api.webservice.BaseEndpoint;
import com.intransit.merchant.api.webservice.UserWrapper;
import com.intransit.merchant.helper.IntransitUtils;
import com.intransit.merchant.model.Airport;
import com.intransit.merchant.model.LoggingTrack;
import com.intransit.merchant.model.Membership;
import com.intransit.merchant.model.Store;
import com.intransit.merchant.model.Terminal;
import com.intransit.merchant.model.Trip;
import com.intransit.merchant.model.User;
import com.intransit.merchant.model.UserConnections;
import com.intransit.merchant.model.UserInterests;
import com.intransit.merchant.model.UserNotifications;
import com.intransit.merchant.service.DemoGraphicsService;
import com.intransit.merchant.service.LocationService;
import com.intransit.merchant.service.PushNotificationService;
import com.intransit.merchant.service.SocialLoungeService;
import com.intransit.merchant.service.TripService;
import com.intransit.merchant.service.UserService;
import com.intransit.merchant.validator.UserValidator;

/**
 * @author kodelavg
 *
 */
@RestController
@RequestMapping("/api/lounge")
public class SocialLoungeController extends BaseEndpoint {

	@Autowired
	private MessageSource messageSource;

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
	protected SocialLoungeService socialLoungeService;

	@Resource
	protected PushNotificationService pushNotificationService;

	private static final Logger logger = LoggerFactory.getLogger(SocialLoungeController.class);

	@RequestMapping(value = "/enter", method = RequestMethod.POST, produces = "application/json")
	public void enterLounge(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(inviteRequestDto.getUserId());
		int profileCompleted = 0;
		if (userdata != null) {
			profileCompleted += 35;
			if (!StringUtils.isEmpty(userdata.getAboutMe())) {
				profileCompleted += 15;
			}
			if (userdata.getSeatPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getMealPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getClassPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getProfilePic() != null) {
				profileCompleted += 15;
			}
			List<UserInterests> interestsList = userService.findInterestsByUserId(inviteRequestDto.getUserId());
			List<Membership> MembersList = userService.getMembersByUserId(inviteRequestDto.getUserId());
			if (CollectionUtils.isNotEmpty(interestsList)) {
				profileCompleted += 10;
			}
			if (CollectionUtils.isNotEmpty(MembersList)) {
				profileCompleted += MembersList.size() * 2;
			}
			json.put("profilePercentage", profileCompleted);
			Trip todayTripForUser = tripService.getTodayTripForUser(userdata.getId());
			Boolean hasTrip = Boolean.FALSE;
			Boolean isAtAirportOfTrip = Boolean.FALSE;
			Airport userAtAirport = locationService.getCloserAirportsByUserLocation(inviteRequestDto.getLatitude(), inviteRequestDto.getLongitude());
			String iataCode = "";
			Long terminalId = null;
			if (todayTripForUser != null) {
				hasTrip = Boolean.TRUE;
				if (userAtAirport != null) {
					Airport origin = todayTripForUser.getOrigin();
					Airport destination = todayTripForUser.getDestination();
					if (origin != null && StringUtils.equals(userAtAirport.getIataCode(), origin.getIataCode()) || destination != null
							&& StringUtils.equals(userAtAirport.getIataCode(), destination.getIataCode())) {
						isAtAirportOfTrip = Boolean.TRUE;
						if (origin != null && StringUtils.equals(userAtAirport.getIataCode(), origin.getIataCode())) {
							iataCode = origin.getIataCode();
							Terminal terminalOrigin = todayTripForUser.getTerminalOrigin();
							if (terminalOrigin != null) {
								terminalId = terminalOrigin.getId();
							}
						} else if (origin != null && StringUtils.equals(userAtAirport.getIataCode(), destination.getIataCode())) {
							iataCode = destination.getIataCode();
							Terminal terminalDestination = todayTripForUser.getTerminalDestination();
							if (terminalDestination != null) {
								terminalId = terminalDestination.getId();
							}
						}
					}
				}
			}
			Boolean hasProfilePicture = Boolean.FALSE;
			if (StringUtils.isNotEmpty(userdata.getFormedProfileUrl())) {
				hasProfilePicture = Boolean.TRUE;
			}
			json.put("hasProfilePicture", hasProfilePicture);
			json.put("hasTrip", hasTrip);
			json.put("iataCode", iataCode);
			json.put("terminalId", terminalId);
			json.put("isAtAirportOfTrip", isAtAirportOfTrip);
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");
			Date dateTime = new Date();
			String userSessionToken = userdata.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ENTERLOUNGE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			logTrack.setUserSessionToken(userSessionToken);
			userService.save(logTrack);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/enter", method = RequestMethod.POST, produces = "application/json")
	public void enterLoungeV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(inviteRequestDto.getUserId());
		int profileCompleted = 0;
		if (userdata != null) {
			profileCompleted += 35;
			if (!StringUtils.isEmpty(userdata.getAboutMe())) {
				profileCompleted += 15;
			}
			if (userdata.getSeatPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getMealPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getClassPreference() != null) {
				profileCompleted += 5;
			}
			if (userdata.getProfilePic() != null) {
				profileCompleted += 15;
			}
			List<UserInterests> interestsList = userService.findInterestsByUserId(inviteRequestDto.getUserId());
			List<Membership> MembersList = userService.getMembersByUserId(inviteRequestDto.getUserId());

			if (CollectionUtils.isNotEmpty(interestsList)) {
				profileCompleted += 10;
			}

			if (CollectionUtils.isNotEmpty(MembersList)) {
				profileCompleted += MembersList.size() * 2;
			}
			json.put("profilePercentage", profileCompleted);
			Trip todayTripForUser = tripService.getTodayTripForUser(userdata.getId());
			Boolean hasTrip = Boolean.FALSE;
			Boolean isAtAirportOfTrip = Boolean.FALSE;
			Airport userAtAirport = locationService.getCloserAirportsByUserLocation(inviteRequestDto.getLatitude(), inviteRequestDto.getLongitude());
			String iataCode = "";
			Long terminalId = null;
			if (todayTripForUser != null) {
				hasTrip = Boolean.TRUE;

				if (userAtAirport != null) {
					Airport origin = todayTripForUser.getOrigin();
					Airport destination = todayTripForUser.getDestination();
					if ((origin != null && StringUtils.equals(userAtAirport.getIataCode(), origin.getIataCode()))
							|| (destination != null && StringUtils.equals(userAtAirport.getIataCode(), destination.getIataCode()))) {
						isAtAirportOfTrip = Boolean.TRUE;
						if ((origin != null && StringUtils.equals(userAtAirport.getIataCode(), origin.getIataCode()))) {
							iataCode = origin.getIataCode();
							Terminal terminalOrigin = todayTripForUser.getTerminalOrigin();
							if (terminalOrigin != null) {
								terminalId = terminalOrigin.getId();
							}
						} else if ((origin != null && StringUtils.equals(userAtAirport.getIataCode(), destination.getIataCode()))) {
							iataCode = destination.getIataCode();
							Terminal terminalDestination = todayTripForUser.getTerminalDestination();
							if (terminalDestination != null) {
								terminalId = terminalDestination.getId();
							}
						}
					}
				}

			}
			Boolean hasProfilePicture = Boolean.FALSE;
			if (StringUtils.isNotEmpty(userdata.getFormedProfileUrl())) {
				hasProfilePicture = Boolean.TRUE;
			}
			json.put("hasProfilePicture", hasProfilePicture);
			json.put("hasTrip", hasTrip);
			json.put("iataCode", iataCode);
			json.put("terminalId", terminalId);
			json.put("isAtAirportOfTrip", isAtAirportOfTrip);
			if (userdata.getGender() != null) {
				json.put("gender", true);
			} else {
				json.put("gender", false);
			}
			if (userdata.getDateofBirth() != null) {
				json.put("dob", true);
			} else {
				json.put("dob", false);
			}
			if (userdata.getHomeTown() != null) {
				json.put("hometown", true);
			} else {
				json.put("hometown", false);
			}
			if (userdata.getOccupation() != null) {
				json.put("occupation", true);
			} else {
				json.put("occupation", false);
			}
			if (userdata.getNationality() != null) {
				json.put("nationality", true);
			} else {
				json.put("nationality", false);
			}
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");

			/* its for logging track details */
			Date dateTime = new Date();
			String userSessionToken = userdata.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ENTERLOUNGE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			logTrack.setUserSessionToken(userSessionToken);
			userService.save(logTrack);

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/get/friends", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsList(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Gson gson = new GsonBuilder().serializeNulls().create();
		JSONArray jsonObject1 = new JSONArray();
		JSONArray jsonObject = new JSONArray();
		Boolean hasActiveUsers = false;
		User userdata = userService.find(inviteRequestDto.getUserId());
		if (userdata != null) {
			List<UserConnections> allInvitesReceived;
			try {
				List<User> allActiveUsersExceptMe = socialLoungeService.getMatchedUsersList(inviteRequestDto.getUserId(), inviteRequestDto.getLatitude(),
						inviteRequestDto.getLongitude());
				HashMap<User, Trip> userswithtrip = new HashMap<User, Trip>();
				if (CollectionUtils.isNotEmpty(allActiveUsersExceptMe)) {
					for (User user : allActiveUsersExceptMe) {
						Trip trip = tripService.getTodayTripForUser(user.getId());
						userswithtrip.put(user, trip);
					}
					System.out.println("size=====" + allActiveUsersExceptMe.size());
					UserDto dto = new UserDto(userswithtrip);
					List<UserDto> users = dto.getUsers();
					String json2 = gson.toJson(users, users.getClass());
					jsonObject = new JSONArray(json2);
					hasActiveUsers = true;
				}
			} catch (Exception e) {
				System.out.println("Error In Fetching Friends Details" + e.toString());
			}
			if (CollectionUtils.isNotEmpty(allInvitesReceived = socialLoungeService.getAllInvitesReceivedAndAccepted(inviteRequestDto.getUserId()))) {
				SocialNotificationDto notificationDto = new SocialNotificationDto(allInvitesReceived);
				List<SocialNotificationDto> socialNotifications = notificationDto.getSocialNotifications();
				String json3 = gson.toJson(socialNotifications, socialNotifications.getClass());
				jsonObject1 = new JSONArray(json3);
			}
			json.put("notifications", jsonObject1);
			json.put("users", jsonObject);
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");
			Date dateTime = new Date();
			String userSessionToken = userdata.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ENTERFRIENDLIST");
			logTrack.setUpdateAt(dateTime);
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			logTrack.setUserSessionToken(userSessionToken);
			userService.save(logTrack);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "You got no matches");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/friends", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsListV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Gson gson = new GsonBuilder().serializeNulls().create();
		// List<User> allActiveUsersExceptMe =
		// socialLoungeService.getAllActiveUsers(inviteRequestDto.getUserId());
		// List<User> allActiveUsersExceptMe =
		// socialLoungeService.getMatchedUsersList(inviteRequestDto.getUserId());
		JSONArray jsonObject1 = new JSONArray();
		JSONArray jsonObject = new JSONArray();
		Boolean hasActiveUsers = false;
		User userdata = userService.find(inviteRequestDto.getUserId());
		if (userdata != null) {
			try {
				List<User> allActiveUsersExceptMe = socialLoungeService.getMatchedUsersList(inviteRequestDto.getUserId(), inviteRequestDto.getLatitude(),
						inviteRequestDto.getLongitude());
				Map<User, Trip> userswithtrip = new HashMap<User, Trip>();
				if (CollectionUtils.isNotEmpty(allActiveUsersExceptMe)) {
					for (User user : allActiveUsersExceptMe) {
						Trip trip = tripService.getTodayTripForUser(user.getId());
						userswithtrip.put(user, trip);
					}

					System.out.println("size=====" + allActiveUsersExceptMe.size());
					UserDto dto = new UserDto(userswithtrip);
					List<UserDto> users = dto.getUsers();
					String json2 = gson.toJson(users, users.getClass());
					jsonObject = new JSONArray(json2);
					hasActiveUsers = true;
				}
			} catch (Exception e) {
				System.out.println("Error In Fetching Friends Details" + e.toString());
			}
			List<UserConnections> allInvitesReceived = socialLoungeService.getAllInvitesReceivedAndAccepted(inviteRequestDto.getUserId());
			if (CollectionUtils.isNotEmpty(allInvitesReceived)) {
				SocialNotificationDto notificationDto = new SocialNotificationDto(allInvitesReceived);
				List<SocialNotificationDto> socialNotifications = notificationDto.getSocialNotifications();
				String json3 = gson.toJson(socialNotifications, socialNotifications.getClass());
				jsonObject1 = new JSONArray(json3);
			}
			json.put("notifications", jsonObject1);
			json.put("users", jsonObject);
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");

			/* its for logging track details */
			Date dateTime = new Date();
			String userSessionToken = userdata.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ENTERFRIENDLIST");
			logTrack.setUpdateAt(dateTime);
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			logTrack.setUserSessionToken(userSessionToken);

			long profileCompleted = 0;
			if (userdata != null) {
				profileCompleted += 35;
				if (!StringUtils.isEmpty(userdata.getAboutMe())) {
					profileCompleted += 15;
				}
				if (userdata.getSeatPreference() != null) {
					profileCompleted += 5;
				}
				if (userdata.getMealPreference() != null) {
					profileCompleted += 5;
				}
				if (userdata.getClassPreference() != null) {
					profileCompleted += 5;
				}
				if (userdata.getProfilePic() != null) {
					profileCompleted += 15;
				}
				List<UserInterests> interestsList = userService.findInterestsByUserId(inviteRequestDto.getUserId());
				List<Membership> MembersList = userService.getMembersByUserId(inviteRequestDto.getUserId());

				if (CollectionUtils.isNotEmpty(interestsList)) {
					profileCompleted += 10;
				}

				if (CollectionUtils.isNotEmpty(MembersList)) {
					profileCompleted += MembersList.size() * 2;
				}
			}
			logTrack.setLogField1(profileCompleted);
			userService.save(logTrack);

		} else {
			json.put("status", "FAILURE");
			json.put("message", "You got no matches");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/get/friend/details", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsDetails(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Trip todayTripForUser = tripService.getTodayTripForUser(friendData.getId());
				UserDto dto = new UserDto(friendData, todayTripForUser);
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(inviteRequestDto.getUserId(), inviteRequestDto.getFriendId());
				Boolean hasInvitesBetween = socialLoungeService.hasInvitesBetween(inviteRequestDto.getUserId(), inviteRequestDto.getFriendId());
				Gson gson = new GsonBuilder().serializeNulls().create();
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("friend", jsonObject);
				json.put("hasMadeConnection", hasMadeConnections);
				json.put("hasInvitesBetween", hasInvitesBetween);
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/friend/details", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsDetailsV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Trip todayTripForUser = tripService.getTodayTripForUser(friendData.getId());
				UserDto dto = new UserDto(friendData, todayTripForUser);
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(inviteRequestDto.getUserId(), inviteRequestDto.getFriendId());
				Boolean hasInvitesBetween = socialLoungeService.hasInvitesBetween(inviteRequestDto.getUserId(), inviteRequestDto.getFriendId());
				Gson gson = new GsonBuilder().serializeNulls().create();
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("friend", jsonObject);
				json.put("hasMadeConnection", hasMadeConnections);
				json.put("hasInvitesBetween", hasInvitesBetween);
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	public JSONObject getUserData(JSONObject json, User friendData) throws JSONException {
		int profileCompleted = 0;
		json.put("userId", friendData.getId());
		json.put("email", friendData.getEmail());
		json.put("firstname", friendData.getFirstName());
		json.put("lastname", friendData.getLastName());
		json.put("gender", friendData.getGender());
		json.put("dat of birth", friendData.getDateofBirth());
		json.put("hometown", friendData.getHomeTown().getName());
		json.put("occupation", friendData.getOccupation().getValue());
		profileCompleted += 35;
		if (friendData.getNationality().getName() != null) {
			json.put("nationality", friendData.getNationality().getName());
		}
		if (!StringUtils.isEmpty(friendData.getAboutMe())) {
			json.put("aboutme", friendData.getAboutMe());
			profileCompleted += 15;
		}
		if (friendData.getSeatPreference() != null) {
			json.put("seatPreference", friendData.getSeatPreference().getPreferenceName());
			profileCompleted += 5;
		}

		if (friendData.getMealPreference() != null) {
			json.put("mealPreference", friendData.getMealPreference().getPreferenceName());
			profileCompleted += 5;
		}
		if (friendData.getClassPreference() != null) {
			json.put("classPreference", friendData.getClassPreference().getPreferenceName());
			profileCompleted += 5;
		}
		if (friendData.getSeatPrivacy() != null) {
			json.put("seatPrivacy", friendData.getSeatPrivacy().getPrivacyName());
		}
		if (friendData.getClassPrivacy() != null) {
			json.put("classPrivacy", friendData.getClassPrivacy().getPrivacyName());
		}
		if (friendData.getMealPrivacy() != null) {
			json.put("mealPrivacy", friendData.getMealPrivacy().getPrivacyName());
		}
		if (friendData.getInterestsPrivacy() != null) {
			json.put("interestsPrivacy", friendData.getInterestsPrivacy().getPrivacyName());
		}
		if (friendData.getMembershipsPrivacy() != null) {
			json.put("membersPrivacy", friendData.getMembershipsPrivacy().getPrivacyName());
		}
		if (friendData.getDobPrivacy() != null) {
			json.put("dateofbirthPrivacy", friendData.getDobPrivacy().getPrivacyName());
		}
		if (friendData.getEmailPrivacy() != null) {
			json.put("emailPrivacy", friendData.getEmailPrivacy().getPrivacyName());
		}
		if (friendData.getGenderPrivacy() != null) {
			json.put("genderPrivacy", friendData.getGenderPrivacy().getPrivacyName());
		}
		if (friendData.getHometownPrivacy() != null) {
			json.put("hometownPrivacy", friendData.getHometownPrivacy().getPrivacyName());
		}
		if (friendData.getOccupationPrivacy() != null) {
			json.put("occupationPrivacy", friendData.getOccupationPrivacy().getPrivacyName());
		}
		if (friendData.getNationalityPrivacy() != null) {
			json.put("nationalityPrivacy", friendData.getNationalityPrivacy().getPrivacyName());
		}
		if (friendData.getAboutmePrivacy() != null) {
			json.put("aboutmePrivacy", friendData.getAboutmePrivacy().getPrivacyName());
		}

		List<UserInterests> interestsList = userService.findInterestsByUserId(friendData.getId());
		List<Membership> MembersList = userService.getMembersByUserId(friendData.getId());

		if (CollectionUtils.isNotEmpty(interestsList)) {
			profileCompleted += 10;
			JSONArray array = new JSONArray();
			for (UserInterests interest : interestsList) {
				JSONObject object = new JSONObject();
				object.put("interest", interest.getInterest().getInterestName());
				array.put(object);
			}
			json.put("interests", array);
		}

		if (CollectionUtils.isNotEmpty(MembersList)) {
			profileCompleted += MembersList.size() * 2;
			JSONArray array = new JSONArray();
			for (Membership member : MembersList) {
				JSONObject object = new JSONObject();
				object.put("membershipNumber", member.getMembershipNumber());
				array.put(object);
			}
			json.put("membershipNumbers", array);
		}
		json.put("ProfileCompleted", profileCompleted + "%");
		return json;

	}

	@RequestMapping(value = "/get/friend/about", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsAboutMe(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(userData.getId(), friendData.getId());
				json.put("status", "SUCCESS");
				List<UserInterests> interestsList = userService.findInterestsByUserId(friendData.getId());
				List<Membership> MembersList = userService.getMembersByUserId(friendData.getId());
				UserProfileDto dto = new UserProfileDto(friendData, interestsList, MembersList);
				Gson gson = new GsonBuilder().serializeNulls().create();
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("userDetails", jsonObject);
				json.put("connections", "Your connections are : 3");
				json.put("deals", "Your deals are : 4");
				json.put("hasMadeConnection", hasMadeConnections);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/friend/about", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsAboutMeV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(userData.getId(), friendData.getId());
				json.put("status", "SUCCESS");
				List<UserInterests> interestsList = userService.findInterestsByUserId(friendData.getId());
				List<Membership> MembersList = userService.getMembersByUserId(friendData.getId());
				UserProfileDto dto = new UserProfileDto(friendData, interestsList, MembersList);
				Gson gson = new GsonBuilder().serializeNulls().create();
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("userDetails", jsonObject);
				json.put("connections", "Your connections are : " + 3);
				json.put("deals", "Your deals are : " + 4);
				json.put("hasMadeConnection", hasMadeConnections);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/get/friend/travel", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsTravel(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Gson gson = new GsonBuilder().serializeNulls().create();
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(userData.getId(), friendData.getId());
				Trip todayTripForUser = tripService.getTodayTripForUser(friendData.getId());
				BigInteger futureTrips = tripService.futureTrips(friendData.getId());
				TripDto dto;
				if (todayTripForUser != null) {
					dto = new TripDto(todayTripForUser);
				} else {
					dto = new TripDto();
				}
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("trip", jsonObject);
				json.put("futuretrips", futureTrips);
				json.put("status", "SUCCESS");
				List<UserInterests> interestsList = userService.findInterestsByUserId(friendData.getId());
				List<Membership> MembersList = userService.getMembersByUserId(friendData.getId());
				UserProfileDto profileDto = new UserProfileDto(friendData, interestsList, MembersList);
				String profileJson2 = gson.toJson(profileDto, profileDto.getClass());
				JSONObject profileJsonObject = new JSONObject(profileJson2);
				json.put("userDetails", profileJsonObject);
				json.put("connections", "Your connections are : 3");
				json.put("deals", "Your deals are : 4");
				json.put("hasMadeConnection", hasMadeConnections);
				Date dateTime = new Date();
				String userSessionToken = userData.getSessionToken();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(inviteRequestDto.getUserId());
				logTrack.setUserLogType("VIEWPROFILE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionToken(userSessionToken);
				logTrack.setLogField1(inviteRequestDto.getFriendId());
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/friend/travel", method = RequestMethod.POST, produces = "application/json")
	public void getFriendsTravelV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userData = userService.find(inviteRequestDto.getUserId());
		if (userData != null) {
			User friendData = userService.find(inviteRequestDto.getFriendId());
			if (friendData != null) {
				Gson gson = new GsonBuilder().serializeNulls().create();
				Boolean hasMadeConnections = socialLoungeService.hasMadeConnections(userData.getId(), friendData.getId());
				Trip todayTripForUser = tripService.getTodayTripForUser(friendData.getId());
				BigInteger futureTrips = tripService.futureTrips(friendData.getId());
				TripDto dto;
				if (todayTripForUser != null) {
					dto = new TripDto(todayTripForUser);
				} else {
					dto = new TripDto();
				}
				String json2 = gson.toJson(dto, dto.getClass());
				JSONObject jsonObject = new JSONObject(json2);
				json.put("trip", jsonObject);
				json.put("futuretrips", futureTrips);
				json.put("status", "SUCCESS");
				List<UserInterests> interestsList = userService.findInterestsByUserId(friendData.getId());
				List<Membership> MembersList = userService.getMembersByUserId(friendData.getId());
				UserProfileDto profileDto = new UserProfileDto(friendData, interestsList, MembersList);
				String profileJson2 = gson.toJson(profileDto, profileDto.getClass());
				JSONObject profileJsonObject = new JSONObject(profileJson2);
				json.put("userDetails", profileJsonObject);
				json.put("connections", "Your connections are : " + 3);
				json.put("deals", "Your deals are : " + 4);
				json.put("hasMadeConnection", hasMadeConnections);

				/* its for logging track details */
				Date dateTime = new Date();
				String userSessionToken = userData.getSessionToken();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(inviteRequestDto.getUserId());
				logTrack.setUserLogType("VIEWPROFILE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionToken(userSessionToken);
				logTrack.setLogField1(inviteRequestDto.getFriendId());
				logTrack.setLatitude(inviteRequestDto.getLatitude());
				logTrack.setLongitude(inviteRequestDto.getLongitude());
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No matches Available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/invite/user", method = RequestMethod.POST, produces = "application/json")
	public void inviteAUser(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Long meetingEpochTime = inviteRequestDto.getMeetingEpochTime();
		Date convertEpochToDate = IntransitUtils.convertEpochToDate(meetingEpochTime);
		Boolean sendInviteToUser = socialLoungeService.sendInviteToUser(inviteRequestDto);
		if (sendInviteToUser.booleanValue()) {
			json.put("status", "SUCCESS");
			json.put("message", "Invitation sent successfully");
			User friend = userService.find(inviteRequestDto.getFriendId());
			User user = userService.find(inviteRequestDto.getUserId());
			Date dateTime = new Date();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("SENDINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(inviteRequestDto.getFriendId());
			userService.save(logTrack);
			List<UserNotifications> notificationsList = userService.getMatchedNotification("social", inviteRequestDto.getFriendId());
			if (notificationsList != null) {
				for (UserNotifications notify : notificationsList) {
					if (notificationsList != null && notify.getEmail().booleanValue()
							&& notify.getNotificationText().equals("When I receive a new connection request")) {
						try {
							mail.sendSocialInviteMail(String.valueOf(friend.getFirstName()) + friend.getLastName(),
									String.valueOf(user.getFirstName()) + user.getLastName(), friend.getEmail());
						} catch (Exception e) {
							System.out.println(String.valueOf(e.toString()) + "\n Mail Not Sent Successfully");
						}
					}
					if (notificationsList != null && notify.getApp() && notify.getNotificationText().equals("When I receive a new connection request")) {
						List<String> mobileList = new ArrayList<String>();
						mobileList.add(user.getFirstName() + " " + user.getLastName());
						String message = messageSource.getMessage("mobile.notification.social.invite", mobileList.toArray(), Locale.US);
						System.out.println("message======" + message);
						if (StringUtils.isNotEmpty(friend.getDeviceToken())) {
							pushNotificationService.sendPushMessageForMobile(friend.getDeviceToken(), message, friend.getDeviceType(), "Social");
						} else {
							String errorMessage = "Bloody Idiot You have Not Registered Your Device either in Apple/Goole Store";
							System.out.println("errorMessage======" + errorMessage);
							json.put("errorMessage", errorMessage);
						}
					}
				}
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/invite/user", method = RequestMethod.POST, produces = "application/json")
	public void inviteAUserV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Long meetingEpochTime = inviteRequestDto.getMeetingEpochTime();
		Date convertEpochToDate = IntransitUtils.convertEpochToDate(meetingEpochTime);
		// inviteRequestDto.setMeetingTime(convertEpochToDate);
		Boolean sendInviteToUser = socialLoungeService.sendInviteToUser(inviteRequestDto);
		if (sendInviteToUser) {
			json.put("status", "SUCCESS");
			json.put("message", "Invitation sent successfully");
			// TODO Send Push Notifications To The Receiver Of the Invite.
			User friend = userService.find(inviteRequestDto.getFriendId());
			User user = userService.find(inviteRequestDto.getUserId());

			/* its for logging track details */
			Date dateTime = new Date();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("SENDINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(inviteRequestDto.getFriendId());
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			userService.save(logTrack);

			List<UserNotifications> notificationsList = userService.getMatchedNotification("social", inviteRequestDto.getFriendId());
			if (notificationsList != null) {
				for (UserNotifications notify : notificationsList) {

					if (notificationsList != null && notify.getEmail() && notify.getNotificationText().equals("When I receive a new connection request")) {
						try {
							mail.sendSocialInviteMail(friend.getFirstName() + friend.getLastName(), user.getFirstName() + user.getLastName(), friend.getEmail());
						} catch (Exception e) {
							System.out.println(e.toString() + "\n Mail Not Sent Successfully");

						}
					}

					if (notificationsList != null && notify.getApp() && notify.getNotificationText().equals("When I receive a new connection request")) {
						List<String> mobileList = new ArrayList<String>();
						mobileList.add(user.getFirstName() + " " + user.getLastName());
						String message = messageSource.getMessage("mobile.notification.social.invite", mobileList.toArray(), Locale.US);
						System.out.println("message======" + message);
						if (StringUtils.isNotEmpty(friend.getDeviceToken())) {
							pushNotificationService.sendPushMessageForMobile(friend.getDeviceToken(), message, friend.getDeviceType(), "Social");
						} else {
							String errorMessage = "You have Not Registered Your Device either in Apple/Goole Store";
							System.out.println("errorMessage======" + errorMessage);
							json.put("errorMessage", errorMessage);
						}
					}
				}
			}

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/invite/action", method = RequestMethod.POST, produces = "application/json")
	public void doInviteAction(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Boolean success = Boolean.FALSE;
		if (StringUtils.equals(inviteRequestDto.getActionType(), "Accept")) {
			success = socialLoungeService.acceptInviteFromUser(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Accepted");
			UserConnections userConnections = socialLoungeService.find(inviteRequestDto.getUserConnectionId());
			User friend = userConnections.getSender();
			User user = userConnections.getReceiver();
			Date dateTime = new Date();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ACCEPTINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(friend.getId());
			userService.save(logTrack);
			List<UserNotifications> notificationsList = userService.getMatchedNotification("social", friend.getId());
			if (notificationsList != null) {
				for (UserNotifications notify : notificationsList) {
					if (notificationsList != null && notify.getEmail() && notify.getNotificationText().equals("When someone accepts my connection request")) {
						try {
							mail.sendSocialInviteAcceptMail(friend.getFirstName() + friend.getLastName(), user.getFirstName() + user.getLastName(),
									friend.getEmail());
						} catch (Exception e) {
							System.out.println(e.toString() + "\n Mail Not Sent Successfully");

						}
					}
					if (notificationsList != null && notify.getApp() && notify.getNotificationText().equals("When someone accepts my connection request")) {
						List<String> mobileList = new ArrayList<String>();
						mobileList.add(user.getFirstName() + " " + user.getLastName());
						String message = messageSource.getMessage("mobile.notification.social.invite.action", mobileList.toArray(), Locale.US);
						System.out.println("message======" + message);
						if (StringUtils.isNotEmpty(friend.getDeviceToken())) {
							pushNotificationService.sendPushMessageForMobile(friend.getDeviceToken(), message, friend.getDeviceType(), "Social");
						} else {
							String errorMessage = "Bloody Idiot You have Not Registered Your Device either in Apple/Goole Store";
							System.out.println("errorMessage======" + errorMessage);
							json.put("errorMessage", errorMessage);
						}
					}
				}
			}
		} else if (StringUtils.equals(inviteRequestDto.getActionType(), "Reject")) {
			success = socialLoungeService.rejectInviteFromUser(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Rejected");
			Date dateTime = new Date();
			UserConnections userConnections = socialLoungeService.find(inviteRequestDto.getUserConnectionId());
			User friend = userConnections.getSender();
			User user = userConnections.getReceiver();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("REJECTINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(friend.getId());
			userService.save(logTrack);
		} else if (StringUtils.equals(inviteRequestDto.getActionType(), "Notified")) {
			success = socialLoungeService.updateNotificationAsNotified(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Notified");
		} else if (success == Boolean.FALSE) {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/invite/action", method = RequestMethod.POST, produces = "application/json")
	public void doInviteActionV1(@RequestBody SocialInviteRequestDto inviteRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Boolean success = Boolean.FALSE;
		if (StringUtils.equals(inviteRequestDto.getActionType(), "Accept")) {
			success = socialLoungeService.acceptInviteFromUser(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Accepted");
			// TODO Send Push Notifications To The Sender Of the Invite.
			UserConnections userConnections = socialLoungeService.find(inviteRequestDto.getUserConnectionId());
			User friend = userConnections.getSender();
			User user = userConnections.getReceiver();

			/* its for logging track details */
			Date dateTime = new Date();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("ACCEPTINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(friend.getId());
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			userService.save(logTrack);

			List<UserNotifications> notificationsList = userService.getMatchedNotification("social", friend.getId());
			if (notificationsList != null) {
				for (UserNotifications notify : notificationsList) {
					if (notificationsList != null && notify.getEmail() && notify.getNotificationText().equals("When someone accepts my connection request")) {
						try {
							mail.sendSocialInviteAcceptMail(friend.getFirstName() + friend.getLastName(), user.getFirstName() + user.getLastName(),
									friend.getEmail());
						} catch (Exception e) {
							System.out.println(e.toString() + "\n Mail Not Sent Successfully");

						}
					}
					if (notificationsList != null && notify.getApp() && notify.getNotificationText().equals("When someone accepts my connection request")) {
						List<String> mobileList = new ArrayList<String>();
						mobileList.add(user.getFirstName() + " " + user.getLastName());
						String message = messageSource.getMessage("mobile.notification.social.invite.action", mobileList.toArray(), Locale.US);
						System.out.println("message======" + message);
						if (StringUtils.isNotEmpty(friend.getDeviceToken())) {
							pushNotificationService.sendPushMessageForMobile(friend.getDeviceToken(), message, friend.getDeviceType(), "Social");
						} else {
							String errorMessage = "Bloody Idiot You have Not Registered Your Device either in Apple/Goole Store";
							System.out.println("errorMessage======" + errorMessage);
							json.put("errorMessage", errorMessage);
						}
					}
				}
			}
		} else if (StringUtils.equals(inviteRequestDto.getActionType(), "Reject")) {
			success = socialLoungeService.rejectInviteFromUser(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Rejected");

			/* its for logging track details */
			Date dateTime = new Date();
			UserConnections userConnections = socialLoungeService.find(inviteRequestDto.getUserConnectionId());
			User friend = userConnections.getSender();
			User user = userConnections.getReceiver();
			String userSessionToken = user.getSessionToken();
			LoggingTrack logTrack = new LoggingTrack();
			logTrack.setUserId(inviteRequestDto.getUserId());
			logTrack.setUserLogType("REJECTINVITE");
			logTrack.setUpdateAt(dateTime);
			logTrack.setUserSessionToken(userSessionToken);
			logTrack.setLogField1(friend.getId());
			logTrack.setLatitude(inviteRequestDto.getLatitude());
			logTrack.setLongitude(inviteRequestDto.getLongitude());
			userService.save(logTrack);

		} else if (StringUtils.equals(inviteRequestDto.getActionType(), "Notified")) {
			success = socialLoungeService.updateNotificationAsNotified(inviteRequestDto);
			json.put("status", "SUCCESS");
			json.put("message", "Notified");
		} else if (success == Boolean.FALSE) {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/stores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void getStores(@RequestBody SocialInviteStoreRequestDto storeRequest, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Store> storesBasedOnUserLocationAndMerchant = locationService.getStoresBasedOnUserLocationAndMerchant(storeRequest.getLatitude(),
				storeRequest.getLongitude(), storeRequest.getIataCode(), storeRequest.getTerminalId());
		if (CollectionUtils.isNotEmpty(storesBasedOnUserLocationAndMerchant)) {
			StoreDto dto = new StoreDto(storesBasedOnUserLocationAndMerchant);
			Gson gson = new Gson();
			List<StoreDto> stores = dto.getStores();
			String json2 = gson.toJson(stores, stores.getClass());
			JSONArray jsonObject = new JSONArray(json2);
			json.put("stores", jsonObject);
		}
		json.put("status", "SUCCESS");
		json.put("message", "");
		response.setContentType("application/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/stores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void getStoresV1(@RequestBody SocialInviteStoreRequestDto storeRequest, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Store> storesBasedOnUserLocationAndMerchant = locationService.getStoresBasedOnUserLocationAndMerchant(storeRequest.getLatitude(),
				storeRequest.getLongitude(), storeRequest.getIataCode(), storeRequest.getTerminalId());
		if (CollectionUtils.isNotEmpty(storesBasedOnUserLocationAndMerchant)) {
			StoreDto dto = new StoreDto(storesBasedOnUserLocationAndMerchant);
			Gson gson = new Gson();
			List<StoreDto> stores = dto.getStores();
			String json2 = gson.toJson(stores, stores.getClass());
			JSONArray jsonObject = new JSONArray(json2);
			json.put("stores", jsonObject);
		}
		json.put("status", "SUCCESS");
		// json.put("status", "FAILURE");
		json.put("message", "");
		response.setContentType("application/json;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}
}

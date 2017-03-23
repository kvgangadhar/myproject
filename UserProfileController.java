/**
 * 
 */
package com.intransit.merchant.api.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intransit.campaignscore.utils.CampaignScoreUtils;
import com.intransit.email.MailMail;
import com.intransit.merchant.api.dto.CamapignAdsRequestDto;
import com.intransit.merchant.api.dto.CampaignScore;
import com.intransit.merchant.api.dto.CampaignScoreJson;
import com.intransit.merchant.api.dto.UserProfileDto;
import com.intransit.merchant.api.service.MobileService;
import com.intransit.merchant.api.utils.MobileUtils;
import com.intransit.merchant.api.webservice.BaseEndpoint;
import com.intransit.merchant.api.webservice.UserWrapper;
import com.intransit.merchant.helper.IntransitUtils;
import com.intransit.merchant.model.Airport;
import com.intransit.merchant.model.City;
import com.intransit.merchant.model.ClassPreference;
import com.intransit.merchant.model.Country;
import com.intransit.merchant.model.Interests;
import com.intransit.merchant.model.LoggingTrack;
import com.intransit.merchant.model.MealPreference;
import com.intransit.merchant.model.Membership;
import com.intransit.merchant.model.OccupationDemoGraphic;
import com.intransit.merchant.model.Privacy;
import com.intransit.merchant.model.SeatPreference;
import com.intransit.merchant.model.Trip;
import com.intransit.merchant.model.User;
import com.intransit.merchant.model.UserCampaign;
import com.intransit.merchant.model.UserDetails;
import com.intransit.merchant.model.UserInterests;
import com.intransit.merchant.model.UserNotifications;
import com.intransit.merchant.model.Version;
import com.intransit.merchant.service.CampaignService;
import com.intransit.merchant.service.DemoGraphicsService;
import com.intransit.merchant.service.IndustryInterestsService;
import com.intransit.merchant.service.LocationService;
import com.intransit.merchant.service.SocialLoungeService;
import com.intransit.merchant.service.TripService;
import com.intransit.merchant.service.UserService;
import com.intransit.merchant.status.UserStatus;
import com.intransit.merchant.utils.CryptoUtility;
import com.intransit.merchant.validator.UserValidator;

/**
 * @author kodelavg
 *
 */
@RestController
@RequestMapping("/api")
@Produces(value = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes(value = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class UserProfileController extends BaseEndpoint {

	private List<CampaignScoreJson> embeddedArray = new ArrayList<CampaignScoreJson>();
	private List<CampaignScoreJson> popUpArray = new ArrayList<CampaignScoreJson>();

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

	@Resource
	protected CampaignService campaignService;

	@Resource
	protected MobileService mobileService;

	@Autowired
	private UserValidator userValidator;

	@Resource
	protected IndustryInterestsService industryInterestsService;

	@Resource
	protected SocialLoungeService socialLoungeService;

	private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public void userLogin(@RequestBody User user, BindingResult result, Locale locale, TimeZone timeZone, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException, ServletException {
		logger.info("Welcome home! The client locale is {}.", locale);
		logger.info("Welcome home! The client TimeZone is {}.", timeZone + timeZone.getID());
		User matchedMobileUser = null;
		User userdata = null;
		JSONObject json = new JSONObject();
		user.setLevel("directlogin");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			matchedMobileUser = userService.getMatchedUser(user.getEmail(), CryptoUtility.encrypt(user.getPassword()));
			if (matchedMobileUser != null) {
				BigDecimal latitude = user.getLatitude();
				BigDecimal longitude = user.getLongitude();
				System.out.println("latitude====" + latitude);
				System.out.println("longitude====" + longitude);
				if (latitude != null && longitude != null) {
					List<String> initData = socialLoungeService.initAlgo(matchedMobileUser.getId(), latitude, longitude);
					System.out.println("Running init ALGO IN BACKGROUND================================");
					if (CollectionUtils.isNotEmpty(initData)) {
						userdata = userService.find(matchedMobileUser.getId());
						String userState = userdata.getUserState();
						String userAirportLocation = userdata.getCurrentAirport();
						System.out.println("user state ===" + userState);
						System.out.println("user airport ===" + userAirportLocation);
						List<UserDetails> algoUserDetails = null;
						List<UserCampaign> MatchedCampaigns = null;
						if (userdata.getGender().isEmpty()) {
							algoUserDetails = socialLoungeService.algoIosUserPerameterDetails(matchedMobileUser.getId(), userState, userAirportLocation);
						} else {
							algoUserDetails = socialLoungeService.algoUserPerameterDetails(matchedMobileUser.getId(), userState, userAirportLocation);
						}

						System.out.println("alog details of user==" + algoUserDetails);
						if (!algoUserDetails.contains(null)) {
							MatchedCampaigns = socialLoungeService.algoMatchedCampaignsForUser(algoUserDetails.get(0).getUser_age(), algoUserDetails.get(0)
									.getUser_gender(), algoUserDetails.get(0).getUser_occupation(), algoUserDetails.get(0).getUser_nationality(),
									algoUserDetails.get(0).getUser_hometown(), algoUserDetails.get(0).getTrip_travel_purpose(), algoUserDetails.get(0)
											.getTrip_destination(), algoUserDetails.get(0).getTrip_current_location(), userState);
						}
						CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
						if (CollectionUtils.isNotEmpty(MatchedCampaigns)) {
							List<CampaignScore> campaignScore = socialLoungeService.computeCampaignScore(MatchedCampaigns, matchedMobileUser.getId());
							System.out.println("*********** Before Sorting ***********");
							socialLoungeService.displayCampaignScore(campaignScore);
							System.out.println("*********** After Sorting ***********");
							campaignScore = campaignScoreUtils.sortCampaignScore(campaignScore);
							socialLoungeService.displayCampaignScore(campaignScore);
							embeddedArray = campaignScoreUtils.getEmbeddedArray(userState, campaignScore);
							popUpArray = campaignScoreUtils.getPopUpArray(userState, campaignScore);
							System.out.println("*********** EMBEDDED ARRAY BEFORE CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY BEFORE CO EFF***********");
							socialLoungeService.displayJsonArray(popUpArray);
							embeddedArray = campaignScoreUtils.computeCoeffProbability(embeddedArray);
							popUpArray = campaignScoreUtils.computeCoeffProbability(popUpArray);
							System.out.println("*********** EMBEDDED ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(popUpArray);
							System.out.println("*********** EMBEDDED ARRAY JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArray));
							System.out.println("*********** POPUP ARRAY AFTER JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(popUpArray));
							Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(campaignScoreUtils.convertArrayToJson(embeddedArray),
									userdata.getId(), userState);
							Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArray),
									userdata.getId(), userState);
							if (hasSavedEmbeddedJsonData.booleanValue() || hasSavedPopupJsonData.booleanValue()) {
								socialLoungeService.updatecampaignAlgoStatus(userdata.getId(), "publish");
							}
						} else {
							socialLoungeService.updatecampaignAlgoStatus(userdata.getId(), "no campaigns");
						}
					}
				}
				if (latitude != null && longitude != null) {
					userdata.setSessionToken(MobileUtils.generateSessionToken());
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						userdata.setDeviceToken(user.getDeviceToken());
					}
					String deviceType = user.getDeviceType();
					if (StringUtils.isEmpty(deviceType)) {
						deviceType = "android";
					}
					List<Trip> upcomingTripsWithReminder = tripService.upcomingTripsWithReminder(userdata.getId());
					JSONArray notifications = new JSONArray();
					if (CollectionUtils.isNotEmpty(upcomingTripsWithReminder)) {
						for (Trip trip : upcomingTripsWithReminder) {
							JSONObject notification = new JSONObject();
							notification.put("id", trip.getId());
							notification.put("depatureTime", trip.getDepatureTime());
							notification.put("reminder", trip.getReminder());
							notifications.put(notification);
						}
					}
					json.put("tripNotifications", notifications);
					userdata.setDeviceType(user.getDeviceType());
					userService.merge(userdata);
					json.put("id", userdata.getId());
					json.put("sessionToken", userdata.getSessionToken());
				} else {
					matchedMobileUser.setSessionToken(MobileUtils.generateSessionToken());
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						matchedMobileUser.setDeviceToken(user.getDeviceToken());
					}
					String deviceType = user.getDeviceType();
					if (StringUtils.isEmpty(deviceType)) {
						deviceType = "android";
					}
					List<Trip> upcomingTripsWithReminder = tripService.upcomingTripsWithReminder(matchedMobileUser.getId());
					JSONArray notifications = new JSONArray();
					if (CollectionUtils.isNotEmpty(upcomingTripsWithReminder)) {
						for (Trip trip : upcomingTripsWithReminder) {
							JSONObject notification = new JSONObject();
							notification.put("id", trip.getId());
							notification.put("depatureTime", trip.getDepatureTime());
							notification.put("reminder", trip.getReminder());
							notifications.put(notification);
						}
					}
					json.put("tripNotifications", notifications);
					matchedMobileUser.setDeviceType(user.getDeviceType());
					userService.merge(matchedMobileUser);
					json.put("id", matchedMobileUser.getId());
					json.put("sessionToken", matchedMobileUser.getSessionToken());
				}
				json.put("oauthProvider", user.getOauthProvider());
				json.put("status", "SUCCESS");
				Long userId = matchedMobileUser.getId();
				Date dateTime = new Date();
				String userSessionToken = userdata.getSessionToken();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(userId);
				logTrack.setUserLogType("USERLOGIN");
				logTrack.setLatitude(latitude);
				logTrack.setLongitude(longitude);
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionType(userdata.getUserState());
				logTrack.setUserSessionToken(userSessionToken);
				if (userdata.getOauthProvider().isEmpty()) {
					logTrack.setLogField2("NA");
				} else {
					logTrack.setLogField2(userdata.getOauthProvider());
				}
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Incorrect credentials");
			}
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/login", method = RequestMethod.POST)
	public void userLoginV1(@RequestBody User user, BindingResult result, Locale locale, TimeZone timeZone, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException, ServletException {
		logger.info("Welcome home! The client locale is {}.", locale);
		logger.info("Welcome home! The client TimeZone is {}.", timeZone + timeZone.getID());
		User matchedMobileUser = null;
		User userdata = null;
		JSONObject json = new JSONObject();
		user.setLevel("directlogin");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			matchedMobileUser = userService.getMatchedUser(user.getEmail(), CryptoUtility.encrypt(user.getPassword()));
			if (matchedMobileUser != null) {

				// List<String> initData =
				// socialLoungeService.initAlgo(matchedMobileUser.getId(), new
				// BigDecimal(12.9811449), new BigDecimal(80.2514914));
				BigDecimal latitude = user.getLatitude();
				BigDecimal longitude = user.getLongitude();
				System.out.println("latitude====" + latitude);
				System.out.println("longitude====" + longitude);
				if (latitude != null && longitude != null) {
					List<String> initData = socialLoungeService.initAlgo(matchedMobileUser.getId(), latitude, longitude);
					System.out.println("Running init ALGO IN BACKGROUND================================");
					// socialLoungeService.runCamapignAlgo(matchedMobileUser.getId(),
					// latitude, longitude);

					if (CollectionUtils.isNotEmpty(initData)) {
						userdata = userService.find(matchedMobileUser.getId());
						String userState = userdata.getUserState();
						String userAirportLocation = userdata.getCurrentAirport();
						System.out.println("user state ===" + userState);
						System.out.println("user airport ===" + userAirportLocation);
						List<UserDetails> algoUserDetails = null;
						List<UserCampaign> MatchedCampaigns = null;

						algoUserDetails = socialLoungeService.algoUserPerameterDetails(matchedMobileUser.getId(), userState, userAirportLocation);

						System.out.println("alog details of user==" + algoUserDetails);

						// System.out.println("alog details of user travel purpose=="
						// + algoUserDetails.get(0).getTrip_travel_purpose());
						if (!algoUserDetails.contains(null)) {

							MatchedCampaigns = socialLoungeService.algoMatchedCampaignsForUser(algoUserDetails.get(0).getUser_age(), algoUserDetails.get(0)
									.getUser_gender(), algoUserDetails.get(0).getUser_occupation(), algoUserDetails.get(0).getUser_nationality(),
									algoUserDetails.get(0).getUser_hometown(), algoUserDetails.get(0).getTrip_travel_purpose(), algoUserDetails.get(0)
											.getTrip_destination(), algoUserDetails.get(0).getTrip_current_location(), userState);
						}

						/*
						 * for (UserCampaign campaigns : MatchedCampaigns) {
						 * Boolean isCampaignEligible =
						 * socialLoungeService.isCampaignEligibleForSelection
						 * (campaigns.getCampaignid(),
						 * matchedMobileUser.getId());
						 * System.out.println("campid====" +
						 * isCampaignEligible); System.out.println("campid===="
						 * + campaigns.getCampaignid());
						 * System.out.println("type===" +
						 * campaigns.getCampaigntypesd_campaigntypeid());
						 * System.out.println("style-===" +
						 * campaigns.getDisplay_style()); }
						 */
						CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
						if (CollectionUtils.isNotEmpty(MatchedCampaigns)) {
							List<CampaignScore> campaignScore = socialLoungeService.computeCampaignScore(MatchedCampaigns, matchedMobileUser.getId());

							System.out.println("*********** Before Sorting ***********");
							socialLoungeService.displayCampaignScore(campaignScore);
							System.out.println("*********** After Sorting ***********");
							campaignScore = campaignScoreUtils.sortCampaignScore(campaignScore);
							socialLoungeService.displayCampaignScore(campaignScore);
							embeddedArray = campaignScoreUtils.getEmbeddedArray(userState, campaignScore);
							popUpArray = campaignScoreUtils.getPopUpArray(userState, campaignScore);
							System.out.println("*********** EMBEDDED ARRAY BEFORE CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY BEFORE CO EFF***********");

							socialLoungeService.displayJsonArray(popUpArray);

							embeddedArray = campaignScoreUtils.computeCoeffProbability(embeddedArray);
							popUpArray = campaignScoreUtils.computeCoeffProbability(popUpArray);

							System.out.println("*********** EMBEDDED ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(popUpArray);

							System.out.println("*********** EMBEDDED ARRAY JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArray));

							System.out.println("*********** POPUP ARRAY AFTER JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(popUpArray));

							Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(campaignScoreUtils.convertArrayToJson(embeddedArray),
									userdata.getId(), userState);

							Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArray),
									userdata.getId(), userState);
							if (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
								socialLoungeService.updatecampaignAlgoStatus(userdata.getId(), "publish");
							}
						} else {
							socialLoungeService.updatecampaignAlgoStatus(userdata.getId(), "no campaigns");
						}

					}

				}

				if (latitude != null && longitude != null) {
					userdata.setSessionToken(MobileUtils.generateSessionToken());
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						userdata.setDeviceToken(user.getDeviceToken());
					}
					String deviceType = user.getDeviceType();
					if (StringUtils.isEmpty(deviceType)) {
						deviceType = "android";
					}
					List<Trip> upcomingTripsWithReminder = tripService.upcomingTripsWithReminder(userdata.getId());
					JSONArray notifications = new JSONArray();
					if (CollectionUtils.isNotEmpty(upcomingTripsWithReminder)) {
						for (Trip trip : upcomingTripsWithReminder) {
							JSONObject notification = new JSONObject();
							notification.put("id", trip.getId());
							notification.put("depatureTime", trip.getLocalDepatureTime());
							notification.put("reminder", trip.getReminder());
							notifications.put(notification);
						}
					}
					json.put("tripNotifications", notifications);
					userdata.setDeviceType(user.getDeviceType());
					userService.merge(userdata);
					json.put("id", userdata.getId());
					json.put("sessionToken", userdata.getSessionToken());
					json.put("profilePic", userdata.getFormedProfileUrl());
					if (userdata.getGender() != null) {
						json.put("gender", userdata.getGender());
					} else {
						json.put("gender", "");
					}
					if (userdata.getDateofBirth() != null) {
						json.put("dob", userdata.getDateofBirth());
					} else {
						json.put("dob", "");
					}
					if (userdata.getHomeTown() != null) {
						json.put("homeTown", userdata.getHomeTown().getName());
					} else {
						json.put("homeTown", "");
					}

					if (userdata.getNationality() != null) {
						json.put("nationality", userdata.getNationality().getName());
					} else {
						json.put("nationality", "");
					}

					if (userdata.getOccupation() != null) {
						json.put("occupation", userdata.getOccupation().getValue());
					} else {
						json.put("occupation", "");
					}

				} else {
					matchedMobileUser.setSessionToken(MobileUtils.generateSessionToken());
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						matchedMobileUser.setDeviceToken(user.getDeviceToken());
					}
					String deviceType = user.getDeviceType();
					if (StringUtils.isEmpty(deviceType)) {
						deviceType = "android";
					}
					List<Trip> upcomingTripsWithReminder = tripService.upcomingTripsWithReminder(matchedMobileUser.getId());
					JSONArray notifications = new JSONArray();
					if (CollectionUtils.isNotEmpty(upcomingTripsWithReminder)) {
						for (Trip trip : upcomingTripsWithReminder) {
							JSONObject notification = new JSONObject();
							notification.put("id", trip.getId());
							notification.put("depatureTime", trip.getDepatureTime());
							notification.put("reminder", trip.getReminder());
							notifications.put(notification);
						}
					}
					json.put("tripNotifications", notifications);
					matchedMobileUser.setDeviceType(user.getDeviceType());
					userService.merge(matchedMobileUser);
					json.put("id", matchedMobileUser.getId());
					json.put("sessionToken", matchedMobileUser.getSessionToken());
					json.put("profilePic", matchedMobileUser.getFormedProfileUrl());
					if (matchedMobileUser.getGender() != null) {
						json.put("gender", matchedMobileUser.getGender());
					} else {
						json.put("gender", "");
					}
					if (userdata.getHomeTown() != null) {
						json.put("homeTown", userdata.getHomeTown().getName());
					} else {
						json.put("homeTown", "");
					}

					if (userdata.getNationality() != null) {
						json.put("nationality", userdata.getNationality().getName());
					} else {
						json.put("nationality", "");
					}

					if (userdata.getOccupation() != null) {
						json.put("occupation", userdata.getOccupation().getValue());
					} else {
						json.put("occupation", "");
					}
				}
				json.put("oauthProvider", user.getOauthProvider());
				json.put("status", "SUCCESS");

				/* its for logging track details */
				Long userId = matchedMobileUser.getId();
				Date dateTime = new Date();
				String userSessionToken = matchedMobileUser.getSessionToken();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(userId);
				logTrack.setUserLogType("USERLOGIN");
				logTrack.setLatitude(latitude);
				logTrack.setLongitude(longitude);
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionType(userdata.getUserState());
				logTrack.setUserSessionToken(userSessionToken);
				if (userdata.getOauthProvider().isEmpty()) {
					logTrack.setLogField2("NA");
				} else {
					logTrack.setLogField2(userdata.getOauthProvider());
				}
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Incorrect credentials");
			}
		}

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public void userLogout(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		User mobileUser = null;
		JSONObject json = new JSONObject();
		mobileUser = userService.find(user.getId());
		if (mobileUser != null) {
			if (StringUtils.equals(mobileUser.getSessionToken(), user.getSessionToken())) {
				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(mobileUser.getId());
				logTrack.setUserLogType("USERLOGOUT");
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionToken(mobileUser.getSessionToken());
				userService.save(logTrack);
				mobileUser.setSessionToken(null);
				mobileUser.setDeviceToken(null);
				mobileUser.setUserState(null);
				mobileUser.setCurrentAirport(null);
				mobileUser.setCampaignAlgoStatus(null);
				userService.update(mobileUser);
				socialLoungeService.clearCampaignAdsForTheUser(mobileUser.getId());
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Invalid Session Token");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "Incorrect credentials");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/logout", method = RequestMethod.POST)
	public void userLogoutV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		User mobileUser = null;
		JSONObject json = new JSONObject();
		mobileUser = userService.find(user.getId());
		if (mobileUser != null) {
			if (StringUtils.equals(mobileUser.getSessionToken(), user.getSessionToken())) {

				/* its for logging track details */
				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserId(mobileUser.getId());
				logTrack.setUserLogType("USERLOGOUT");
				logTrack.setUpdateAt(dateTime);
				logTrack.setUserSessionToken(mobileUser.getSessionToken());
				logTrack.setLatitude(user.getLatitude());
				logTrack.setLongitude(user.getLongitude());
				userService.save(logTrack);

				/* its for logging track details */

				mobileUser.setSessionToken(null);
				mobileUser.setDeviceToken(null);
				mobileUser.setUserState(null);
				mobileUser.setCurrentAirport(null);
				mobileUser.setCampaignAlgoStatus(null);
				userService.update(mobileUser);
				socialLoungeService.clearCampaignAdsForTheUser(mobileUser.getId());
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Invalid Session Token");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "Incorrect credentials");
		}

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/login/social", method = RequestMethod.POST, produces = "application/json")
	public void socialLogin(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User checkEmail = null;
		User userdata = null;
		user.setLevel("sociallogin");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail == null) {
				json.put("status", "FAILURE");
				json.put("message", "Email Not Registered Already ");
			} else {
				BigDecimal latitude = user.getLatitude();
				BigDecimal longitude = user.getLongitude();
				System.out.println("latitude====" + latitude);
				System.out.println("longitude====" + longitude);
				if (latitude != null && longitude != null) {
					List<String> initData = socialLoungeService.initAlgo(checkEmail.getId(), latitude, longitude);
					System.out.println("Running init ALGO IN BACKGROUND================================");
					// socialLoungeService.runCamapignAlgo(matchedMobileUser.getId(),
					// latitude, longitude);

					if (CollectionUtils.isNotEmpty(initData)) {
						userdata = userService.find(checkEmail.getId());
						String userState = userdata.getUserState();
						String userAirportLocation = userdata.getCurrentAirport();
						System.out.println("user state ===" + userState);
						System.out.println("user airport ===" + userAirportLocation);

						List<UserDetails> algoUserDetails = null;
						List<UserCampaign> MatchedCampaigns = null;

						if (userdata.getGender().isEmpty()) {
							algoUserDetails = socialLoungeService.algoIosUserPerameterDetails(checkEmail.getId(), userState, userAirportLocation);

						} else {

							algoUserDetails = socialLoungeService.algoUserPerameterDetails(checkEmail.getId(), userState, userAirportLocation);

						}
						System.out.println("alog details of user==" + algoUserDetails);

						// System.out.println("alog details of user travel purpose=="
						// +
						// algoUserDetails.get(0).getTrip_travel_purpose());
						if (!algoUserDetails.contains(null)) {

							/*
							 * List<UserDetails> algoUserDetails =
							 * socialLoungeService
							 * .algoUserPerameterDetails(checkEmail.getId(),
							 * userState, userAirportLocation);
							 */

							MatchedCampaigns = socialLoungeService.algoMatchedCampaignsForUser(algoUserDetails.get(0).getUser_age(), algoUserDetails.get(0)
									.getUser_gender(), algoUserDetails.get(0).getUser_occupation(), algoUserDetails.get(0).getUser_nationality(),
									algoUserDetails.get(0).getUser_hometown(), algoUserDetails.get(0).getTrip_travel_purpose(), algoUserDetails.get(0)
											.getTrip_destination(), algoUserDetails.get(0).getTrip_current_location(), userState);
						}
						/*
						 * for (UserCampaign campaigns : MatchedCampaigns) {
						 * Boolean isCampaignEligible =
						 * socialLoungeService.isCampaignEligibleForSelection
						 * (campaigns.getCampaignid(),
						 * matchedMobileUser.getId());
						 * System.out.println("campid====" +
						 * isCampaignEligible); System.out.println("campid===="
						 * + campaigns.getCampaignid());
						 * System.out.println("type===" +
						 * campaigns.getCampaigntypesd_campaigntypeid());
						 * System.out.println("style-===" +
						 * campaigns.getDisplay_style()); }
						 */
						CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
						if (CollectionUtils.isNotEmpty(MatchedCampaigns)) {
							List<CampaignScore> campaignScore = socialLoungeService.computeCampaignScore(MatchedCampaigns, checkEmail.getId());

							System.out.println("*********** Before Sorting ***********");
							socialLoungeService.displayCampaignScore(campaignScore);
							System.out.println("*********** After Sorting ***********");
							campaignScore = campaignScoreUtils.sortCampaignScore(campaignScore);
							socialLoungeService.displayCampaignScore(campaignScore);
							embeddedArray = campaignScoreUtils.getEmbeddedArray(userState, campaignScore);
							popUpArray = campaignScoreUtils.getPopUpArray(userState, campaignScore);
							System.out.println("*********** EMBEDDED ARRAY BEFORE CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY BEFORE CO EFF***********");

							socialLoungeService.displayJsonArray(popUpArray);

							embeddedArray = campaignScoreUtils.computeCoeffProbability(embeddedArray);
							popUpArray = campaignScoreUtils.computeCoeffProbability(popUpArray);

							System.out.println("*********** EMBEDDED ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(embeddedArray);
							System.out.println("*********** POPUP ARRAY AFTER CO EFF***********");
							socialLoungeService.displayJsonArray(popUpArray);

							System.out.println("*********** EMBEDDED ARRAY JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArray));

							System.out.println("*********** POPUP ARRAY AFTER JSON***********");
							System.out.println(campaignScoreUtils.convertArrayToJson(popUpArray));

							Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(campaignScoreUtils.convertArrayToJson(embeddedArray),
									checkEmail.getId(), userState);

							Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArray),
									checkEmail.getId(), userState);
							if (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
								socialLoungeService.updatecampaignAlgoStatus(checkEmail.getId(), "publish");
							}
						} else {
							socialLoungeService.updatecampaignAlgoStatus(checkEmail.getId(), "no campaigns");
						}

					}

				}

				checkEmail.setSessionToken(MobileUtils.generateSessionToken());
				userService.update(checkEmail);
				json.put("id", checkEmail.getId());
				json.put("sessionToken", checkEmail.getSessionToken());
				json.put("oauthProvider", user.getOauthProvider());
				json.put("Uid", checkEmail.getOauthUid());
				json.put("status", "SUCCESS");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "Incorrect credentials");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/login/social", method = RequestMethod.POST, produces = "application/json")
	public void socialLoginV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User checkEmail = null;
		User userdata = null;
		user.setLevel("sociallogin");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			if (user.getEmail() != null) {
				checkEmail = userService.checkByEmail(user.getEmail());
				if (checkEmail == null) {
					json.put("status", "FAILURE");
					json.put("message", "Email Not Registered Already ");
				} else {

					BigDecimal latitude = user.getLatitude();
					BigDecimal longitude = user.getLongitude();
					System.out.println("latitude====" + latitude);
					System.out.println("longitude====" + longitude);
					if (latitude != null && longitude != null) {
						List<String> initData = socialLoungeService.initAlgo(checkEmail.getId(), latitude, longitude);
						System.out.println("Running init ALGO IN BACKGROUND================================");
						// socialLoungeService.runCamapignAlgo(matchedMobileUser.getId(),
						// latitude, longitude);

						if (CollectionUtils.isNotEmpty(initData)) {
							userdata = userService.find(checkEmail.getId());
							String userState = userdata.getUserState();
							String userAirportLocation = userdata.getCurrentAirport();
							System.out.println("user state ===" + userState);
							System.out.println("user airport ===" + userAirportLocation);

							List<UserDetails> algoUserDetails = null;
							List<UserCampaign> MatchedCampaigns = null;

							if (userdata.getGender().isEmpty()) {
								algoUserDetails = socialLoungeService.algoIosUserPerameterDetails(checkEmail.getId(), userState, userAirportLocation);

							} else {

								algoUserDetails = socialLoungeService.algoUserPerameterDetails(checkEmail.getId(), userState, userAirportLocation);

							}
							System.out.println("alog details of user==" + algoUserDetails);

							// System.out.println("alog details of user travel purpose=="
							// +
							// algoUserDetails.get(0).getTrip_travel_purpose());
							if (!algoUserDetails.contains(null)) {

								/*
								 * List<UserDetails> algoUserDetails =
								 * socialLoungeService
								 * .algoUserPerameterDetails(checkEmail.getId(),
								 * userState, userAirportLocation);
								 */

								MatchedCampaigns = socialLoungeService.algoMatchedCampaignsForUser(algoUserDetails.get(0).getUser_age(), algoUserDetails.get(0)
										.getUser_gender(), algoUserDetails.get(0).getUser_occupation(), algoUserDetails.get(0).getUser_nationality(),
										algoUserDetails.get(0).getUser_hometown(), algoUserDetails.get(0).getTrip_travel_purpose(), algoUserDetails.get(0)
												.getTrip_destination(), algoUserDetails.get(0).getTrip_current_location(), userState);
							}
							/*
							 * for (UserCampaign campaigns : MatchedCampaigns) {
							 * Boolean isCampaignEligible =
							 * socialLoungeService.isCampaignEligibleForSelection
							 * (campaigns.getCampaignid(),
							 * matchedMobileUser.getId());
							 * System.out.println("campid====" +
							 * isCampaignEligible);
							 * System.out.println("campid====" +
							 * campaigns.getCampaignid());
							 * System.out.println("type===" +
							 * campaigns.getCampaigntypesd_campaigntypeid());
							 * System.out.println("style-===" +
							 * campaigns.getDisplay_style()); }
							 */
							CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
							if (CollectionUtils.isNotEmpty(MatchedCampaigns)) {
								List<CampaignScore> campaignScore = socialLoungeService.computeCampaignScore(MatchedCampaigns, checkEmail.getId());

								System.out.println("*********** Before Sorting ***********");
								socialLoungeService.displayCampaignScore(campaignScore);
								System.out.println("*********** After Sorting ***********");
								campaignScore = campaignScoreUtils.sortCampaignScore(campaignScore);
								socialLoungeService.displayCampaignScore(campaignScore);
								embeddedArray = campaignScoreUtils.getEmbeddedArray(userState, campaignScore);
								popUpArray = campaignScoreUtils.getPopUpArray(userState, campaignScore);
								System.out.println("*********** EMBEDDED ARRAY BEFORE CO EFF***********");
								socialLoungeService.displayJsonArray(embeddedArray);
								System.out.println("*********** POPUP ARRAY BEFORE CO EFF***********");

								socialLoungeService.displayJsonArray(popUpArray);

								embeddedArray = campaignScoreUtils.computeCoeffProbability(embeddedArray);
								popUpArray = campaignScoreUtils.computeCoeffProbability(popUpArray);

								System.out.println("*********** EMBEDDED ARRAY AFTER CO EFF***********");
								socialLoungeService.displayJsonArray(embeddedArray);
								System.out.println("*********** POPUP ARRAY AFTER CO EFF***********");
								socialLoungeService.displayJsonArray(popUpArray);

								System.out.println("*********** EMBEDDED ARRAY JSON***********");
								System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArray));

								System.out.println("*********** POPUP ARRAY AFTER JSON***********");
								System.out.println(campaignScoreUtils.convertArrayToJson(popUpArray));

								Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(
										campaignScoreUtils.convertArrayToJson(embeddedArray), checkEmail.getId(), userState);

								Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArray),
										checkEmail.getId(), userState);
								if (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
									socialLoungeService.updatecampaignAlgoStatus(checkEmail.getId(), "publish");
								}
							} else {
								socialLoungeService.updatecampaignAlgoStatus(checkEmail.getId(), "no campaigns");
							}

						}

					}

					checkEmail.setSessionToken(MobileUtils.generateSessionToken());
					userService.update(checkEmail);
					json.put("id", checkEmail.getId());
					json.put("sessionToken", checkEmail.getSessionToken());
					json.put("oauthProvider", user.getOauthProvider());
					json.put("Uid", checkEmail.getOauthUid());
					json.put("status", "SUCCESS");
				}
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Incorrect credentials");
			}
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/forgot/password", method = RequestMethod.POST, produces = "application/json")
	public void apiForGotPassword(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail == null) {
				json.put("status", "FAILURE");
				json.put("message", "Email Not Registered Already");
			} else {
				checkEmail.setSessionToken(MobileUtils.generateSessionToken());
				checkEmail.setTemporaryPassword(IntransitUtils.getRandomNumber(6).toString());
				try {
					System.out.println("context path======================" + request.getContextPath());
					mail.sendForgotPasswordMail(String.valueOf(checkEmail.getFirstName()) + checkEmail.getLastName(), checkEmail.getTemporaryPassword(),
							checkEmail.getEmail());
					userService.update(checkEmail);
					json.put("status", "SUCCESS");
					json.put("sessionToken", checkEmail.getSessionToken());
				} catch (Exception e) {
					json.put("status", "FAILURE");
					json.put("message", "Not a Valid Email");
				}
			}
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/forgot/password", method = RequestMethod.POST, produces = "application/json")
	public void apiForGotPasswordV1(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail == null) {
				json.put("status", "FAILURE");
				json.put("message", "Email Not Registered Already");
			} else {
				checkEmail.setSessionToken(MobileUtils.generateSessionToken());
				checkEmail.setTemporaryPassword(IntransitUtils.getRandomNumber(6).toString());
				try {
					System.out.println("context path======================" + request.getContextPath());
					mail.sendForgotPasswordMail(checkEmail.getFirstName() + checkEmail.getLastName(), checkEmail.getTemporaryPassword(), checkEmail.getEmail());
					userService.update(checkEmail);
					json.put("status", "SUCCESS");
					json.put("sessionToken", checkEmail.getSessionToken());
				} catch (Exception e) {
					json.put("status", "FAILURE");
					json.put("message", "Not a Valid Email");
				}

			}
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/reset/password", method = RequestMethod.POST, produces = "application/json")
	public void apiForResetPassword(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException,
			ServletException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail == null) {
				json.put("status", "FAILURE");
				json.put("message", "Email Not Registered Already");
			} else if (StringUtils.equals(user.getTemporaryPassword(), checkEmail.getTemporaryPassword())) {
				if (StringUtils.equals(user.getSessionToken(), checkEmail.getSessionToken())) {
					checkEmail.setPassword(CryptoUtility.encrypt(user.getPassword()));
					userService.update(checkEmail);
					json.put("status", "SUCCESS");
					json.put("id", checkEmail.getId());
					json.put("sessionToken", checkEmail.getSessionToken());
					json.put("oauthProvider", checkEmail.getOauthProvider());
				} else {
					json.put("status", "FAILURE");
					json.put("message", "Session Expired");
				}
			} else {
				json.put("status", "FAILURE");
				json.put("message", "Temporary Password Not Matched");
			}
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/reset/password", method = RequestMethod.POST, produces = "application/json")
	public void apiForResetPasswordV1(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException,
			ServletException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail == null) {
				json.put("status", "FAILURE");
				json.put("message", "Email Not Registered Already");
			} else {
				if (StringUtils.equals(user.getTemporaryPassword(), checkEmail.getTemporaryPassword())) {
					if (StringUtils.equals(user.getSessionToken(), checkEmail.getSessionToken())) {
						checkEmail.setPassword(CryptoUtility.encrypt(user.getPassword()));
						if (user.getDeviceToken() != null) {
							checkEmail.setDeviceToken(user.getDeviceToken());
						}
						if (user.getOauthProvider() != null) {
							checkEmail.setOauthProvider(user.getOauthProvider());
						}
						userService.update(checkEmail);
						json.put("status", "SUCCESS");
						json.put("id", checkEmail.getId());
						json.put("sessionToken", checkEmail.getSessionToken());
						json.put("oauthProvider", checkEmail.getOauthProvider());
					} else {
						json.put("status", "FAILURE");
						json.put("message", "Session Expired");
					}
				} else {
					json.put("status", "FAILURE");
					json.put("message", "Temporary Password Not Matched");
				}
			}
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/register", method = RequestMethod.POST, produces = "application/json")
	public void apiUserRegister(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException, ServletException, MessagingException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		user.setLevel("register");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else if (user.getEmail() != null) {
			checkEmail = userService.checkByEmail(user.getEmail());
			if (checkEmail != null) {
				json.put("status", "FAILURE");
				json.put("message", "Email already registered");
			} else {
				long all = 1;
				long connections = 2;
				long fav = 3;
				long me = 4;
				City hometown = locationService.findCityById(user.getHometownId());
				OccupationDemoGraphic occupation = demoGraphicsService.findOccupationById(user.getOccupationId());
				Country nationality = locationService.findCountryById(user.getNationalityId());
				Privacy aboutmeprivacy = userService.findPrivacyById(Long.valueOf(all));
				Privacy dobprivacy = userService.findPrivacyById(Long.valueOf(all));
				Privacy genderprivacy = userService.findPrivacyById(Long.valueOf(all));
				Privacy hometownprivacy = userService.findPrivacyById(Long.valueOf(all));
				Privacy occupationprivacy = userService.findPrivacyById(Long.valueOf(connections));
				Privacy seatprivacy = userService.findPrivacyById(Long.valueOf(fav));
				Privacy mealprivacy = userService.findPrivacyById(Long.valueOf(me));
				Privacy classprivacy = userService.findPrivacyById(Long.valueOf(fav));
				Privacy interestsPrivacy = userService.findPrivacyById(Long.valueOf(connections));
				Privacy membershipsPrivacy = userService.findPrivacyById(Long.valueOf(me));
				if (hometown != null && occupation != null && nationality != null) {
					user.setHomeTown(hometown);
					user.setNationality(nationality);
					user.setOccupation(occupation);
					user.setSessionToken(MobileUtils.generateSessionToken());
					user.setAboutmePrivacy(aboutmeprivacy);
					user.setDobPrivacy(dobprivacy);
					user.setGenderPrivacy(genderprivacy);
					user.setHometownPrivacy(hometownprivacy);
					user.setOccupationPrivacy(occupationprivacy);
					user.setSeatPrivacy(seatprivacy);
					user.setMealPrivacy(mealprivacy);
					user.setClassPrivacy(classprivacy);
					user.setInterestsPrivacy(interestsPrivacy);
					user.setMembershipsPrivacy(membershipsPrivacy);
					user.setStatus(UserStatus.ACTIVE);
					user.setPassword(CryptoUtility.encrypt(user.getPassword()));
					user.setIsSubscribe(user.getIsSubscribe());
					userService.save(user);
					mail.sendMailForUser(String.valueOf(user.getFirstName()) + " " + user.getLastName(), user.getEmail());
					json.put("status", "SUCCESS");
					json.put("id", user.getId());
					json.put("sessionToken", user.getSessionToken());
					json.put("oauthProvider", user.getOauthProvider());
				} else {
					json.put("message", "Invalid Details");
				}
			}
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/register", method = RequestMethod.POST, produces = "application/json")
	public void apiUserRegisterV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException, ServletException, MessagingException {
		JSONObject json = new JSONObject();
		System.out.println("===================got controller for api");
		User checkEmail = null;
		user.setLevel("register");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			if (user.getEmail() != null) {
				checkEmail = userService.checkByEmail(user.getEmail());
				if (checkEmail != null) {
					json.put("status", "FAILURE");
					json.put("message", "Email already registered");
				} else {
					long all = 1;
					long connections = 2;
					long fav = 3;
					long me = 4;
					City hometown = locationService.findCityById(user.getHometownId());
					OccupationDemoGraphic occupation = demoGraphicsService.findOccupationById(user.getOccupationId());
					Country nationality = locationService.findCountryById(user.getNationalityId());
					Privacy aboutmeprivacy = userService.findPrivacyById(all);
					Privacy dobprivacy = userService.findPrivacyById(all);
					Privacy genderprivacy = userService.findPrivacyById(all);
					Privacy hometownprivacy = userService.findPrivacyById(all);
					Privacy occupationprivacy = userService.findPrivacyById(connections);
					Privacy seatprivacy = userService.findPrivacyById(fav);
					Privacy mealprivacy = userService.findPrivacyById(me);
					Privacy classprivacy = userService.findPrivacyById(fav);
					Privacy interestsPrivacy = userService.findPrivacyById(connections);
					Privacy membershipsPrivacy = userService.findPrivacyById(me);
					if (hometown != null || occupation != null || nationality != null) {
						user.setHomeTown(hometown);
						user.setNationality(nationality);
						user.setOccupation(occupation);
						user.setSessionToken(MobileUtils.generateSessionToken());
						user.setAboutmePrivacy(aboutmeprivacy);
						user.setDobPrivacy(dobprivacy);
						user.setGenderPrivacy(genderprivacy);
						user.setHometownPrivacy(hometownprivacy);
						user.setOccupationPrivacy(occupationprivacy);
						user.setSeatPrivacy(seatprivacy);
						user.setMealPrivacy(mealprivacy);
						user.setClassPrivacy(classprivacy);
						user.setInterestsPrivacy(interestsPrivacy);
						user.setMembershipsPrivacy(membershipsPrivacy);
						user.setStatus(UserStatus.ACTIVE);
						user.setPassword(CryptoUtility.encrypt(user.getPassword()));
						user.setIsSubscribe(user.getIsSubscribe());
						user.setDeviceToken(user.getDeviceToken());
						userService.save(user);
						try {
							mail.sendMailForUser(user.getFirstName() + " " + user.getLastName(), user.getEmail());
						} catch (Exception e) {
							e.printStackTrace();
						}
						json.put("status", "SUCCESS");
						json.put("id", user.getId());
						json.put("sessionToken", user.getSessionToken());
						json.put("oauthProvider", user.getOauthProvider());
					} else {
						user.setSessionToken(MobileUtils.generateSessionToken());
						user.setStatus(UserStatus.ACTIVE);
						user.setPassword(CryptoUtility.encrypt(user.getPassword()));
						user.setIsSubscribe(user.getIsSubscribe());
						user.setDeviceToken(user.getDeviceToken());
						userService.save(user);
						mail.sendMailForUser(user.getFirstName() + " " + user.getLastName(), user.getEmail());
						json.put("status", "SUCCESS");
						json.put("id", user.getId());
						json.put("sessionToken", user.getSessionToken());
						json.put("oauthProvider", user.getOauthProvider());
					}
				}
			} else {
				json.put("status", "FAILURE");
			}
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/welcomeuserupdate", method = RequestMethod.POST, produces = "application/json")
	public void apiWelcomeUserUpdate(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userupdate = userService.find(user.getId());
		City hometown = locationService.findCityById(user.getHometownId());
		OccupationDemoGraphic occupation = demoGraphicsService.findOccupationById(user.getOccupationId());
		Country nationality = locationService.findCountryById(user.getNationalityId());
		if (userupdate != null) {
			if (StringUtils.isNotBlank(user.getGender())) {
				userupdate.setGender(user.getGender());
			}
			if (user.getDateofBirth() != null) {
				userupdate.setDateofBirth(user.getDateofBirth());
			}
			if (hometown != null) {
				userupdate.setHomeTown(hometown);
			} else {
				userupdate.setHomeTown(null);
			}
			if (occupation != null) {
				userupdate.setOccupation(occupation);
			} else {
				userupdate.setOccupation(null);
			}
			if (nationality != null) {
				userupdate.setNationality(nationality);
			} else {
				userupdate.setNationality(null);
			}
			userService.update(userupdate);
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "user id is not matched");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userupdate", method = RequestMethod.POST, produces = "application/json")
	public void apiUserDataUpdate(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		System.out.println("File Uploaded===" + user.getProfilePicFile());
		JSONObject json = new JSONObject();
		user.setLevel("update");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			Privacy aboutmeprivacy = userService.findPrivacyById(user.getAboutmePrivacyId());
			Privacy dobprivacy = userService.findPrivacyById(user.getDobPrivacyId());
			Privacy emailprivacy = userService.findPrivacyById(user.getEmailPrivacyId());
			Privacy genderprivacy = userService.findPrivacyById(user.getGenderPrivacyId());
			Privacy hometownprivacy = userService.findPrivacyById(user.getHometownPrivacyId());
			Privacy occupationprivacy = userService.findPrivacyById(user.getOccupationPrivacyId());
			Privacy nationalityprivacy = userService.findPrivacyById(user.getNationalityPrivacyId());
			Privacy seatprivacy = userService.findPrivacyById(user.getSeatPrivacyId());
			Privacy mealprivacy = userService.findPrivacyById(user.getMealPrivacyId());
			Privacy classprivacy = userService.findPrivacyById(user.getClassPrivacyId());
			Privacy interestsprivacy = userService.findPrivacyById(user.getInterestsPrivacyId());
			Privacy membershipsprivacy = userService.findPrivacyById(user.getMembershipsPrivacyId());
			SeatPreference seatPreference = userService.findSeatPreferenceById(user.getSeatPreferenceId());
			MealPreference mealPreference = userService.findMealPreferenceById(user.getMealPreferenceId());
			ClassPreference classPreference = userService.findClassPreferenceById(user.getClassPreferenceId());
			User userupdate = userService.find(user.getId());
			City hometown = locationService.findCityById(user.getHometownId());
			OccupationDemoGraphic occupation = demoGraphicsService.findOccupationById(user.getOccupationId());
			Country nationality = locationService.findCountryById(user.getNationalityId());
			if (userupdate != null) {
				List<Long> memberships = user.getMemberships();
				List<String> membershipsNames = user.getMembershipsNames();
				if (memberships == null) {
					memberships = new ArrayList<Long>();
				}
				if (CollectionUtils.isNotEmpty(memberships) && memberships.size() > 5) {
					json.put("errormessage", "You can have five memberships only");
					json.put("Status", "failure");
				} else {
					if (hometown != null && occupation != null && nationality != null) {
						userupdate.setFirstName(user.getFirstName());
						userupdate.setLastName(user.getLastName());
						userupdate.setDateofBirth(user.getDateofBirth());
						userupdate.setAboutMe(user.getAboutMe());
						userupdate.setGender(user.getGender());
						userupdate.setHomeTown(hometown);
						userupdate.setNationality(nationality);
						userupdate.setOccupation(occupation);
					}
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						userupdate.setDeviceToken(user.getDeviceToken());
					}
					List<Interests> interests = user.getInterests();
					ArrayList<Long> interestIds = new ArrayList<Long>();
					if (CollectionUtils.isNotEmpty(interests)) {
						for (Interests interest : interests) {
							if (interest.getId() != null) {
								interestIds.add(interest.getId());
							}
						}
					}
					List<UserInterests> existedInterestsList = userService.getInterestsByUserId(user.getId());
					if (CollectionUtils.isNotEmpty(existedInterestsList)) {
						for (int i = 0; i < existedInterestsList.size(); i++) {
							System.out.println(interests);
							if (interestIds.contains(existedInterestsList.get(i).getInterest().getId()) == Boolean.FALSE) {
								UserInterests existeduserInterests = userService.getMatchedInterest(existedInterestsList.get(i).getInterest().getId(),
										user.getId());
								if (existeduserInterests != null) {
									userService.removeUserInterestById(existeduserInterests);
								}
							}
						}
					}
					if (CollectionUtils.isNotEmpty(interests)) {
						for (Interests interest : interests) {
							UserInterests userInterests = new UserInterests();
							Interests interestid = userService.findInterestById(interest.getId());
							User userid = userService.find(user.getId());
							UserInterests userInterestsList = userService.getMatchedInterest(interest.getId(), user.getId());
							if (interestid != null && userInterestsList == null) {
								userInterests.setInterest(interestid);
								userInterests.setUser(userid);
								userService.saveUserInterests(userInterests);
							}
						}
					}

					List<Membership> existedMembersList = userService.getMembersByUserId(user.getId());
					if (CollectionUtils.isNotEmpty(existedMembersList)) {
						for (int j = 0; j < existedMembersList.size(); j++) {
							if (memberships.contains(existedMembersList.get(j).getMembershipNumber()) == Boolean.FALSE) {
								Membership exixstemembershipList = userService.getMatchedMembers(user.getId(), existedMembersList.get(j).getMembershipNumber());
								if (exixstemembershipList != null) {
									userService.removeUserMemberById(exixstemembershipList);
								}
							}
						}
					}
					if (CollectionUtils.isNotEmpty(memberships)) {
						for (int i = 0; i < memberships.size(); i++) {
							Membership userMembers = new Membership();
							User userid = userService.find(user.getId());
							Membership usermembershipList = userService.getMatchedMembers(user.getId(), memberships.get(i));
							System.out.println("member ids ==" + memberships.get(i));
							if (memberships.get(i) != null && usermembershipList == null) {
								userMembers.setUser(userid);
								userMembers.setMembershipNumber(memberships.get(i));
								userMembers.setMembershipName(membershipsNames.get(i));
								userService.saveMembership(userMembers);
							}
						}

						for (int i = 0; i < memberships.size(); i++) {
							Membership userMembers = new Membership();
							User userid = userService.find(user.getId());
							Membership usermembershipList = userService.getMatchedMembers(user.getId(), memberships.get(i));
							System.out.println("member ids ==" + memberships.get(i));
							if (usermembershipList == null) {
								userMembers.setUser(userid);
								userMembers.setMembershipNumber(memberships.get(i));
								userMembers.setMembershipName(membershipsNames.get(i));
								userService.saveMembership(userMembers);
							}
						}

					}
					if (aboutmeprivacy != null) {
						userupdate.setAboutmePrivacy(aboutmeprivacy);
					}
					if (dobprivacy != null) {
						userupdate.setDobPrivacy(dobprivacy);
					}
					if (emailprivacy != null) {
						userupdate.setEmailPrivacy(emailprivacy);
					}
					if (genderprivacy != null) {
						userupdate.setGenderPrivacy(genderprivacy);
					}
					if (hometownprivacy != null) {
						userupdate.setHometownPrivacy(hometownprivacy);
					}
					if (occupationprivacy != null) {
						userupdate.setOccupationPrivacy(occupationprivacy);
					}
					if (nationalityprivacy != null) {
						userupdate.setNationalityPrivacy(nationalityprivacy);
					}
					userupdate.setSeatPreference(seatPreference);
					userupdate.setMealPreference(mealPreference);
					userupdate.setClassPreference(classPreference);
					if (seatprivacy != null) {
						userupdate.setSeatPrivacy(seatprivacy);
					}
					if (mealprivacy != null) {
						userupdate.setMealPrivacy(mealprivacy);
					}
					if (classprivacy != null) {
						userupdate.setClassPrivacy(classprivacy);
					}
					if (interestsprivacy != null) {
						userupdate.setInterestsPrivacy(interestsprivacy);
					}
					if (membershipsprivacy != null) {
						userupdate.setMembershipsPrivacy(membershipsprivacy);
					}
					userService.update(userupdate);
					json.put("status", "SUCCESS");
				}
			} else {
				json.put("status", "FAILURE");
				json.put("message", "user id is not matched");
			}
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userupdate", method = RequestMethod.POST, produces = "application/json")
	public void apiUserDataUpdateV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		System.out.println("File Uploaded===" + user.getProfilePicFile());
		JSONObject json = new JSONObject();
		user.setLevel("update");
		userValidator.validate(user, result);
		if (result.hasErrors()) {
			json.put("status", "FAILURE");
			List<String> allErrors = mobileService.getAllErrors(result);
			json.put("errors", allErrors);
		} else {
			Privacy aboutmeprivacy = userService.findPrivacyById(user.getAboutmePrivacyId());
			Privacy dobprivacy = userService.findPrivacyById(user.getDobPrivacyId());
			Privacy emailprivacy = userService.findPrivacyById(user.getEmailPrivacyId());
			Privacy genderprivacy = userService.findPrivacyById(user.getGenderPrivacyId());
			Privacy hometownprivacy = userService.findPrivacyById(user.getHometownPrivacyId());
			Privacy occupationprivacy = userService.findPrivacyById(user.getOccupationPrivacyId());
			Privacy nationalityprivacy = userService.findPrivacyById(user.getNationalityPrivacyId());
			Privacy seatprivacy = userService.findPrivacyById(user.getSeatPrivacyId());
			Privacy mealprivacy = userService.findPrivacyById(user.getMealPrivacyId());
			Privacy classprivacy = userService.findPrivacyById(user.getClassPrivacyId());
			Privacy interestsprivacy = userService.findPrivacyById(user.getInterestsPrivacyId());
			Privacy membershipsprivacy = userService.findPrivacyById(user.getMembershipsPrivacyId());
			SeatPreference seatPreference = userService.findSeatPreferenceById(user.getSeatPreferenceId());
			MealPreference mealPreference = userService.findMealPreferenceById(user.getMealPreferenceId());
			ClassPreference classPreference = userService.findClassPreferenceById(user.getClassPreferenceId());

			User userupdate = userService.find(user.getId());
			City hometown = locationService.findCityById(user.getHometownId());
			OccupationDemoGraphic occupation = demoGraphicsService.findOccupationById(user.getOccupationId());
			Country nationality = locationService.findCountryById(user.getNationalityId());
			if (userupdate != null) {
				List<Long> memberships = user.getMemberships();
				List<String> membershipsNames = user.getMembershipsNames();
				if (memberships == null) {
					memberships = new ArrayList<Long>();
				}
				if (CollectionUtils.isNotEmpty(memberships) && memberships.size() > 5) {
					json.put("errormessage", "You can have five memberships only");
					json.put("Status", "failure");
				} else {
					if (hometown != null || occupation != null || nationality != null) {
						userupdate.setFirstName(user.getFirstName());
						userupdate.setLastName(user.getLastName());
						userupdate.setDateofBirth(user.getDateofBirth());
						// userupdate.setEmail(user.getEmail());
						userupdate.setAboutMe(user.getAboutMe());
						userupdate.setGender(user.getGender());
						// userupdate.setPassword(user.getPassword());
						// userupdate.setOauthProvider(user.getOauthProvider());
						if (hometown != null) {
							userupdate.setHomeTown(hometown);
						} else {
							userupdate.setHomeTown(null);
						}
						if (nationality != null) {
							userupdate.setNationality(nationality);
						} else {
							userupdate.setNationality(null);
						}
						if (occupation != null) {
							userupdate.setOccupation(occupation);
						} else {
							userupdate.setOccupation(null);
						}

					} else {
						userupdate.setFirstName(user.getFirstName());
						userupdate.setLastName(user.getLastName());
						userupdate.setDateofBirth(user.getDateofBirth());
						// userupdate.setEmail(user.getEmail());
						userupdate.setAboutMe(user.getAboutMe());
						userupdate.setGender(user.getGender());
						if (hometown != null) {
							userupdate.setHomeTown(hometown);
						} else {
							userupdate.setHomeTown(null);
						}
						if (nationality != null) {
							userupdate.setNationality(nationality);
						} else {
							userupdate.setNationality(null);
						}
						if (occupation != null) {
							userupdate.setOccupation(occupation);
						} else {
							userupdate.setOccupation(null);
						}
					}
					if (StringUtils.isNotEmpty(user.getDeviceToken())) {
						userupdate.setDeviceToken(user.getDeviceToken());
					}

					List<Interests> interests = user.getInterests();

					ArrayList<Long> interestIds = new ArrayList<Long>();
					if (CollectionUtils.isNotEmpty(interests)) {
						for (Interests interest : interests) {
							if (interest.getId() != null) {
								interestIds.add(interest.getId());
							}
						}
					}

					List<UserInterests> existedInterestsList = userService.getInterestsByUserId(user.getId());
					if (CollectionUtils.isNotEmpty(existedInterestsList)) {
						for (int i = 0; i < existedInterestsList.size(); i++) {
							System.out.println(interests);
							if (interestIds.contains(existedInterestsList.get(i).getInterest().getId()) == Boolean.FALSE) {
								UserInterests existeduserInterests = userService.getMatchedInterest(existedInterestsList.get(i).getInterest().getId(),
										user.getId());
								if (existeduserInterests != null) {
									userService.removeUserInterestById(existeduserInterests);
								}
							}
						}
					}
					if (CollectionUtils.isNotEmpty(interests)) {
						for (Interests interest : interests) {
							UserInterests userInterests = new UserInterests();
							Interests interestid = userService.findInterestById(interest.getId());
							User userid = userService.find(user.getId());
							UserInterests userInterestsList = userService.getMatchedInterest(interest.getId(), user.getId());
							if (interestid != null && userInterestsList == null) {
								userInterests.setInterest(interestid);
								userInterests.setUser(userid);
								userService.saveUserInterests(userInterests);
							}
						}
					}

					List<Membership> existedMembersList = userService.getMembersByUserId(user.getId());
					if (CollectionUtils.isNotEmpty(existedMembersList)) {
						for (int j = 0; j < existedMembersList.size(); j++) {
							if (memberships.contains(existedMembersList.get(j).getMembershipNumber()) == Boolean.FALSE) {
								Membership exixstemembershipList = userService.getMatchedMembers(user.getId(), existedMembersList.get(j).getMembershipNumber());
								if (exixstemembershipList != null) {
									userService.removeUserMemberById(exixstemembershipList);
								}
							}
						}
					}
					if (CollectionUtils.isNotEmpty(memberships)) {
						for (int i = 0; i < memberships.size(); i++) {
							Membership userMembers = new Membership();
							User userid = userService.find(user.getId());
							Membership usermembershipList = userService.getMatchedMembers(user.getId(), memberships.get(i));
							System.out.println("member ids ==" + memberships.get(i));
							if (memberships.get(i) != null && usermembershipList == null) {
								userMembers.setUser(userid);
								userMembers.setMembershipNumber(memberships.get(i));
								userMembers.setMembershipName(membershipsNames.get(i));
								userService.saveMembership(userMembers);
							}
						}

						for (int i = 0; i < memberships.size(); i++) {
							Membership userMembers = new Membership();
							User userid = userService.find(user.getId());
							Membership usermembershipList = userService.getMatchedMembers(user.getId(), memberships.get(i));
							System.out.println("member ids ==" + memberships.get(i));
							if (usermembershipList == null) {
								userMembers.setUser(userid);
								userMembers.setMembershipNumber(memberships.get(i));
								userMembers.setMembershipName(membershipsNames.get(i));
								userService.saveMembership(userMembers);
							}
						}

					}

					if (aboutmeprivacy != null) {
						userupdate.setAboutmePrivacy(aboutmeprivacy);
					}
					if (dobprivacy != null) {
						userupdate.setDobPrivacy(dobprivacy);
					}
					if (emailprivacy != null) {
						userupdate.setEmailPrivacy(emailprivacy);
					}
					if (genderprivacy != null) {
						userupdate.setGenderPrivacy(genderprivacy);
					}
					if (hometownprivacy != null) {
						userupdate.setHometownPrivacy(hometownprivacy);
					}
					if (occupationprivacy != null) {
						userupdate.setOccupationPrivacy(occupationprivacy);
					}
					if (nationalityprivacy != null) {
						userupdate.setNationalityPrivacy(nationalityprivacy);
					}
					userupdate.setSeatPreference(seatPreference);
					userupdate.setMealPreference(mealPreference);
					userupdate.setClassPreference(classPreference);
					if (seatprivacy != null) {
						userupdate.setSeatPrivacy(seatprivacy);
					}
					if (mealprivacy != null) {
						userupdate.setMealPrivacy(mealprivacy);
					}
					if (classprivacy != null) {
						userupdate.setClassPrivacy(classprivacy);
					}
					if (interestsprivacy != null) {
						userupdate.setInterestsPrivacy(interestsprivacy);
					}
					if (membershipsprivacy != null) {
						userupdate.setMembershipsPrivacy(membershipsprivacy);
					}
					userService.update(userupdate);
					json.put("status", "SUCCESS");
				}
			} else {
				json.put("status", "FAILURE");
				json.put("message", "user id is not matched");
			}
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/usernationality", method = RequestMethod.GET, produces = "application/json")
	public void apiUserCountries(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Country> cities = userService.countryList();
		if (cities != null) {
			json.put("status", "SUCCESS");
			json.put("message", "Nationality details available");
			if (CollectionUtils.isNotEmpty(cities)) {
				JSONArray array = new JSONArray();
				for (Country city : cities) {
					JSONObject object = new JSONObject();
					object.put("id", city.getId());
					object.put("name", city.getName());
					array.put(object);
				}
				json.put("nationalities", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No nationality data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/usernationality", method = RequestMethod.GET, produces = "application/json")
	public void apiUserCountriesV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Country> cities = userService.countryList();
		if (cities != null) {
			json.put("status", "SUCCESS");
			json.put("message", "Nationality details available");
			if (CollectionUtils.isNotEmpty(cities)) {
				JSONArray array = new JSONArray();
				for (Country city : cities) {
					JSONObject object = new JSONObject();
					object.put("id", city.getId());
					object.put("name", city.getName());
					array.put(object);
				}
				json.put("nationalities", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No nationality data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/userhometown", method = RequestMethod.GET, produces = { "application/json; charset=utf-8" })
	public void apiUserHometown(HttpServletRequest request, @RequestParam(value = "searchTerm") String searchTerm, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Object[]> cities = locationService.getHomeTownListBySearch(searchTerm);
		if (cities != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got hometown details");
			if (CollectionUtils.isNotEmpty(cities)) {
				JSONArray array = new JSONArray();
				for (Object[] city : cities) {
					JSONObject object = new JSONObject();
					object.put("id", city[0]);
					object.put("name", city[1]);
					object.put("desc", city[2]);
					array.put(object);
				}
				json.put("hometowns", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No hometown data available");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
	}

	@RequestMapping(value = "/v1/userhometown", method = RequestMethod.GET, produces = { "application/json; charset=utf-8" })
	public void apiUserHometownV1(HttpServletRequest request, @RequestParam("searchTerm") String searchTerm, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Object[]> cities = locationService.getHomeTownListBySearch(searchTerm);
		if (cities != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got hometown details");
			if (CollectionUtils.isNotEmpty(cities)) {
				JSONArray array = new JSONArray();
				for (Object[] city : cities) {
					JSONObject object = new JSONObject();
					object.put("id", city[0]);
					object.put("name", city[1]);
					object.put("desc", city[2]);
					array.put(object);
				}
				json.put("hometowns", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No hometown data available");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		// System.out.println(json.toString());
	}

	@RequestMapping(value = "/welcomehometown", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserWelcomeHometown(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Object[]> cities = locationService.getHomeTownListBySearch(user.getSearchTerm());
		if (cities != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got hometown details");
			if (CollectionUtils.isNotEmpty(cities)) {
				JSONArray array = new JSONArray();
				for (Object[] city : cities) {
					JSONObject object = new JSONObject();
					object.put("id", city[0]);
					object.put("name", city[1]);
					object.put("desc", city[2]);
					array.put(object);
				}
				json.put("hometowns", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No hometown data available");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		// System.out.println(json.toString());9
	}

	@RequestMapping(value = "/useroccupation", method = RequestMethod.GET, produces = "application/json")
	public void apiUserOccupation(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<OccupationDemoGraphic> countries = userService.occupationList();
		if (countries != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got occupation details");
			if (CollectionUtils.isNotEmpty(countries)) {
				JSONArray array = new JSONArray();
				for (OccupationDemoGraphic city : countries) {
					JSONObject object = new JSONObject();
					object.put("id", city.getId());
					object.put("name", city.getValue());
					array.put(object);
				}
				json.put("occupations", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No occupation data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/useroccupation", method = RequestMethod.GET, produces = "application/json")
	public void apiUserOccupationV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<OccupationDemoGraphic> countries = userService.occupationList();
		if (countries != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got occupation details");
			if (CollectionUtils.isNotEmpty(countries)) {
				JSONArray array = new JSONArray();
				for (OccupationDemoGraphic city : countries) {
					JSONObject object = new JSONObject();
					object.put("id", city.getId());
					object.put("name", city.getValue());
					array.put(object);
				}
				json.put("occupations", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No occupation data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/userinterests", method = RequestMethod.GET, produces = "application/json")
	public void apiUserInterests(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Interests> interests = userService.interestsList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (Interests interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getInterestName());
					array.put(object);
				}
				json.put("interests", array);
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

	@RequestMapping(value = "/v1/userinterests", method = RequestMethod.GET, produces = "application/json")
	public void apiUserInterestsV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Interests> interests = userService.interestsList();
		if (interests != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got interests details");
			if (CollectionUtils.isNotEmpty(interests)) {
				JSONArray array = new JSONArray();
				for (Interests interest : interests) {
					JSONObject object = new JSONObject();
					object.put("id", interest.getId());
					object.put("name", interest.getInterestName());
					array.put(object);
				}
				json.put("interests", array);
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

	@RequestMapping(value = "/userpreferences", method = RequestMethod.GET, produces = "application/json")
	public void apiUserSeatPreference(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<SeatPreference> seatPreferences = userService.seatPreferenceList();
		List<MealPreference> mealPreferences = userService.mealPreferenceList();
		List<ClassPreference> classPreferences = userService.classPreferenceList();
		if (seatPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForSeat", "You got seat preference details");
			if (CollectionUtils.isNotEmpty(seatPreferences)) {
				JSONArray array = new JSONArray();
				for (SeatPreference seat : seatPreferences) {
					JSONObject object = new JSONObject();
					object = new JSONObject();
					object.put("id", seat.getId());
					object.put("name", seat.getPreferenceName());
					array.put(object);
				}
				json.put("seatPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No seat preferencesdata available");
		}
		if (mealPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForMeal", "You got meal preference details");
			if (CollectionUtils.isNotEmpty(mealPreferences)) {
				JSONArray array = new JSONArray();
				for (MealPreference meal : mealPreferences) {
					JSONObject object = new JSONObject();
					object = new JSONObject();
					object.put("id", meal.getId());
					object.put("name", meal.getPreferenceName());
					array.put(object);
				}
				json.put("mealPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No meal preferencesdata available");
		}
		if (classPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForClass", "You got class preference details");
			if (CollectionUtils.isNotEmpty(classPreferences)) {
				JSONArray array = new JSONArray();
				for (ClassPreference classpreference : classPreferences) {
					JSONObject object = new JSONObject();
					object.put("id", classpreference.getId());
					object.put("name", classpreference.getPreferenceName());
					array.put(object);
				}
				json.put("classPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No class preferencesdata available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/userpreferences", method = RequestMethod.GET, produces = "application/json")
	public void apiUserSeatPreferenceV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<SeatPreference> seatPreferences = userService.seatPreferenceList();
		List<MealPreference> mealPreferences = userService.mealPreferenceList();
		List<ClassPreference> classPreferences = userService.classPreferenceList();
		if (seatPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForSeat", "You got seat preference details");
			if (CollectionUtils.isNotEmpty(seatPreferences)) {
				JSONArray array = new JSONArray();
				for (SeatPreference seat : seatPreferences) {
					JSONObject object = new JSONObject();
					object.put("id", seat.getId());
					object.put("name", seat.getPreferenceName());
					array.put(object);
				}
				json.put("seatPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No seat preferencesdata available");
		}
		if (mealPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForMeal", "You got meal preference details");
			if (CollectionUtils.isNotEmpty(mealPreferences)) {
				JSONArray array = new JSONArray();
				for (MealPreference meal : mealPreferences) {
					JSONObject object = new JSONObject();
					object.put("id", meal.getId());
					object.put("name", meal.getPreferenceName());
					array.put(object);
				}
				json.put("mealPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No meal preferencesdata available");
		}

		if (classPreferences != null) {
			json.put("status", "SUCCESS");
			json.put("messageForClass", "You got class preference details");
			if (CollectionUtils.isNotEmpty(classPreferences)) {
				JSONArray array = new JSONArray();
				for (ClassPreference classpreference : classPreferences) {
					JSONObject object = new JSONObject();
					object.put("id", classpreference.getId());
					object.put("name", classpreference.getPreferenceName());
					array.put(object);
				}
				json.put("classPreferences", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No class preferencesdata available");
		}

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/usernotification", method = RequestMethod.POST, produces = "application/json")
	public void apiUserNotifications(@RequestBody UserNotifications userNotification, User user, BindingResult result, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		String name = userNotification.getNotificationName();
		String text = userNotification.getNotificationText();
		Long userid = userNotification.getUserId();
		System.out.println(StringUtils.isEmpty(name));
		if (name.equals("social") || name.equals("trip") || name.equals("shopping")) {
			if (!StringUtils.isEmpty(name) && userid != null && userNotification.getApp() != null && userNotification.getEmail() != null) {
				UserNotifications notificationsList = userService.getMatchedNotification(name, text, userid);
				User userid1 = userService.find(userid);
				if (notificationsList == null) {
					userNotification.setUser(userid1);
					userService.saveNotification(userNotification);
				} else {
					notificationsList.setApp(userNotification.getApp());
					notificationsList.setEmail(userNotification.getEmail());
					userService.updateNotification(notificationsList);
				}
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "set all fields");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("errormessage", "incorrect details");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/usernotification", method = RequestMethod.POST, produces = "application/json")
	public void apiUserNotificationsV1(@RequestBody UserNotifications userNotification, User user, BindingResult result, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		String name = userNotification.getNotificationName();
		String text = userNotification.getNotificationText();
		Long userid = userNotification.getUserId();
		System.out.println(StringUtils.isEmpty(name));
		if (name.equals("social") || name.equals("trip") || name.equals("shopping")) {
			if (!StringUtils.isEmpty(name) && userid != null && userNotification.getApp() != null && userNotification.getEmail() != null) {
				UserNotifications notificationsList = userService.getMatchedNotification(name, text, userid);
				User userid1 = userService.find(userid);
				if (notificationsList == null) {
					userNotification.setUser(userid1);
					userService.saveNotification(userNotification);
				} else {
					notificationsList.setApp(userNotification.getApp());
					notificationsList.setEmail(userNotification.getEmail());
					// notificationsList.setNotificationText(userNotification.getNotificationText());
					userService.updateNotification(notificationsList);
				}
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "set all fields");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("errormessage", "incorrect details");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/userprofiledetails", method = RequestMethod.POST, produces = "application/json")
	public void apiUserProfileDetails(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			json.put("status", "SUCCESS");
			List<UserInterests> interestsList = userService.findInterestsByUserId(user.getId());
			List<Membership> MembersList = userService.getMembersByUserId(user.getId());
			UserProfileDto dto = new UserProfileDto(userdata, interestsList, MembersList);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("userDetails", jsonObject);
			json = addCommonLists(json);
			BigInteger madeTrips = tripService.madeTrips(user.getId());
			Long allMadeConnectionsCount = socialLoungeService.getAllMadeConnectionsCount(user.getId());
			Long allMadeDealsCount = socialLoungeService.getAllMadeDealsCount(user.getId());
			json.put("connections", allMadeConnectionsCount);
			json.put("deals", allMadeDealsCount);
			json.put("trips", madeTrips);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/userprofiledetails", method = RequestMethod.POST, produces = "application/json")
	public void apiUserProfileDetailsV1(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			json.put("status", "SUCCESS");
			List<UserInterests> interestsList = userService.findInterestsByUserId(user.getId());
			List<Membership> MembersList = userService.getMembersByUserId(user.getId());
			UserProfileDto dto = new UserProfileDto(userdata, interestsList, MembersList);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("userDetails", jsonObject);
			json = addCommonLists(json);
			BigInteger madeTrips = tripService.madeTrips(user.getId());
			Long allMadeConnectionsCount = socialLoungeService.getAllMadeConnectionsCount(user.getId());
			Long allMadeDealsCount = socialLoungeService.getAllMadeDealsCount(user.getId());
			json.put("connections", allMadeConnectionsCount);
			json.put("deals", allMadeDealsCount);
			json.put("trips", madeTrips);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/usernotificationsview", method = RequestMethod.POST, produces = "application/json")
	public void apiUserNotificationDetails(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		System.out.println("user id to view notifications=======" + user.getId());
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			List<UserNotifications> notifications = userService.getNotificationsByUserId(user.getId());
			if (CollectionUtils.isNotEmpty(notifications)) {
				JSONArray array = new JSONArray();
				for (UserNotifications notification : notifications) {
					JSONObject object = new JSONObject();
					object.put("notificationName", notification.getNotificationName());
					object.put("notificationText", notification.getNotificationText());
					object.put("App", notification.getApp());
					object.put("email", notification.getEmail());
					array.put(object);
				}
				json.put("notificationDetails", array);
			}
			json.put("status", "SUCCESS");
			json.put("message", "");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/usernotificationsview", method = RequestMethod.POST, produces = "application/json")
	public void apiUserNotificationDetailsV1(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		System.out.println("user id to view notifications=======" + user.getId());
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			List<UserNotifications> notifications = userService.getNotificationsByUserId(user.getId());

			if (CollectionUtils.isNotEmpty(notifications)) {
				JSONArray array = new JSONArray();
				for (UserNotifications notification : notifications) {
					JSONObject object = new JSONObject();
					object.put("notificationName", notification.getNotificationName());
					object.put("notificationText", notification.getNotificationText());
					object.put("App", notification.getApp());
					object.put("email", notification.getEmail());
					array.put(object);
				}
				json.put("notificationDetails", array);
			}
			json.put("status", "SUCCESS");
			json.put("message", "");

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/profiledetailsnotifications", method = RequestMethod.POST, produces = "application/json")
	public void apiUserProfileDetailsNotifications(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			json.put("status", "SUCCESS");
			List<UserInterests> interestsList = userService.findInterestsByUserId(user.getId());
			List<Membership> MembersList = userService.getMembersByUserId(user.getId());
			UserProfileDto dto = new UserProfileDto(userdata, interestsList, MembersList);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("userDetails", jsonObject);
			json = addCommonLists(json);
			BigInteger madeTrips = tripService.madeTrips(user.getId());
			Long allMadeConnectionsCount = socialLoungeService.getAllMadeConnectionsCount(user.getId());
			Long allMadeDealsCount = socialLoungeService.getAllMadeDealsCount(user.getId());
			json.put("connections", allMadeConnectionsCount);
			json.put("deals", allMadeDealsCount);
			json.put("trips", madeTrips);
			List<UserNotifications> notifications = userService.getNotificationsByUserId(user.getId());

			if (CollectionUtils.isNotEmpty(notifications)) {
				JSONArray array = new JSONArray();
				for (UserNotifications notification : notifications) {
					JSONObject object = new JSONObject();
					object.put("notificationName", notification.getNotificationName());
					object.put("notificationText", notification.getNotificationText());
					object.put("App", notification.getApp());
					object.put("email", notification.getEmail());
					array.put(object);
				}
				json.put("notificationDetails", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/userprivacylist", method = RequestMethod.GET, produces = "application/json")
	public void apiUserPrivacyList(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Privacy> privacyList = userService.privacyList();
		if (privacyList != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got privacy details");
			if (CollectionUtils.isNotEmpty(privacyList)) {
				JSONArray array = new JSONArray();
				for (Privacy privacy : privacyList) {
					JSONObject object = new JSONObject();
					object.put("id", privacy.getId());
					object.put("name", privacy.getPrivacyName());
					array.put(object);
				}
				json.put("privacies", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No privacy data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/userprivacylist", method = RequestMethod.GET, produces = "application/json")
	public void apiUserPrivacyListV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		List<Privacy> privacyList = userService.privacyList();
		if (privacyList != null) {
			json.put("status", "SUCCESS");
			json.put("message", "You got privacy details");
			if (CollectionUtils.isNotEmpty(privacyList)) {
				JSONArray array = new JSONArray();
				for (Privacy privacy : privacyList) {
					JSONObject object = new JSONObject();
					object.put("id", privacy.getId());
					object.put("name", privacy.getPrivacyName());
					array.put(object);
				}
				json.put("privacies", array);
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No privacy data available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	public JSONObject addCommonLists(JSONObject json) throws JSONException {
		List<SeatPreference> seatPreferences = userService.seatPreferenceList();
		List<MealPreference> mealPreferences = userService.mealPreferenceList();
		List<ClassPreference> classPreferences = userService.classPreferenceList();
		List<Privacy> privacyList = userService.privacyList();
		List<Interests> interestsList = userService.interestsList();
		if (CollectionUtils.isNotEmpty(interestsList)) {
			JSONArray array = new JSONArray();
			for (Interests interest : interestsList) {
				JSONObject object = new JSONObject();
				object.put("id", interest.getId());
				object.put("name", interest.getInterestName());
				array.put(object);
			}
			json.put("interests", array);
		}

		List<Country> cities = userService.countryList();
		if (CollectionUtils.isNotEmpty(cities)) {
			JSONArray array = new JSONArray();
			for (Country city : cities) {
				JSONObject object = new JSONObject();
				object.put("id", city.getId());
				object.put("name", city.getName());
				array.put(object);
			}
			json.put("nationalities", array);
		}
		List<City> homeTowns = userService.cityList();
		if (CollectionUtils.isNotEmpty(homeTowns)) {
			JSONArray array = new JSONArray();
			for (City home : homeTowns) {
				JSONObject object = new JSONObject();
				object.put("id", home.getId());
				object.put("name", home.getName());
				array.put(object);
			}
			// json.put("hometowns", array);
		}

		List<OccupationDemoGraphic> countries = userService.occupationList();
		if (CollectionUtils.isNotEmpty(countries)) {
			JSONArray array = new JSONArray();
			for (OccupationDemoGraphic city : countries) {
				JSONObject object = new JSONObject();
				object.put("id", city.getId());
				object.put("name", city.getValue());
				array.put(object);
			}
			json.put("occupations", array);
		}
		if (CollectionUtils.isNotEmpty(privacyList)) {
			JSONArray array = new JSONArray();
			for (Privacy privacy : privacyList) {
				JSONObject object = new JSONObject();
				object.put("id", privacy.getId());
				object.put("name", privacy.getPrivacyName());
				array.put(object);
			}
			json.put("privacies", array);
		}
		if (CollectionUtils.isNotEmpty(seatPreferences)) {
			JSONArray array = new JSONArray();
			for (SeatPreference seat : seatPreferences) {
				JSONObject object = new JSONObject();
				object.put("id", seat.getId());
				object.put("name", seat.getPreferenceName());
				array.put(object);
			}
			json.put("seatPreferences", array);
		}
		if (CollectionUtils.isNotEmpty(mealPreferences)) {
			JSONArray array = new JSONArray();
			for (MealPreference meal : mealPreferences) {
				JSONObject object = new JSONObject();
				object.put("id", meal.getId());
				object.put("name", meal.getPreferenceName());
				array.put(object);
			}
			json.put("mealPreferences", array);
		}

		if (CollectionUtils.isNotEmpty(classPreferences)) {
			JSONArray array = new JSONArray();
			for (ClassPreference classpreference : classPreferences) {
				JSONObject object = new JSONObject();
				object.put("id", classpreference.getId());
				object.put("name", classpreference.getPreferenceName());
				array.put(object);
			}
			json.put("classPreferences", array);
		}
		return json;
	}

	@RequestMapping(value = "/usercommonlists", method = RequestMethod.GET, produces = "application/json")
	public void apiUserCommonLists(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		json = addCommonLists(json);
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/usercommonlists", method = RequestMethod.GET, produces = "application/json")
	public void apiUserCommonListsV1(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		json = addCommonLists(json);
		json.put("status", "SUCCESS");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/userprofilecompleted", method = RequestMethod.POST, produces = "application/json")
	public void apiUserProfileCompleted(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		int profileCompleted = 0;
		if (userdata != null) {
			JSONObject object;
			JSONArray array;
			json.put("status", "SUCCESS");
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
			List<UserInterests> interestsList = userService.findInterestsByUserId(user.getId());
			List<Membership> MembersList = userService.getMembersByUserId(user.getId());
			if (CollectionUtils.isNotEmpty(interestsList)) {
				profileCompleted += 10;
				array = new JSONArray();
				for (UserInterests interest : interestsList) {
					object = new JSONObject();
					array.put(object);
				}
			}
			if (CollectionUtils.isNotEmpty(MembersList)) {
				profileCompleted += MembersList.size() * 2;
				array = new JSONArray();
				for (Membership member : MembersList) {
					object = new JSONObject();
					array.put(object);
				}
			}
			json.put("ProfileCompleted", String.valueOf(profileCompleted) + "%");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/userprofilecompleted", method = RequestMethod.POST, produces = "application/json")
	public void apiUserProfileCompletedV1(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		int profileCompleted = 0;
		if (userdata != null) {
			json.put("status", "SUCCESS");
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

			List<UserInterests> interestsList = userService.findInterestsByUserId(user.getId());
			List<Membership> MembersList = userService.getMembersByUserId(user.getId());

			if (CollectionUtils.isNotEmpty(interestsList)) {
				profileCompleted += 10;
				JSONArray array = new JSONArray();
				for (UserInterests interest : interestsList) {
					JSONObject object = new JSONObject();
					// object.put("interest",
					// interest.getInterest().getInterestName());
					array.put(object);
				}
				// json.put("interests", array);
			}

			if (CollectionUtils.isNotEmpty(MembersList)) {
				profileCompleted += MembersList.size() * 2;
				JSONArray array = new JSONArray();
				for (Membership member : MembersList) {
					JSONObject object = new JSONObject();
					// object.put("membership number",
					// member.getMembershipNumber());
					array.put(object);
				}
				// json.put("membership numbers", array);
			}
			json.put("ProfileCompleted", profileCompleted + "%");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/usersecurity", method = RequestMethod.POST, produces = "application/json")
	public void apiUserSecurity(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException, ServletException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			String existedPassword = userdata.getPassword();
			System.out.println("current password = " + existedPassword);
			System.out.println("current password after decrypt= " + CryptoUtility.decrypt(existedPassword));
			String oldPassword = user.getOldPassword();
			System.out.println("old password = " + oldPassword);
			String newPassword = user.getNewPassword();
			if (existedPassword.equals(oldPassword)) {
				userdata.setPassword(newPassword);
				if (existedPassword.equals(CryptoUtility.encrypt(oldPassword))) {
					userdata.setPassword(CryptoUtility.encrypt(newPassword));
					userService.update(userdata);
					json.put("status", "SUCCESS");
				}
			} else {
				json.put("status", "FAILURE");
				json.put("message", "old password does not matched");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/usersecurity", method = RequestMethod.POST, produces = "application/json")
	public void apiUserSecurityV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException, ServletException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			String existedPassword = userdata.getPassword();
			System.out.println("current password = " + existedPassword);
			System.out.println("current password after decrypt= " + CryptoUtility.decrypt(existedPassword));
			String oldPassword = user.getOldPassword();
			System.out.println("old password = " + oldPassword);

			String newPassword = user.getNewPassword();
			if (existedPassword.equals(CryptoUtility.encrypt(oldPassword))) {
				userdata.setPassword(CryptoUtility.encrypt(newPassword));
				userService.update(userdata);
				json.put("status", "SUCCESS");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "old password does not matched");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userdeactivate", method = RequestMethod.POST, produces = "application/json")
	public void apiUserDeactivate(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			userdata.setAboutMe(null);
			userdata.setProfilePic(null);
			userdata.setAboutmePrivacy(null);
			userdata.setClassPreference(null);
			userdata.setClassPrivacy(null);
			userdata.setDateofBirth(null);
			userdata.setDobPrivacy(null);
			userdata.setEmail(null);
			userdata.setEmailPrivacy(null);
			userdata.setFirstName(null);
			userdata.setGender(null);
			userdata.setGenderPrivacy(null);
			userdata.setHomeTown(null);
			userdata.setHometownPrivacy(null);
			userdata.setInterests(null);
			userdata.setInterestsPrivacy(null);
			userdata.setLastName(null);
			userdata.setMealPreference(null);
			userdata.setMealPrivacy(null);
			userdata.setMemberships(null);
			userdata.setMembershipsNames(null);
			userdata.setMembershipsPrivacy(null);
			userdata.setNationality(null);
			userdata.setNationalityPrivacy(null);
			userdata.setOauthProvider(null);
			userdata.setOauthUid(null);
			userdata.setOccupation(null);
			userdata.setOccupationPrivacy(null);
			userdata.setPassword(null);
			userdata.setProfilePic(null);
			userdata.setSeatPreference(null);
			userdata.setSeatPrivacy(null);
			userdata.setSessionToken(null);
			userdata.setTemporaryPassword(null);
			userdata.setStatus(UserStatus.INACTIVE);
			userService.update(userdata);
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userdeactivate", method = RequestMethod.POST, produces = "application/json")
	public void apiUserDeactivateV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			userdata.setAboutMe(null);
			userdata.setProfilePic(null);
			userdata.setAboutmePrivacy(null);
			userdata.setClassPreference(null);
			userdata.setClassPrivacy(null);
			userdata.setDateofBirth(null);
			userdata.setDobPrivacy(null);
			userdata.setEmail(null);
			userdata.setEmailPrivacy(null);
			userdata.setFirstName(null);
			userdata.setGender(null);
			userdata.setGenderPrivacy(null);
			userdata.setHomeTown(null);
			userdata.setHometownPrivacy(null);
			userdata.setInterests(null);
			userdata.setInterestsPrivacy(null);
			userdata.setLastName(null);
			userdata.setMealPreference(null);
			userdata.setMealPrivacy(null);
			userdata.setMemberships(null);
			userdata.setMembershipsNames(null);
			userdata.setMembershipsPrivacy(null);
			userdata.setNationality(null);
			userdata.setNationalityPrivacy(null);
			userdata.setOauthProvider(null);
			userdata.setOauthUid(null);
			userdata.setOccupation(null);
			userdata.setOccupationPrivacy(null);
			userdata.setPassword(null);
			userdata.setProfilePic(null);
			userdata.setSeatPreference(null);
			userdata.setSeatPrivacy(null);
			userdata.setSessionToken(null);
			userdata.setTemporaryPassword(null);
			userdata.setStatus(UserStatus.INACTIVE);
			userService.update(userdata);
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userindustryinterests", method = RequestMethod.POST, produces = "application/json")
	public void apiUserIndustryInterests(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			String value = industryInterestsService.getMatchedIndustry(user.getId(), user.getIndustryId());
			json.put("status", "SUCCESS");
			json.put("isthere", value);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userindustryinterests", method = RequestMethod.POST, produces = "application/json")
	public void apiUserIndustryInterestsV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			String value = industryInterestsService.getMatchedIndustry(user.getId(), user.getIndustryId());
			json.put("status", "SUCCESS");
			json.put("isthere", value);
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/update/profilepicture", method = RequestMethod.POST, produces = "application/json")
	public void updateProfilePicture(@RequestParam(value = "profilePicFile") MultipartFile file, @RequestParam(value = "id") Long userId,
			HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(userId);
		if (userdata != null) {
			String uploadFile = uploadFile(file);
			userdata.setProfilePic(uploadFile);
			userService.update(userdata);
			json.put("status", "SUCCESS");
			json.put("profilePic", userdata.getFormedProfileUrl());
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/update/profilepicture", method = RequestMethod.POST, produces = "application/json")
	public void updateProfilePictureV1(@RequestParam("profilePicFile") MultipartFile file, @RequestParam("id") Long userId, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(userId);
		if (userdata != null) {
			String uploadFile = uploadFile(file);
			userdata.setProfilePic(uploadFile);
			userService.update(userdata);
			json.put("status", "SUCCESS");
			json.put("profilePic", userdata.getFormedProfileUrl());
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/get/profilepicture", method = RequestMethod.POST, produces = "application/json")
	public void getProfilePicture(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			json.put("status", "SUCCESS");
			json.put("profilePic", userdata.getFormedProfileUrl());
			BigInteger madeTrips = tripService.madeTrips(user.getId());
			Long allMadeConnectionsCount = socialLoungeService.getAllMadeConnectionsCount(user.getId());
			Long allMadeDealsCount = socialLoungeService.getAllMadeDealsCount(user.getId());
			json.put("connections", allMadeConnectionsCount);
			json.put("deals", allMadeDealsCount);
			json.put("trips", madeTrips);
			json.put("firstname", userdata.getFirstName());
			json.put("lastname", userdata.getLastName());
			json.put("gender", userdata.getGender());
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/get/profilepicture", method = RequestMethod.POST, produces = "application/json")
	public void getProfilePictureV1(@RequestBody User user, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(user.getId());
		if (userdata != null) {
			json.put("status", "SUCCESS");
			json.put("profilePic", userdata.getFormedProfileUrl());
			BigInteger madeTrips = tripService.madeTrips(user.getId());
			Long allMadeConnectionsCount = socialLoungeService.getAllMadeConnectionsCount(user.getId());
			Long allMadeDealsCount = socialLoungeService.getAllMadeDealsCount(user.getId());
			json.put("connections", allMadeConnectionsCount);
			json.put("deals", allMadeDealsCount);
			json.put("trips", madeTrips);
			json.put("firstname", userdata.getFirstName());
			json.put("lastname", userdata.getLastName());
			json.put("gender", userdata.getGender());
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No user data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	public String uploadFile(MultipartFile file) {
		String fileName = "";
		if (file != null && !file.isEmpty()) {
			long currentTimeMillis = System.currentTimeMillis();
			fileName = "" + currentTimeMillis + "_" + file.getOriginalFilename();
			File file1 = new File(User.readHomePath());
			if (file1.exists() == Boolean.FALSE) {
				file1.mkdirs();
			}
			String name = User.readHomePath() + fileName;
			try {
				byte[] bytes = file.getBytes();
				File file2 = new File(name);
				if (!file2.exists()) {
					file2.createNewFile();
				}
				BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file2));
				stream.write(bytes);
				stream.close();
				System.out.println("You successfully uploaded " + name + "!");
			} catch (Exception e) {
				System.out.println("You failed to upload " + name + " => " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.println("The file was empty.");
		}
		return fileName;
	}

	@RequestMapping(value = "/get/airport", method = RequestMethod.POST, produces = "application/json")
	public void getAirport(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		if (userdata != null) {
			Trip todayTripForUser = tripService.getTodayTripForUser(userdata.getId());
			Boolean hasTrip = Boolean.FALSE;
			String currentAirportByLocation = socialLoungeService.getCurrentAirportByLocation(adsRequestDto.getLatitude(), adsRequestDto.getLongitude());
			if (currentAirportByLocation != null) {
				userdata.setCurrentAirport(currentAirportByLocation);
				userService.update(userdata);

			}
			if (todayTripForUser != null && currentAirportByLocation.equals(todayTripForUser.getOrigin().getIataCode())) {
				hasTrip = Boolean.TRUE;
			}
			BigDecimal latitude = adsRequestDto.getLatitude();
			BigDecimal longitude = adsRequestDto.getLongitude();
			if (latitude != null && longitude != null) {
				// empty if block
			}
			Boolean hasFutureTripsForNext15Days = tripService.hasFutureTripsForNext15Days(userdata.getId());
			if (todayTripForUser != null) {
				json.put("iatacode", todayTripForUser.getOrigin().getIataCode());
			} else {
				json.put("iatacode", "");
			}
			json.put("isAtAirportOfTrip", StringUtils.isNotEmpty(currentAirportByLocation));
			json.put("hasTrip", hasTrip);
			json.put("hasFutureTrip", hasFutureTripsForNext15Days);
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/get/airport", method = RequestMethod.POST, produces = "application/json")
	public void getAirportV1(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		if (userdata != null) {
			Trip todayTripForUser = tripService.getTodayTripForUser(userdata.getId());
			Boolean hasTrip = Boolean.FALSE;
			String currentAirportByLocation = socialLoungeService.getCurrentAirportByLocation(adsRequestDto.getLatitude(), adsRequestDto.getLongitude());
			userdata.setCurrentAirport(currentAirportByLocation);
			userService.update(userdata);
			if (todayTripForUser != null) {
				if (currentAirportByLocation.equals(todayTripForUser.getOrigin().getIataCode())) {
					hasTrip = Boolean.TRUE;
				}
			}
			BigDecimal latitude = adsRequestDto.getLatitude();
			BigDecimal longitude = adsRequestDto.getLongitude();
			if (latitude != null && longitude != null) {

				/*
				 * // System.out.println(
				 * "Running CAMPAIGN ALGO IN BACKGROUND================================"
				 * ); //
				 * socialLoungeService.runCamapignAlgo(adsRequestDto.getUserId
				 * (), // latitude, longitude); List<String> initData =
				 * socialLoungeService.initAlgo(adsRequestDto.getUserId(),
				 * latitude, longitude); System.out.println(
				 * "Running init ALGO IN BACKGROUND================================"
				 * ); //
				 * socialLoungeService.runCamapignAlgo(matchedMobileUser.getId
				 * (), // latitude, longitude); if
				 * (CollectionUtils.isNotEmpty(initData)) { userdata =
				 * userService.find(adsRequestDto.getUserId()); String userState
				 * = userdata.getUserState(); String userAirportLocation =
				 * userdata.getCurrentAirport();
				 * System.out.println("user state ===" + userState);
				 * System.out.println("user airport ===" + userAirportLocation);
				 * List<UserDetails> algoUserDetails =
				 * socialLoungeService.algoUserPerameterDetails
				 * (adsRequestDto.getUserId(), userState, userAirportLocation);
				 * List<UserCampaign> MatchedCampaigns =
				 * socialLoungeService.algoMatchedCampaignsForUser
				 * (algoUserDetails.get(0).getUser_age(), algoUserDetails
				 * .get(0).getUser_gender(),
				 * algoUserDetails.get(0).getUser_occupation(),
				 * algoUserDetails.get(0).getUser_nationality(),
				 * algoUserDetails.get(0).getUser_hometown(),
				 * algoUserDetails.get(0).getTrip_travel_purpose(),
				 * algoUserDetails.get(0) .getTrip_destination(),
				 * algoUserDetails.get(0).getTrip_current_location(),
				 * userState); for (UserCampaign campaigns : MatchedCampaigns) {
				 * Boolean isCampaignEligible =
				 * socialLoungeService.isCampaignEligibleForSelection
				 * (campaigns.getCampaignid(), matchedMobileUser.getId());
				 * System.out.println("campid====" + isCampaignEligible);
				 * System.out.println("campid====" + campaigns.getCampaignid());
				 * System.out.println("type===" +
				 * campaigns.getCampaigntypesd_campaigntypeid());
				 * System.out.println("style-===" +
				 * campaigns.getDisplay_style()); } CampaignScoreUtils
				 * campaignScoreUtils = new CampaignScoreUtils(); if
				 * (CollectionUtils.isNotEmpty(MatchedCampaigns)) {
				 * List<CampaignScore> campaignScore =
				 * socialLoungeService.computeCampaignScore(MatchedCampaigns,
				 * adsRequestDto.getUserId());
				 * System.out.println("*********** Before Sorting ***********");
				 * socialLoungeService.displayCampaignScore(campaignScore);
				 * System.out.println("*********** After Sorting ***********");
				 * campaignScore =
				 * campaignScoreUtils.sortCampaignScore(campaignScore);
				 * socialLoungeService.displayCampaignScore(campaignScore);
				 * embeddedArray =
				 * campaignScoreUtils.getEmbeddedArray(userState,
				 * campaignScore); popUpArray =
				 * campaignScoreUtils.getPopUpArray(userState, campaignScore);
				 * System.out.println(
				 * "*********** EMBEDDED ARRAY BEFORE CO EFF***********");
				 * socialLoungeService.displayJsonArray(embeddedArray);
				 * System.out
				 * .println("*********** POPUP ARRAY BEFORE CO EFF***********");
				 * socialLoungeService.displayJsonArray(popUpArray);
				 * embeddedArray =
				 * campaignScoreUtils.computeCoeffProbability(embeddedArray);
				 * popUpArray =
				 * campaignScoreUtils.computeCoeffProbability(popUpArray);
				 * System
				 * .out.println("*********** EMBEDDED ARRAY AFTER CO EFF***********"
				 * ); socialLoungeService.displayJsonArray(embeddedArray);
				 * System
				 * .out.println("*********** POPUP ARRAY AFTER CO EFF***********"
				 * ); socialLoungeService.displayJsonArray(popUpArray);
				 * System.out
				 * .println("*********** EMBEDDED ARRAY JSON***********");
				 * System
				 * .out.println(campaignScoreUtils.convertArrayToJson(embeddedArray
				 * )); System.out.println(
				 * "*********** POPUP ARRAY AFTER JSON***********");
				 * System.out.println
				 * (campaignScoreUtils.convertArrayToJson(popUpArray)); Boolean
				 * hasSavedEmbeddedJsonData =
				 * socialLoungeService.saveEmbeddedJsonData
				 * (campaignScoreUtils.convertArrayToJson(embeddedArray),
				 * adsRequestDto.getUserId(), userState); Boolean
				 * hasSavedPopupJsonData =
				 * socialLoungeService.savePopUpJsonData(
				 * campaignScoreUtils.convertArrayToJson(popUpArray),
				 * adsRequestDto.getUserId(), userState); if
				 * (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
				 * socialLoungeService
				 * .updatecampaignAlgoStatus(adsRequestDto.getUserId(),
				 * "publish"); } } else {
				 * socialLoungeService.updatecampaignAlgoStatus
				 * (adsRequestDto.getUserId(), "no campaigns"); } }
				 */
			}
			Boolean hasFutureTripsForNext15Days = tripService.hasFutureTripsForNext15Days(userdata.getId());
			if (todayTripForUser != null) {
				json.put("iatacode", todayTripForUser.getOrigin().getIataCode());
			} else {
				json.put("iatacode", "");
			}
			json.put("profilePic", userdata.getFormedProfileUrl());
			if (userdata.getGender() != null) {
				json.put("gender", userdata.getGender());
			} else {
				json.put("gender", "");
			}
			if (userdata.getDateofBirth() != null) {
				json.put("dob", userdata.getDateofBirth());
			} else {
				json.put("dob", "");
			}
			if (userdata.getHomeTown() != null) {
				json.put("homeTown", userdata.getHomeTown().getName());
			} else {
				json.put("homeTown", "");
			}

			if (userdata.getNationality() != null) {
				json.put("nationality", userdata.getNationality().getName());
			} else {
				json.put("nationality", "");
			}

			if (userdata.getOccupation() != null) {
				json.put("occupation", userdata.getOccupation().getValue());
			} else {
				json.put("occupation", "");
			}
			json.put("isAtAirportOfTrip", StringUtils.isNotEmpty(currentAirportByLocation));
			json.put("hasTrip", hasTrip);
			json.put("hasFutureTrip", hasFutureTripsForNext15Days);
			json.put("status", "SUCCESS");
			json.put("message", "You got matches");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/isnewdevice", method = RequestMethod.POST, produces = "application/json")
	public void checkNewDevice(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport userAtAirport = locationService.getCloserAirportsByUserLocation(adsRequestDto.getLatitude(), adsRequestDto.getLongitude());
		Boolean hasDeviceRegistered = userService.hasDeviceIdRegistered(adsRequestDto.getDeviceToken());
		if (userAtAirport != null && hasDeviceRegistered != null) {
			Boolean isIntransitAirport = userAtAirport.getIsintransitAirport();
			if (isIntransitAirport && !hasDeviceRegistered) {
				json.put("status", "SUCCESS");
				json.put("isIntransitAirport", Boolean.TRUE);
				json.put("isNewDevice", Boolean.TRUE);
			} else if (!hasDeviceRegistered && !isIntransitAirport) {
				json.put("status", "SUCCESS");
				json.put("isIntransitAirport", Boolean.FALSE);
				json.put("isNewDevice", Boolean.TRUE);

			} else {
				json.put("status", "SUCCESS");
				json.put("isIntransitAirport", Boolean.FALSE);
				json.put("isNewDevice", Boolean.FALSE);
			}
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.write(json.toString());
			System.out.println(json.toString());

		}
	}

	@RequestMapping(value = "/v1/version", method = RequestMethod.GET, produces = "application/json")
	public void getVersion(HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Version version = userService.getVersion();
		if (version != null) {
			System.out.println(version.getVersionNumber());
			json.put("status", "SUCCESS");
			json.put("versionNumber", version.getVersionNumber());
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

}
/* UUID.randomUUID().toString().replaceAll("-", "") */

/**
 * 
 */
package com.intransit.merchant.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intransit.campaignscore.utils.CampaignScoreUtils;
import com.intransit.email.MailMail;
import com.intransit.merchant.api.dto.AdRequestDto;
import com.intransit.merchant.api.dto.AdResponseDto;
import com.intransit.merchant.api.dto.CamapignAdsRequestDto;
import com.intransit.merchant.api.dto.CampaignScoreJson;
import com.intransit.merchant.api.dto.StoreDto;
import com.intransit.merchant.api.webservice.BaseEndpoint;
import com.intransit.merchant.model.Airport;
import com.intransit.merchant.model.Campaign;
import com.intransit.merchant.model.GlobalSwitch;
import com.intransit.merchant.model.LoggingTrack;
import com.intransit.merchant.model.MerchantIndustry;
import com.intransit.merchant.model.Store;
import com.intransit.merchant.model.Terminal;
import com.intransit.merchant.model.User;
import com.intransit.merchant.model.UserCampaignScore;
import com.intransit.merchant.service.CampaignService;
import com.intransit.merchant.service.LocationService;
import com.intransit.merchant.service.SocialLoungeService;
import com.intransit.merchant.service.UserService;
import com.intransit.merchant.status.CampaignStatus;

/**
 * @author kodelavg
 *
 */
@RestController
@RequestMapping("/api/shopping")
public class UserShoppingController extends BaseEndpoint {

	@Resource
	protected UserService userService;

	@Resource
	protected CampaignService campaignService;

	@Resource
	protected LocationService locationService;

	@Resource
	protected SocialLoungeService socialLoungeService;

	@Resource
	protected MailMail mail;

	@Transactional
	@RequestMapping(value = "/usersearchstore", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserSearchStores(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			Boolean isIntransitAirport = airportData.getIsintransitAirport();
			json.put("isIntransitAirport", isIntransitAirport);
			List<Store> stores = locationService.getSearchedStores(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(stores)) {
				ArrayList<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					ArrayList<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();
					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}
				Gson gson = new Gson();
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("stores", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores details");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No stores data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/usersearchstore", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserSearchStoresV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			Boolean isIntransitAirport = airportData.getIsintransitAirport();
			json.put("isIntransitAirport", isIntransitAirport);
			List<Store> stores = locationService.getSearchedStores(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(stores)) {
				List<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					List<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();

					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}

				// StoreDto dto = new StoreDto(stores);
				Gson gson = new Gson();
				// List<StoreDto> storeDtos = dto.getStores();

				// String json2 = gson.toJson(storeDtos, storeDtos.getClass());
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("stores", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores details");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No stores data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/store/details", method = RequestMethod.POST, produces = "application/json")
	public void getStoreDetails(@RequestBody Store store, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		Store dbstore = locationService.findStoreById(store.getId());
		if (dbstore != null) {
			StoreDto dto = new StoreDto(dbstore);
			Gson gson = new Gson();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("store", jsonObject);
			json.put("status", "SUCCESS");
			json.put("messageForStores", "You got stores details");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No store details data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/store/details", method = RequestMethod.POST, produces = "application/json")
	public void getStoreDetailsV1(@RequestBody Store store, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Store dbstore = locationService.findStoreById(store.getId());
		if (dbstore != null) {
			StoreDto dto = new StoreDto(dbstore);
			Gson gson = new Gson();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("store", jsonObject);
			json.put("status", "SUCCESS");
			json.put("messageForStores", "You got stores details");
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No store details data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userspecialoffer", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserSpaecialOffer(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getSpecialOfferStores(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(stores)) {
				ArrayList<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					ArrayList<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();

					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);

					}
				}
				Gson gson = new Gson();
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("offerStores", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got offer stores details");
				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("SPECIAL OFFERS");
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer stores data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userspecialoffer", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserSpaecialOfferV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getSpecialOfferStores(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(stores)) {
				List<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					List<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();

					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);

					}
				}

				// StoreDto dto = new StoreDto(stores);
				Gson gson = new Gson();
				// List<StoreDto> storeDtos = dto.getStores();

				// String json2 = gson.toJson(storeDtos, storeDtos.getClass());
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("offerStores", jsonObject);
				/*
				 * Integer count = 1; List<Campaign> campaignList =
				 * campaignService.findTestEmbedCampaigns(count); if
				 * (CollectionUtils.isNotEmpty(campaignList)) { // JSONArray
				 * array = new JSONArray(); for (Campaign campaign :
				 * campaignList) { // JSONObject object = new JSONObject();
				 * json.put("id", campaign.getId()); json.put("embedImage",
				 * campaign.getFormedEmbeddedMediaUrl()); // object.put("id",
				 * campaign.getId()); // object.put("embedImage", //
				 * campaign.getFormedEmbeddedMediaUrl()); // array.put(object);
				 * } // json.put("status", "SUCCESS"); //
				 * json.put("embeddedCampaigns", array); } else {
				 * json.put("message", "No campaigns data found"); }
				 */
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got offer stores details");

				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("SPECIAL OFFERS");
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);

			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer stores data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/usersindustrystores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserStoresByIndustry(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getIndustryStores(airport.getIataCode(), airport.getIndustryId());
			if (CollectionUtils.isNotEmpty(stores)) {
				ArrayList<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					ArrayList<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();
					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}
				Gson gson = new Gson();
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("storesbyIndustry", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores by industry details");
				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("CATEGORY");
				logTrack.setLogField3(airport.getIndustryId());
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No stores by industry data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/usersindustrystores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserStoresByIndustryV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getIndustryStores(airport.getIataCode(), airport.getIndustryId());
			/*
			 * if (CollectionUtils.isNotEmpty(stores)) { StoreDto dto = new
			 * StoreDto(stores); Gson gson = new Gson(); List<StoreDto>
			 * storeDtos = dto.getStores(); String json2 =
			 * gson.toJson(storeDtos, storeDtos.getClass()); JSONArray
			 * jsonObject = new JSONArray(json2); json.put("storesbyIndustry",
			 * jsonObject); json.put("status", "SUCCESS");
			 * json.put("messageForStores",
			 * "You got stores by industry details"); }
			 */
			if (CollectionUtils.isNotEmpty(stores)) {
				List<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					List<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();

					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}

				// StoreDto dto = new StoreDto(stores);
				Gson gson = new Gson();
				// List<StoreDto> storeDtos = dto.getStores();

				// String json2 = gson.toJson(storeDtos, storeDtos.getClass());
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("storesbyIndustry", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores by industry details");

				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("CATEGORY");
				logTrack.setLogField3(airport.getIndustryId());
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);

			} else {
				json.put("status", "FAILURE");
				json.put("message", "No stores by industry data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/usersterminalstores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserStoresByTerminal(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getTerminalStores(airport.getIataCode(), airport.getTerminalId());
			if (CollectionUtils.isNotEmpty(stores)) {
				ArrayList<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					ArrayList<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();
					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}
				Gson gson = new Gson();
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("storesByTerminal", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores by terminal details");
				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("LOCATION");
				logTrack.setLogField3(airport.getTerminalId());
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No stores by terminal data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/usersterminalstores", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserStoresByTerminalV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Store> stores = locationService.getTerminalStores(airport.getIataCode(), airport.getTerminalId());
			/*
			 * if (CollectionUtils.isNotEmpty(stores)) { StoreDto dto = new
			 * StoreDto(stores); Gson gson = new Gson(); List<StoreDto>
			 * storeDtos = dto.getStores(); String json2 =
			 * gson.toJson(storeDtos, storeDtos.getClass()); JSONArray
			 * jsonObject = new JSONArray(json2); json.put("storesByTerminal",
			 * jsonObject); json.put("status", "SUCCESS");
			 * json.put("messageForStores",
			 * "You got stores by terminal details"); JSONArray array = new
			 * JSONArray(); for (Store store : stores) { JSONObject object = new
			 * JSONObject(); object.put("id", store.getId());
			 * object.put("storename", store.getShopName()); array.put(object);
			 * } // json.put("status", "SUCCESS"); //
			 * json.put("storesByTerminal", array); }
			 */
			if (CollectionUtils.isNotEmpty(stores)) {
				List<StoreDto> finalStores = new ArrayList<StoreDto>();
				User userdata = userService.find(airport.getUserId());
				if (userdata != null) {
					String cpmAdType = "Embedded Advertising";
					List<Campaign> campaignsByStore = new ArrayList<Campaign>();
					GlobalSwitch swithAction = userService.checkGlobalSwitch();

					for (Store store : stores) {
						System.out.println("store id is=======" + store.getId());
						List<Campaign> campaignidsByStore = campaignService.getcampaignsbyStoreid(store.getId());
						for (Campaign campaign : campaignidsByStore) {
							List<Long> MatchedCampaignId = socialLoungeService.verifyStoreCampaignForPublish(campaign.getId(), userdata.getId(),
									userdata.getUserState(), cpmAdType);
							List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
							if (CollectionUtils.isNotEmpty(campaignsByIds) && swithAction.getSwitchAction() == Boolean.TRUE) {

								System.out.println("campaign id from verify finction" + campaignsByIds.get(0).getId());
								if (campaignsByIds.get(0).getId() == campaign.getId()) {
									campaignsByStore.add(campaign);
								}
							}
						}

						AdResponseDto dtocampaign = new AdResponseDto(campaignsByStore);
						List<AdResponseDto> adResponseDtos = dtocampaign.getAdResponseDtos();

						StoreDto d = new StoreDto(store, adResponseDtos);
						// List<StoreDto> storeDtos = d.getStores();
						finalStores.add(d);
					}
				}

				// StoreDto dto = new StoreDto(stores);
				Gson gson = new Gson();
				// List<StoreDto> storeDtos = dto.getStores();

				// String json2 = gson.toJson(storeDtos, storeDtos.getClass());
				String json2 = gson.toJson(finalStores, finalStores.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("storesByTerminal", jsonObject);
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got stores by terminal details");

				Date dateTime = new Date();
				LoggingTrack logTrack = new LoggingTrack();
				logTrack.setUserLogType("BROWSESTORE");
				logTrack.setUpdateAt(dateTime);
				logTrack.setLogField2("LOCATION");
				logTrack.setLogField3(airport.getTerminalId());
				logTrack.setUserSessionToken(userdata.getSessionToken());
				userService.save(logTrack);

			}

			else {
				json.put("status", "FAILURE");
				json.put("message", "No stores by terminal data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userindustries", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserIndustriesList(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<MerchantIndustry> industries = locationService.getIndustries(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(industries)) {
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got all list of industries details");
				JSONArray array = new JSONArray();
				for (MerchantIndustry industry : industries) {
					JSONObject object = new JSONObject();
					object.put("id", industry.getId());
					object.put("industryName", industry.getValue());
					array.put(object);
				}
				json.put("status", "SUCCESS");
				json.put("offerStores", array);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer all list of industries data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userindustries", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserIndustriesListV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<MerchantIndustry> industries = locationService.getIndustries(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(industries)) {
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got all list of industries details");
				JSONArray array = new JSONArray();

				for (MerchantIndustry industry : industries) {
					JSONObject object = new JSONObject();
					object.put("id", industry.getId());
					object.put("industryName", industry.getValue());
					array.put(object);
				}

				json.put("status", "SUCCESS");
				json.put("offerStores", array);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer all list of industries data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/userterminals", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserTerminalsList(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Terminal> terminals = locationService.getTerminals(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(terminals)) {
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got all list of terminals details");
				JSONArray array = new JSONArray();
				for (Terminal terminal : terminals) {
					JSONObject object = new JSONObject();
					object.put("id", terminal.getId());
					object.put("terminalName", terminal.getTerminalName());
					array.put(object);
				}
				json.put("status", "SUCCESS");
				json.put("offerStores", array);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer all list of terminals data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/userterminals", method = RequestMethod.POST, produces = { "application/json; charset=utf-8" })
	public void apiUserTerminalsListV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Terminal> terminals = locationService.getTerminals(airport.getIataCode());
			if (CollectionUtils.isNotEmpty(terminals)) {
				json.put("status", "SUCCESS");
				json.put("messageForStores", "You got all list of terminals details");
				JSONArray array = new JSONArray();

				for (Terminal terminal : terminals) {
					JSONObject object = new JSONObject();
					object.put("id", terminal.getId());
					object.put("terminalName", terminal.getTerminalName());
					array.put(object);
				}

				json.put("status", "SUCCESS");
				json.put("offerStores", array);
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No offer all list of terminals data available");
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json ; charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/get/camapigns", method = RequestMethod.POST, produces = "application/json")
	public void getCampaignAds(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		System.out.println("called get campaign,  userid is ===" + adsRequestDto.getUserId());
		System.out.println("called get campaign,  latitude is ===" + adsRequestDto.getLatitude());
		System.out.println("called get campaign,  longitude is ===" + adsRequestDto.getLongitude());
		System.out.println("called get campaign,  count is ===" + adsRequestDto.getCount());
		System.out.println("called get campaign,  ad type is ===" + adsRequestDto.getAdType());
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		Gson gson = new GsonBuilder().serializeNulls().create();
		if (userdata != null) {
			String cpmAdType = adsRequestDto.getAdType();
			if (StringUtils.equalsIgnoreCase("popup", cpmAdType)) {
				cpmAdType = "Pop Up Advertising";
			} else if (StringUtils.equalsIgnoreCase("embedded", cpmAdType)) {
				cpmAdType = "Embedded Advertising";
			}
			int randomNumber = CampaignScoreUtils.randInt(1, 100);
			GlobalSwitch swithAction = userService.checkGlobalSwitch();
			List<Long> MatchedCampaignId = socialLoungeService.getCampaignForPublish(randomNumber, userdata.getId(), userdata.getUserState(), cpmAdType);
			if (CollectionUtils.isNotEmpty(MatchedCampaignId) && swithAction.getSwitchAction() == Boolean.TRUE) {
				List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
				AdResponseDto dto = new AdResponseDto(campaignsByIds);
				List<AdResponseDto> adResponseDtos = dto.getAdResponseDtos();
				String json2 = gson.toJson(adResponseDtos, adResponseDtos.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("campaigns", jsonObject);
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");
			} else {
				json.put("status", "FAILURE");
				json.put("message", "No campaign Available");
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

	@RequestMapping(value = "/v1/get/camapigns", method = RequestMethod.POST, produces = "application/json")
	public void getCampaignAdsV1(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		System.out.println("called get campaign,  userid is ===" + adsRequestDto.getUserId());
		System.out.println("called get campaign,  latitude is ===" + adsRequestDto.getLatitude());
		System.out.println("called get campaign,  longitude is ===" + adsRequestDto.getLongitude());
		System.out.println("called get campaign,  count is ===" + adsRequestDto.getCount());
		System.out.println("called get campaign,  ad type is ===" + adsRequestDto.getAdType());
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		Gson gson = new GsonBuilder().serializeNulls().create();
		if (userdata != null) {
			String cpmAdType = adsRequestDto.getAdType();
			if (StringUtils.equalsIgnoreCase("popup", cpmAdType)) {
				cpmAdType = "Pop Up Advertising";
			} else if (StringUtils.equalsIgnoreCase("embedded", cpmAdType)) {
				cpmAdType = "Embedded Advertising";
			}

			int randomNumber = CampaignScoreUtils.randInt(1, 100);
			GlobalSwitch swithAction = userService.checkGlobalSwitch();
			List<Long> MatchedCampaignId = socialLoungeService.getCampaignForPublish(randomNumber, userdata.getId(), userdata.getUserState(), cpmAdType);
			if (CollectionUtils.isNotEmpty(MatchedCampaignId) && swithAction.getSwitchAction() == Boolean.TRUE) {
				List<Campaign> campaignsByIds = campaignService.getCampaignsByIds(MatchedCampaignId);
				AdResponseDto dto = new AdResponseDto(campaignsByIds);
				List<AdResponseDto> adResponseDtos = dto.getAdResponseDtos();
				String json2 = gson.toJson(adResponseDtos, adResponseDtos.getClass());
				JSONArray jsonObject = new JSONArray(json2);
				json.put("campaigns", jsonObject);
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");

			} else {
				json.put("status", "FAILURE");
				json.put("message", "No campaign Available");
			}
			/*
			 * List<Long> matchedCamapignIds =
			 * socialLoungeService.getMatchedCamapignIds
			 * (adsRequestDto.getUserId(), adsRequestDto.getLatitude(),
			 * adsRequestDto.getLongitude(), adsRequestDto.getCount(),
			 * cpmAdType);
			 */

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No matches Available");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/update/useraction", method = RequestMethod.POST, produces = "application/json")
	public void updateUserAction(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		Gson gson = new GsonBuilder().serializeNulls().create();
		Campaign campaign = campaignService.findCampaignById(adsRequestDto.getCampaignId());
		if (userdata != null) {
			Boolean updatedAction = socialLoungeService.updateUserAction(adsRequestDto.getUserId(), adsRequestDto.getCampaignId(),
					adsRequestDto.getUserAction(), adsRequestDto.getLatitude(), adsRequestDto.getLongitude(), adsRequestDto.getAdviewDuration(),
					adsRequestDto.getTravelDistance(), adsRequestDto.getTravelTime());
			if (updatedAction == Boolean.TRUE) {
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");
			}
			Boolean isCampaignEligible = socialLoungeService
					.isCampaignEligibleForSelection(adsRequestDto.getCampaignId().intValue(), adsRequestDto.getUserId());
			if (isCampaignEligible == Boolean.FALSE) {
				List<UserCampaignScore> UserCampaignsjsonData = socialLoungeService.getJsonData(adsRequestDto.getUserId());
				if (UserCampaignsjsonData.isEmpty()) {
					// handle empty
				} else {
					CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
					List<CampaignScoreJson> embeddedArray = new ArrayList<CampaignScoreJson>();
					List<CampaignScoreJson> embeddedArrayAfterRemove = new ArrayList<CampaignScoreJson>();
					List<CampaignScoreJson> popUpArray = new ArrayList<CampaignScoreJson>();
					List<CampaignScoreJson> popUpArrayAfterRemove = new ArrayList<CampaignScoreJson>();

					if (campaign.getCampaignTypes().getId() == 1) {
						if (campaign.getDisplayStyle().equals("Embedded Advertising")) {
							embeddedArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
									campaign.getDisplayStyle());
							System.out.println("before remove embedded array size==" + embeddedArray.size());

							for (int i = 0; i < embeddedArray.size(); i++) {
								System.out.println("first one" + embeddedArray.get(i).getCampaignid());
								if (embeddedArray.get(i).getCampaignid() != campaign.getId().intValue()) {
									System.out.println("not matched campaign no need to remove this from json data" + embeddedArray.get(i).getCampaignid());
									embeddedArrayAfterRemove.add(embeddedArray.get(i));
								}
							}
							System.out.println("after remove embedded array size==" + embeddedArrayAfterRemove.size());

							socialLoungeService.displayJsonArray(embeddedArrayAfterRemove);

							embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);
							popUpArrayAfterRemove = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
									"Pop Up Advertising");
							popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);

						} else {
							popUpArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
									campaign.getDisplayStyle());
							for (int i = 0; i < popUpArray.size(); i++) {
								System.out.println("first one" + popUpArray.get(i).getCampaignid());
								if (popUpArray.get(i).getCampaignid() != campaign.getId().intValue()) {
									System.out.println("matched campaign need to remove this from json data" + popUpArray.get(i).getCampaignid());
									popUpArrayAfterRemove.add(popUpArray.get(i));
								}
							}
							System.out.println("after remove embedded array size==" + popUpArrayAfterRemove.size());

							socialLoungeService.displayJsonArray(popUpArrayAfterRemove);

							popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);
							embeddedArrayAfterRemove = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
									"Embedded Advertising");
							embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);

						}

					} else {
						embeddedArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(), "Embedded Advertising");
						for (int i = 0; i < embeddedArray.size(); i++) {
							System.out.println("first one" + embeddedArray.get(i).getCampaignid());
							if (embeddedArray.get(i).getCampaignid() != campaign.getId().intValue()) {
								System.out.println("not matched campaign no need to remove this from json data" + embeddedArray.get(i).getCampaignid());
								embeddedArrayAfterRemove.add(embeddedArray.get(i));
							}
						}
						System.out.println("after remove embedded array size==" + embeddedArrayAfterRemove.size());

						socialLoungeService.displayJsonArray(embeddedArrayAfterRemove);

						popUpArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(), "Pop Up Advertising");

						for (int i = 0; i < popUpArray.size(); i++) {
							System.out.println("first one" + popUpArray.get(i).getCampaignid());
							if (popUpArray.get(i).getCampaignid() != campaign.getId().intValue()) {
								System.out.println("not matched campaign no need to remove this from json data" + popUpArray.get(i).getCampaignid());
								popUpArrayAfterRemove.add(popUpArray.get(i));
							}
						}
						System.out.println("after remove embedded array size==" + popUpArrayAfterRemove.size());

						socialLoungeService.displayJsonArray(popUpArrayAfterRemove);
						embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);
						popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);

					}

					System.out.println("*********** EMBEDDED ARRAY JSON***********");
					System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArrayAfterRemove));

					System.out.println("*********** POPUP ARRAY AFTER JSON***********");
					System.out.println(campaignScoreUtils.convertArrayToJson(popUpArrayAfterRemove));

					Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(
							campaignScoreUtils.convertArrayToJson(embeddedArrayAfterRemove), adsRequestDto.getUserId(), userdata.getUserState());

					Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArrayAfterRemove),
							adsRequestDto.getUserId(), userdata.getUserState());
					if (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
						socialLoungeService.updatecampaignAlgoStatus(adsRequestDto.getUserId(), "publish");
					}
					if (hasSavedEmbeddedJsonData == Boolean.FALSE && hasSavedPopupJsonData == Boolean.FALSE) {
						socialLoungeService.updatecampaignAlgoStatus(adsRequestDto.getUserId(), "no campaigns");
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

	@RequestMapping(value = "/v1/update/useraction", method = RequestMethod.POST, produces = "application/json")
	public void updateUserActionV1(@RequestBody CamapignAdsRequestDto adsRequestDto, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adsRequestDto.getUserId());
		Gson gson = new GsonBuilder().serializeNulls().create();
		Campaign campaign = campaignService.findCampaignById(adsRequestDto.getCampaignId());
		json.put("status", "FAILURE");
		if (userdata != null) {
			Boolean updatedAction = socialLoungeService.updateUserAction(adsRequestDto.getUserId(), adsRequestDto.getCampaignId(),
					adsRequestDto.getUserAction(), adsRequestDto.getLatitude(), adsRequestDto.getLongitude(), adsRequestDto.getAdviewDuration(),
					adsRequestDto.getTravelDistance(), adsRequestDto.getTravelTime());
			if (updatedAction == Boolean.TRUE) {
				json.put("status", "SUCCESS");
				json.put("message", "You got matches");
				Boolean isCampaignEligible = socialLoungeService.isCampaignEligibleForSelection(adsRequestDto.getCampaignId().intValue(),
						adsRequestDto.getUserId());
				if (isCampaignEligible == Boolean.FALSE) {
					List<UserCampaignScore> UserCampaignsjsonData = socialLoungeService.getJsonData(adsRequestDto.getUserId());
					if (UserCampaignsjsonData.isEmpty()) {
						// handle empty
					} else {
						CampaignScoreUtils campaignScoreUtils = new CampaignScoreUtils();
						List<CampaignScoreJson> embeddedArray = new ArrayList<CampaignScoreJson>();
						List<CampaignScoreJson> embeddedArrayAfterRemove = new ArrayList<CampaignScoreJson>();
						List<CampaignScoreJson> popUpArray = new ArrayList<CampaignScoreJson>();
						List<CampaignScoreJson> popUpArrayAfterRemove = new ArrayList<CampaignScoreJson>();

						if (campaign.getCampaignTypes().getId() == 1) {
							if (campaign.getDisplayStyle().equals("Embedded Advertising")) {
								embeddedArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
										campaign.getDisplayStyle());
								System.out.println("before remove embedded array size==" + embeddedArray.size());

								for (int i = 0; i < embeddedArray.size(); i++) {
									System.out.println("first one" + embeddedArray.get(i).getCampaignid());
									if (embeddedArray.get(i).getCampaignid() != campaign.getId().intValue()) {
										System.out.println("not matched campaign no need to remove this from json data" + embeddedArray.get(i).getCampaignid());
										embeddedArrayAfterRemove.add(embeddedArray.get(i));
									}
								}
								System.out.println("after remove embedded array size==" + embeddedArrayAfterRemove.size());

								socialLoungeService.displayJsonArray(embeddedArrayAfterRemove);

								embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);
								popUpArrayAfterRemove = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
										"Pop Up Advertising");
								popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);

							} else {
								popUpArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
										campaign.getDisplayStyle());
								for (int i = 0; i < popUpArray.size(); i++) {
									System.out.println("first one" + popUpArray.get(i).getCampaignid());
									if (popUpArray.get(i).getCampaignid() != campaign.getId().intValue()) {
										System.out.println("matched campaign need to remove this from json data" + popUpArray.get(i).getCampaignid());
										popUpArrayAfterRemove.add(popUpArray.get(i));
									}
								}
								System.out.println("after remove embedded array size==" + popUpArrayAfterRemove.size());

								socialLoungeService.displayJsonArray(popUpArrayAfterRemove);

								popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);
								embeddedArrayAfterRemove = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(),
										"Embedded Advertising");
								embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);

							}

						} else {
							embeddedArray = campaignScoreUtils
									.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(), "Embedded Advertising");
							for (int i = 0; i < embeddedArray.size(); i++) {
								System.out.println("first one" + embeddedArray.get(i).getCampaignid());
								if (embeddedArray.get(i).getCampaignid() != campaign.getId().intValue()) {
									System.out.println("not matched campaign no need to remove this from json data" + embeddedArray.get(i).getCampaignid());
									embeddedArrayAfterRemove.add(embeddedArray.get(i));
								}
							}
							System.out.println("after remove embedded array size==" + embeddedArrayAfterRemove.size());

							socialLoungeService.displayJsonArray(embeddedArrayAfterRemove);

							popUpArray = campaignScoreUtils.convertJsonToArray(UserCampaignsjsonData.get(0), userdata.getUserState(), "Pop Up Advertising");

							for (int i = 0; i < popUpArray.size(); i++) {
								System.out.println("first one" + popUpArray.get(i).getCampaignid());
								if (popUpArray.get(i).getCampaignid() != campaign.getId().intValue()) {
									System.out.println("not matched campaign no need to remove this from json data" + popUpArray.get(i).getCampaignid());
									popUpArrayAfterRemove.add(popUpArray.get(i));
								}
							}
							System.out.println("after remove embedded array size==" + popUpArrayAfterRemove.size());

							socialLoungeService.displayJsonArray(popUpArrayAfterRemove);
							embeddedArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(embeddedArrayAfterRemove);
							popUpArrayAfterRemove = campaignScoreUtils.computeCoeffProbability(popUpArrayAfterRemove);

						}

						System.out.println("*********** EMBEDDED ARRAY JSON***********");
						System.out.println(campaignScoreUtils.convertArrayToJson(embeddedArrayAfterRemove));

						System.out.println("*********** POPUP ARRAY AFTER JSON***********");
						System.out.println(campaignScoreUtils.convertArrayToJson(popUpArrayAfterRemove));

						Boolean hasSavedEmbeddedJsonData = socialLoungeService.saveEmbeddedJsonData(
								campaignScoreUtils.convertArrayToJson(embeddedArrayAfterRemove), adsRequestDto.getUserId(), userdata.getUserState());

						Boolean hasSavedPopupJsonData = socialLoungeService.savePopUpJsonData(campaignScoreUtils.convertArrayToJson(popUpArrayAfterRemove),
								adsRequestDto.getUserId(), userdata.getUserState());
						if (hasSavedEmbeddedJsonData || hasSavedPopupJsonData) {
							socialLoungeService.updatecampaignAlgoStatus(adsRequestDto.getUserId(), "publish");
						}
						if (hasSavedEmbeddedJsonData == Boolean.FALSE && hasSavedPopupJsonData == Boolean.FALSE) {
							socialLoungeService.updatecampaignAlgoStatus(adsRequestDto.getUserId(), "no campaigns");
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

	@Transactional
	@RequestMapping(value = "/popup", method = RequestMethod.POST, produces = "application/json")
	public void requestPopupBody(@RequestBody AdRequestDto adRequestDto, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Campaign campaign = campaignService.findCampaignById(adRequestDto.getCampaignId());
		if (campaign != null && CampaignStatus.LIVE.equals(campaign.getStatus())) {
			json.put("status", "SUCCESS");
			AdResponseDto dto = new AdResponseDto(campaign);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("campaign", jsonObject);

		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/popup", method = RequestMethod.POST, produces = "application/json")
	public void requestPopupBodyV1(@RequestBody AdRequestDto adRequestDto, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Campaign campaign = campaignService.findCampaignById(adRequestDto.getCampaignId());
		if (campaign != null && CampaignStatus.LIVE.equals(campaign.getStatus())) {
			json.put("status", "SUCCESS");
			AdResponseDto dto = new AdResponseDto(campaign);
			Gson gson = new GsonBuilder().serializeNulls().create();
			String json2 = gson.toJson(dto, dto.getClass());
			JSONObject jsonObject = new JSONObject(json2);
			json.put("campaign", jsonObject);

		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/testcampaigns", method = RequestMethod.POST, produces = "application/json")
	public void getCampaignTestAds(@RequestBody CamapignAdsRequestDto adsRequestDto, BindingResult result, HttpServletRequest request,
			HttpServletResponse response) throws JSONException, IOException {
		System.out.println("called test campaign,  userid is ===" + adsRequestDto.getUserId());
		System.out.println("called test campaign,  latitude is ===" + adsRequestDto.getLatitude());
		System.out.println("called test campaign,  longitude is ===" + adsRequestDto.getLongitude());
		System.out.println("called test campaign,  count is ===" + adsRequestDto.getCount());
		System.out.println("called test campaign,  ad type is ===" + adsRequestDto.getAdType());

		/*
		 * JSONObject json = new JSONObject(); User userdata =
		 * userService.find(adsRequestDto.getUserId()); Gson gson = new
		 * GsonBuilder().serializeNulls().create(); if (userdata != null) {
		 * String cpmAdType = adsRequestDto.getAdType(); if
		 * (StringUtils.equalsIgnoreCase("popup", cpmAdType)) { cpmAdType =
		 * "Pop Up Advertising"; } else if
		 * (StringUtils.equalsIgnoreCase("embedded", cpmAdType) ||
		 * StringUtils.equalsIgnoreCase("cpm", cpmAdType)) { cpmAdType =
		 * "Embedded Advertising"; } int randomNumber =
		 * CampaignScoreUtils.randInt(1, 100); GlobalSwitch swithAction =
		 * userService.checkGlobalSwitch(); List<Long> MatchedCampaignId =
		 * socialLoungeService.getCampaignForPublish(randomNumber,
		 * userdata.getId(), userdata.getUserState(), cpmAdType); if
		 * (CollectionUtils.isNotEmpty(MatchedCampaignId) &&
		 * swithAction.getSwitchAction() == Boolean.TRUE) { List<Campaign>
		 * campaignsByIds =
		 * campaignService.getCampaignsByIds(MatchedCampaignId); AdResponseDto
		 * dto = new AdResponseDto(campaignsByIds); List<AdResponseDto>
		 * adResponseDtos = dto.getAdResponseDtos(); String json2 =
		 * gson.toJson(adResponseDtos, adResponseDtos.getClass()); JSONArray
		 * jsonObject = new JSONArray(json2); json.put("campaigns", jsonObject);
		 * json.put("status", "SUCCESS"); json.put("message",
		 * "You got matches"); } else { json.put("status", "FAILURE");
		 * json.put("message", "No campaign Available"); } List<Long>
		 * matchedCamapignIds = socialLoungeService.getMatchedCamapignIds
		 * (adsRequestDto.getUserId(), adsRequestDto.getLatitude(),
		 * adsRequestDto.getLongitude(), adsRequestDto.getCount(), cpmAdType); }
		 * else { json.put("status", "FAILURE"); json.put("message",
		 * "No matches Available"); }
		 * response.setContentType("application/json"); PrintWriter out =
		 * response.getWriter(); out.write(json.toString());
		 * System.out.println(json.toString());
		 */

		JSONObject json = new JSONObject();
		int count = adsRequestDto.getCount();
		Gson gson = new GsonBuilder().serializeNulls().create();
		GlobalSwitch swithAction = userService.checkGlobalSwitch();
		if (swithAction.getSwitchAction() == Boolean.TRUE) {
			if (adsRequestDto.getAdType().equals("cpm")) {
				List<Campaign> campaignList = campaignService.findTestCpmCampaigns(adsRequestDto.getAdCount());
				if (CollectionUtils.isNotEmpty(campaignList)) {
					JSONArray array = new JSONArray();
					for (Campaign campaign : campaignList) {
						JSONObject object = new JSONObject();
						object.put("id", campaign.getId());
						object.put("cpmimage", campaign.getFormedMediaUrl());
						object.put("campaignTypeId", campaign.getCampaignTypes().getId());
						object.put("link", campaign.getLink());
						array.put(object);
					}
					json.put("status", "SUCCESS");
					json.put("cpmCampaigns", array);
				} else {
					json.put("status", "FAILURE");
					json.put("message", "No campaigns data found");
				}
			}
			if (adsRequestDto.getAdType().equals("embedded")) {
				List<Campaign> campaignList = campaignService.findTestEmbedCampaigns(adsRequestDto.getCount());

				if (CollectionUtils.isNotEmpty(campaignList)) {
					AdResponseDto dto = new AdResponseDto(campaignList);
					List<AdResponseDto> adResponseDtos = dto.getAdResponseDtos();
					String json2 = gson.toJson(adResponseDtos, adResponseDtos.getClass());
					JSONArray jsonObject = new JSONArray(json2);
					json.put("campaigns", jsonObject);
					json.put("status", "SUCCESS");
					json.put("message", "You got matches");
					/*
					 * JSONArray array = new JSONArray(); for (Campaign campaign
					 * : campaignList) { JSONObject object = new JSONObject();
					 * object.put("id", campaign.getId());
					 * object.put("embedImage",
					 * campaign.getFormedEmbeddedMediaUrl());
					 * object.put("popupImage",
					 * campaign.getFormedPopuopMediaUrl());
					 * object.put("description", campaign.getDescription());
					 * array.put(object); } json.put("status", "SUCCESS");
					 * json.put("embeddedCampaigns", array);
					 */
				} else {
					json.put("status", "FAILURE");
					json.put("message", "No campaigns data found");
				}
			}
			if (adsRequestDto.getAdType().equals("popup")) {
				List<Campaign> campaignList = campaignService.findTestPopupCampaigns(adsRequestDto.getCount());
				if (CollectionUtils.isNotEmpty(campaignList)) {
					AdResponseDto dto = new AdResponseDto(campaignList);
					List<AdResponseDto> adResponseDtos = dto.getAdResponseDtos();
					String json2 = gson.toJson(adResponseDtos, adResponseDtos.getClass());
					JSONArray jsonObject = new JSONArray(json2);
					json.put("campaigns", jsonObject);
					json.put("status", "SUCCESS");
					json.put("message", "You got matches");
					/*
					 * JSONArray array = new JSONArray(); for (Campaign campaign
					 * : campaignList) { JSONObject object = new JSONObject();
					 * object.put("id", campaign.getId());
					 * object.put("embedImage",
					 * campaign.getFormedEmbeddedMediaUrl());
					 * object.put("popupImage",
					 * campaign.getFormedPopuopMediaUrl());
					 * object.put("description", campaign.getDescription());
					 * array.put(object); } json.put("status", "SUCCESS");
					 * json.put("popupCampaigns", array);
					 */
				} else {
					json.put("status", "FAILURE");
					json.put("message", "No campaigns data found");
				}
			}
		} else {
			json.put("status", "FAILURE");
			json.put("message", "No Campaigns are Serving");
		}

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/checkdata", method = RequestMethod.POST, produces = "application/json")
	public void apiCheckDataByAirport(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Terminal> terminals = locationService.getTerminals(airport.getIataCode());
			List<MerchantIndustry> industries = locationService.getIndustries(airport.getIataCode());
			List<Store> stores = locationService.getSpecialOfferStores(airport.getIataCode());
			json.put("status", "SUCCESS");
			if (CollectionUtils.isEmpty(stores)) {
				json.put("chekStores", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(stores)) {
				json.put("chekStores", Boolean.TRUE);
			}
			if (CollectionUtils.isEmpty(terminals)) {
				json.put("chekTerminals", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(terminals)) {
				json.put("chekTerminals", Boolean.TRUE);
			}
			if (CollectionUtils.isEmpty(industries)) {
				json.put("chekIndustries", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(industries)) {
				json.put("chekIndustries", Boolean.TRUE);
			}

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@Transactional
	@RequestMapping(value = "/v1/checkdata", method = RequestMethod.POST, produces = "application/json")
	public void apiCheckDataByAirportV1(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		Airport airportData = locationService.findAirportByCode(airport.getIataCode());
		if (airportData != null) {
			List<Terminal> terminals = locationService.getTerminals(airport.getIataCode());
			List<MerchantIndustry> industries = locationService.getIndustries(airport.getIataCode());
			List<Store> stores = locationService.getSpecialOfferStores(airport.getIataCode());
			json.put("status", "SUCCESS");
			if (CollectionUtils.isEmpty(stores)) {
				json.put("chekStores", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(stores)) {
				json.put("chekStores", Boolean.TRUE);
			}
			if (CollectionUtils.isEmpty(terminals)) {
				json.put("chekTerminals", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(terminals)) {
				json.put("chekTerminals", Boolean.TRUE);
			}
			if (CollectionUtils.isEmpty(industries)) {
				json.put("chekIndustries", Boolean.FALSE);
			} else if (CollectionUtils.isNotEmpty(industries)) {
				json.put("chekIndustries", Boolean.TRUE);
			}

		} else {
			json.put("status", "FAILURE");
			json.put("message", "No airport data found");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/statenotify", method = RequestMethod.POST, produces = "application/json")
	public void stateNotification(@RequestBody AdRequestDto adRequestDto, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adRequestDto.getUserId());
		String userExistedState = adRequestDto.getUserState();
		String userPresentStateByLocation = socialLoungeService.getUserStateByLocation(adRequestDto.getUserId(), adRequestDto.getLatitude(),
				adRequestDto.getLongitude());
		String userPresentAirportByLocation = socialLoungeService.getCurrentAirportByLocation(adRequestDto.getLatitude(), adRequestDto.getLongitude());
		if (userExistedState.equals("NO_TRIP@AIRPORT") && userPresentStateByLocation.equals("TRIP@AIRPORT")) {
			userdata.setUserState(userPresentStateByLocation);
			userdata.setCurrentAirport(userPresentAirportByLocation);
			userdata.setCampaignAlgoStatus("compute");
			userService.update(userdata);
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/statenotify", method = RequestMethod.POST, produces = "application/json")
	public void stateNotificationV1(@RequestBody AdRequestDto adRequestDto, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(adRequestDto.getUserId());
		String userExistedState = adRequestDto.getUserState();
		String userPresentStateByLocation = socialLoungeService.getUserStateByLocation(adRequestDto.getUserId(), adRequestDto.getLatitude(),
				adRequestDto.getLongitude());
		String userPresentAirportByLocation = socialLoungeService.getCurrentAirportByLocation(adRequestDto.getLatitude(), adRequestDto.getLongitude());
		if (userExistedState.equals("NO_TRIP@AIRPORT") && userPresentStateByLocation.equals("TRIP@AIRPORT")) {
			userdata.setUserState(userPresentStateByLocation);
			userdata.setCurrentAirport(userPresentAirportByLocation);
			userdata.setCampaignAlgoStatus("compute");
			userService.update(userdata);
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
		}
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/searchstores", method = RequestMethod.POST, produces = "application/json")
	public void apiStoreSearch(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(airport.getUserId());
		Date dateTime = new Date();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userdata.getId());
		logTrack.setUserLogType("STORESEARCH");
		logTrack.setUpdateAt(dateTime);
		logTrack.setUserSessionType(userdata.getUserState());
		logTrack.setLogField2(airport.getStoreName());
		logTrack.setUserSessionToken(userdata.getSessionToken());
		logTrack.setLatitude(airport.getLatitude());
		logTrack.setLongitude(airport.getLongitude());
		userService.save(logTrack);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/storedetails", method = RequestMethod.POST, produces = "application/json")
	public void apiStoreDetails(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(airport.getUserId());
		Date dateTime = new Date();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userdata.getId());
		logTrack.setUserLogType("STOREDETAILS");
		logTrack.setUpdateAt(dateTime);
		logTrack.setUserSessionType(userdata.getUserState());
		logTrack.setLogField1(airport.getStoreId());
		logTrack.setUserSessionToken(userdata.getSessionToken());
		logTrack.setLatitude(airport.getLatitude());
		logTrack.setLongitude(airport.getLongitude());
		if (airport.getHasLiveCampaign()) {
			logTrack.setLogField2("YES");
		} else {
			logTrack.setLogField2("NO");
		}
		userService.save(logTrack);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/storemap", method = RequestMethod.POST, produces = "application/json")
	public void apiStoreMap(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response) throws JSONException,
			IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(airport.getUserId());
		Date dateTime = new Date();
		LoggingTrack logTrack = new LoggingTrack();
		logTrack.setUserId(userdata.getId());
		logTrack.setUserLogType("STOREMAP");
		logTrack.setUpdateAt(dateTime);
		logTrack.setLogField1(airport.getStoreId());
		logTrack.setUserSessionToken(userdata.getSessionToken());
		logTrack.setLatitude(airport.getLatitude());
		logTrack.setLongitude(airport.getLongitude());
		userService.save(logTrack);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

	@RequestMapping(value = "/v1/enterinfo", method = RequestMethod.POST, produces = "application/json")
	public void apiEnterInfo(@RequestBody Airport airport, BindingResult result, HttpServletRequest request, HttpServletResponse response)
			throws JSONException, IOException {
		JSONObject json = new JSONObject();
		User userdata = userService.find(airport.getUserId());
		if (userdata != null) {
			mail.sendEnterInfoMail(airport.getUserId(), airport.getStoreId(), airport.getInfoText());
			json.put("status", "SUCCESS");
		} else {
			json.put("status", "FAILURE");
		}
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		System.out.println(json.toString());
	}

}

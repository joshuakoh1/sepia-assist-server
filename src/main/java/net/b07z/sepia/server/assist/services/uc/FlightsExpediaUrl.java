package net.b07z.sepia.server.assist.services.uc;

import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.assist.tools.DateTimeConverters;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.URLBuilder;

import org.json.simple.JSONObject;

/**
 * Flights search with Expedia.
 * 
 * @author Florian Quirin
 *
 */
public class FlightsExpediaUrl implements ServiceInterface{
	
	//some statics
	public static enum FType{
		oneway, roundtrip;
	}
	public static enum FClass{
		economy, ecoPlus, businessClass, firstClass;
	}
	
	//default values for parameters
	private static final int ADULTS = 1;
	private static final String F_TYPE = FType.oneway.name();
	private static final String F_CLASS = FClass.economy.name();
	
	//info
	public ServiceInfo getInfo(String language){
		//type
		ServiceInfo info = new ServiceInfo(Type.link, Content.redirect, false);
		
		//TODO: more common info:
		//- open question at the end? 
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.LOCATION_START)
				.setRequired(true)
				.setQuestion("flights_ask_start_0a");
		Parameter p2 = new Parameter(PARAMETERS.LOCATION_END)
				.setRequired(true)
				.setQuestion("flights_ask_end_0a");
		Parameter p3 = new Parameter(PARAMETERS.TIME)
				.setRequired(true)
				.setQuestion("flights_ask_time_0a");
		info.addParameter(p1).addParameter(p2).addParameter(p3);
		//optional
		Parameter p4 = new Parameter(PARAMETERS.ADULTS, ADULTS);
		Parameter p5 = new Parameter(PARAMETERS.FLIGHT_TYPE, F_TYPE);
		Parameter p6 = new Parameter(PARAMETERS.FLIGHT_CLASS, F_CLASS);
		info.addParameter(p4).addParameter(p5).addParameter(p6);
		
		//Answers:
		info.addSuccessAnswer("flights_1a")
			.addFailAnswer("abort_0b")
			.addAnswerParameters("start", "end", "date"); 		//be sure to use the same parameter names as in resultInfo
		
		return info;
	}

	//result
	public ServiceResult getResult(NluResult NLU_result){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(NLU_result, getInfo(""));
		
		//get required parameters
		JSONObject timeJSON = NLU_result.getRequiredParameter(PARAMETERS.TIME).getData();
		JSONObject startJSON = NLU_result.getRequiredParameter(PARAMETERS.LOCATION_START).getData();
		JSONObject endJSON = NLU_result.getRequiredParameter(PARAMETERS.LOCATION_END).getData();
		//get optional parameters
		Parameter adults = NLU_result.getOptionalParameter(PARAMETERS.ADULTS, ADULTS);
		Parameter fType = NLU_result.getOptionalParameter(PARAMETERS.FLIGHT_TYPE, F_TYPE);
		Parameter fClass = NLU_result.getOptionalParameter(PARAMETERS.FLIGHT_CLASS, F_CLASS);
				
		//parameter adaptation to service specific format:
		
		//TIME
		String date = (String) timeJSON.get(InterviewData.DATE_DAY);
		String apiTime = DateTimeConverters.convertDateFormat(date, Config.defaultSdf, "dd.MM.yyyy");
		String isoTime = DateTimeConverters.convertDateFormat(date, Config.defaultSdf, "yyyy-MM-dd");
		String time_to_say = DateTimeConverters.getSpeakableDate(date, Config.defaultSdf, api.language);
		api.resultInfoPut("date", time_to_say);
		api.resultInfoPut("dateISO", isoTime);
		api.resultInfoPut("dateEnd", "");
		api.resultInfoPut("dateEndISO", "");
		
		//LOCATION - Start
		String apiStart = (String) startJSON.get(InterviewData.LOCATION_CITY);
		String start_to_say = (String) startJSON.get(InterviewData.LOCATION_CITY);
		api.resultInfoPut("start", start_to_say);
		api.resultInfoPut("airportStart", start_to_say.substring(0, Math.min(3, start_to_say.length())).toUpperCase()); //TODO: get real
		
		//LOCATION - End
		String apiEnd = (String) endJSON.get(InterviewData.LOCATION_CITY);
		String end_to_say = (String) endJSON.get(InterviewData.LOCATION_CITY);
		api.resultInfoPut("end", end_to_say);
		api.resultInfoPut("airportEnd", end_to_say.substring(0, Math.min(3, end_to_say.length())).toUpperCase());		//TODO: get real
		
		//ADULTS
		String adultsN = adults.getDataFieldOrDefault(InterviewData.VALUE).toString();
		api.resultInfoPut("adults", adultsN);
		
		//FLIGHT TYPE
		String apiFType = fType.getDataFieldOrDefault(InterviewData.VALUE).toString();
		if (!apiFType.equals("oneway")){
			apiFType = "oneway";			//force "oneway" as this is the only supported type right now
		}
		api.resultInfoPut("type", apiFType);
		
		//FLIGHT CLASS (not used right now)
		String apiFClass = fClass.getDataFieldOrDefault(InterviewData.VALUE).toString();
		if (!apiFClass.equals("economy")){
			apiFClass = "economy";			//force "economy" (just to keep the form ^^)
		}
		api.resultInfoPut("class", apiFClass);
		
		Debugger.println("cmd: flights, from " + apiStart + ", to " + apiEnd + ", at " + apiTime, 2);		//debug
		
		//build URL
		String flight_url;
		flight_url = URLBuilder.getString("https://www.expedia.de/Flights-Search?trip=" + apiFType + "&leg1=",
						"from:", apiStart,
						",to:", apiEnd,
						",departure:", apiTime);
		flight_url += "TANYT&passengers=children:0,adults:" + adultsN + ",seniors:0,infantinlap:N&mode=search";
		//System.out.println("URL: " + flight_url); 		//debug
		
		//build card
		Card card = new Card(Card.TYPE_SINGLE);
		JSONObject card_data = new JSONObject();
			JSON.add(card_data, "title", "Expedia");
			JSON.add(card_data, "desc", "Expedia meta search");
		String card_url = flight_url;
		String card_img = Config.urlWebImages + "brands/" + "logo-expedia.png";
		card.addElement(ElementType.mobilityR, card_data, null, null, "", card_url, card_img, null, null);
		//add it
		api.addCard(card.getJSON()); 	//pre-Alpha: api.cardInfo = card.cardInfo;
		api.hasCard = true;
		
		//all clear?
		api.status = "success";
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
				
		//return result_JSON.toJSONString();
		return result;
	}

}
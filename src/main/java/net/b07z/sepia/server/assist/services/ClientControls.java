package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A service to trigger client control actions, usually called via direct commands 
 * defined in the teach-UI or by pre-defined sentences.
 * 
 * @author Florian Quirin
 *
 */
public class ClientControls implements ServiceInterface{
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		//GERMAN
		if (lang.equals(Language.DE.toValue())){
			samples.add("Einstellungen öffnen.");
			
		//OTHER
		}else{
			samples.add("Open settings.");
		}
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		ServiceInfo info = new ServiceInfo(Type.plain, Content.data, false);
		
		String DE = LANGUAGES.DE;
		String EN = LANGUAGES.EN;
		
		//Direct-match trigger sentences in different languages:
		/* -- This only works in SDK services (because it is written into user account on upload --
		info.addCustomTriggerSentence("Einstellungen öffnen.", DE)
			;
		info.addCustomTriggerSentence("Open settings.", EN)
			;
		*/
		//Regular expression triggers:
		info.setCustomTriggerRegX("^(einstellungen oeffnen)$", DE);
		info.setCustomTriggerRegX("^(open settings)$", EN);
		info.setCustomTriggerRegXscoreBoost(3);		//boost service a bit to increase priority over similar ones
		
		//Parameters:
		//required
		Parameter p1 = new Parameter(PARAMETERS.ACTION)
				.setRequired(true)
				.setQuestion("default_ask_action_0a");
		Parameter p2 = new Parameter(PARAMETERS.CLIENT_FUN)
				.setRequired(true)
				.setQuestion("client_controls_ask_fun_0a");
		info.addParameter(p1).addParameter(p2);
		
		//Default answers
		info.addSuccessAnswer("ok_0c")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a");
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//get required parameters
		Parameter actionP = nluResult.getRequiredParameter(PARAMETERS.ACTION);
		String action = actionP.getValueAsString().replaceAll("^<|>$", "").trim();
		boolean isActionOpen = (action.equals(Action.Type.show.name()) || action.equals(Action.Type.on.name()));
		boolean isActionClose = (action.equals(Action.Type.remove.name()) || action.equals(Action.Type.off.name()));
		
		Parameter controlFunP = nluResult.getRequiredParameter(PARAMETERS.CLIENT_FUN);
		String controlFun = controlFunP.getValueAsString().replaceAll("^<|>$", "").trim();
				
		//This service basically cannot fail here ... only inside client
		
		//Just for demo purposes we add a button-action with a link to the SDK
		api.addAction(ACTIONS.CLIENT_CONTROL_FUN);
		api.putActionInfo("fun", "settings");
		api.putActionInfo("controlData", JSON.make("action", "open"));
		
		//... and we also add a demo card
		Card card = new Card(Card.TYPE_SINGLE);
		card.addElement(ElementType.link, 
				JSON.make("title", "S.E.P.I.A." + ":", "desc", "Hello World!"),
				null, null, "", 
				"https://sepia-framework.github.io/", 
				"https://sepia-framework.github.io/img/icon.png", 
				null, null);
		//JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}

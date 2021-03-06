package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter to find rooms typically found in a smart home like living-room etc.
 * 
 * @author Florian Quirin
 * 
 */
public class Room implements ParameterHandler{

	//-----data-----
	
	/**
	 * Rooms typically found in a smart home. 
	 */
	public static enum Types{
		livingroom,
		diningroom,
		kitchen,
		bedroom,
		bath,
		study,
		office,
		childsroom,
		garage,
		basement,
		garden,
		shack,
		hallway,
		entrance,
		sunroom, //winter garden
		terrace,
		balcony,
		attic,
		other,
		unassigned	//must be assigned directly
	}
	//TODO: expose this to an endpoint to client and control HUB can download it
	
	//Parameter local type names
	public static HashMap<String, String> types_de = new HashMap<>();
	public static HashMap<String, String> types_en = new HashMap<>();
	static {
		types_de.put("livingroom", "im Wohnzimmer");
		types_de.put("diningroom", "im Esszimmer");
		types_de.put("kitchen", "in der Küche");
		types_de.put("bedroom", "im Schlafzimmer");
		types_de.put("bath", "im Badezimmer");
		types_de.put("study", "im Arbeitszimmer");
		types_de.put("office", "im Büro");
		types_de.put("childsroom", "im Kinderzimmer");
		types_de.put("garage", "in der Garage");
		types_de.put("basement", "im Keller");
		types_de.put("garden", "im Garten");
		types_de.put("sunroom", "im Wintergarten");
		types_de.put("terrace", "auf der Terrasse");
		types_de.put("balcony", "auf dem Balkon");
		types_de.put("shack", "im Schuppen");
		types_de.put("hallway", "im Flur");
		types_de.put("entrance", "am Eingang");
		types_de.put("attic", "auf dem Dachboden");
		types_de.put("other", "am Standort");	//"am erwähnten Ort"
		types_de.put("unassigned", "");
		
		types_en.put("livingroom", "in the living room");
		types_en.put("diningroom", "in the dining room");
		types_en.put("kitchen", "in the kitchen");
		types_en.put("bedroom", "in the bedroom");
		types_en.put("bath", "in the bath");
		types_en.put("study", "in the study room");
		types_en.put("office", "in the office");
		types_en.put("childsroom", "in the child's room");
		types_en.put("garage", "in the garage");
		types_en.put("basement", "in the basement");
		types_en.put("garden", "in the garden");
		types_en.put("sunroom", "in the sunroom");
		types_en.put("terrace", "on the terrace");
		types_en.put("balcony", "on the balcony");
		types_en.put("shack", "in the shack");
		types_en.put("hallway", "in the hallway");
		types_en.put("entrance", "at the entrance");
		types_en.put("attic", "on the attic");
		types_en.put("other", "at location"); 	//"at the mentioned location"
		types_en.put("unassigned", "");
	}
	
	/**
	 * Translate generalized value (e.g. &lt;kitchen&gt;) to a context based, useful local name (e.g. in der Küche).
	 * If generalized value is unknown returns empty string
	 * @param type - generalized type value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String type, String language){
		type = type.replaceAll("^<|>$", "").trim();
		String localName = "";
		if (language.equals(LANGUAGES.DE)){
			localName = types_de.get(type);
		}else if (language.equals(LANGUAGES.EN)){
			localName = types_en.get(type);
		}
		if (localName == null){
			Debugger.println(Room.class.getSimpleName() + " - getLocal() has no '" + language + "' version for '" + type + "'", 3);
			return "";
		}
		return localName;
	}
	//----------------
	
	User user;
	String language;
	NluInput nluInput;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.user = nluInput.user;
		this.language = nluInput.language;
		this.nluInput = nluInput;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.user = nluResult.input.user;
		this.language = nluResult.language;
		this.nluInput = nluResult.input;
	}
	
	/**
	 * Search normalized string for raw type.
	 */
	public static String getType(String input, String language){
		String type = "";
		//German
		if (language.matches(LANGUAGES.DE)){
			type = NluTools.stringFindFirst(input, 
					"wohn( |-|)zimmer(n|)|"
					+ "esszimmer(n|)|"
					+ "kueche(n|)|"
					+ "badezimmer(n|)|bad|baedern|"
					+ "schlaf( |-|)zimmer(n|)|"
					+ "(arbeits|studier|herren)( |-|)(zimmer(n|)|raum|raeumen)|"
					+ "buero(s|)|office|"
					+ "(kinder|baby|wickel)( |-|)(zimmer|stube)(n|)|"
					+ "garage|auto(-| |)schuppen|"
					+ "keller|"
					+ "schuppen|gartenhaus|"
					+ "winter(-| |)(garten|gaerten)|glasveranda|"
					+ "garten|"
					+ "terrasse(n|)|veranda(s|)|patio|"
					+ "balkon(en|es|e|s|)|"
					+ "(haus|)flur|korridor|diele|"
					+ "(haupt|)eingang(stuer|)|haustuer(e|)|"
					+ "dach(boden|speicher)|loft|"
					//+ "andere(n|es|r|)( |-|)(zimmer|raum|raeumen)"
					+ "zimmer(n|)|raum|raeumen|kammer(n|)|(stand|)ort(en|)"
				+ "");
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, 
					"living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room(s|)|"
					+ "dining( |-|)room(s|)|"
					+ "kitchen(s|)|"
					+ "bath(ing|)( |-|)room(s|)|bath(s|)|powder(-|)room(s|)|"
					+ "bed(-|)(room|chamber)(s|)|"
					+ "(study|work)(-|)(room|chamber)(s|)|study|"
					+ "office|"
					+ "(child(s|)|children(s|)|baby)( |-|)room(s|)|nursery|"
					+ "garage|carhouse|"
					+ "basement|"
					+ "shack(s|)|shed(s|)|"
					+ "winter(-| |)garden|sun(-| |)room|conservatory|solarium|"
					+ "garden|"
					+ "terrace(s|)|patio(s|)|porch(es|)|"
					+ "balcon(y|ies)|"
					+ "hallway|corridor|"
					+ "entrance|front(-| |)door|doorway|"
					+ "attic|loft|"
					//+ "other (room|chamber)(s|)|"
					+ "(room|chamber|location)(s|)"
				+ "");
			
		}
		//System.out.println("Type: " + type); 		//debug
		return type;
	}

	@Override
	public String extract(String input) {
		String typeAndTag;
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.ROOM);
		if (pr != null){
			typeAndTag = pr.getExtracted();
			this.found = pr.getFound();
			
			return typeAndTag;
		}
				
		String room = getType(input, language);
		if (room.isEmpty()){
			return "";
		}else{
			//check for room number
			String roomWithNumber;
			if (language.matches(LANGUAGES.DE)){
				roomWithNumber = NluTools.stringFindFirst(input, room + "( (mit der |mit |)nummer|) \\d+");
			}else{
				roomWithNumber = NluTools.stringFindFirst(input, room + "( (with the |with |)number|) \\d+");
			}
			if (!roomWithNumber.isEmpty()){
				this.found = roomWithNumber;
			}else{
				this.found = room;
			}
		}
		
		//classify into types:
		
		String roomTypeTag = null;
		
		if (NluTools.stringContains(room, "wohn( |-|)zimmer(n|)|"
				+ "living( |-|)room(s|)|parlo(u|)r(s|)|lounge(s|)|family(-| )room")){
			roomTypeTag = "<" + Types.livingroom.name() + ">";
			
		}else if (NluTools.stringContains(room, "esszimmer(n|)|"
				+ "dining( |-|)room(s|)")){
			roomTypeTag =  "<" + Types.diningroom.name() + ">";
			
		}else if (NluTools.stringContains(room, "kueche(n|)|"
				+ "kitchen(s|)")){
			roomTypeTag =  "<" + Types.kitchen.name() + ">";
			
		}else if (NluTools.stringContains(room, "badezimmer(n|)|bad|"
				+ "bath(ing|)( |-|)room(s|)|bath|powder(-|)room(s|)")){
			roomTypeTag =  "<" + Types.bath.name() + ">";
			
		}else if (NluTools.stringContains(room, "schlaf( |-|)zimmer(n|)|"
				+ "bed(-|)(room|chamber)(s|)")){
			roomTypeTag =  "<" + Types.bedroom.name() + ">";
			
		}else if (NluTools.stringContains(room, "(arbeits|studier|herren)( |-|)(zimmer(n|)|raum|raeumen)|"
				+ "(study|work)(-|)(room|chamber)(s|)|study")){
			roomTypeTag =  "<" + Types.study.name() + ">";
			
		}else if (NluTools.stringContains(room, "buero(s|)|"
				+ "office")){
			roomTypeTag =  "<" + Types.office.name() + ">";
			
		}else if (NluTools.stringContains(room, "(kinder|baby|wickel)( |-|)(zimmer|stube)(n|)|"
				+ "(child(s|)|children(s|)|baby)( |-|)room(s|)|nursery(s|)")){
			roomTypeTag =  "<" + Types.childsroom.name() + ">";
			
		}else if (NluTools.stringContains(room, "garage|auto(-| |)schuppen|"
				+ "carhouse")){
			roomTypeTag =  "<" + Types.garage.name() + ">";
			
		}else if (NluTools.stringContains(room, "keller|"
				+ "basement")){
			roomTypeTag =  "<" + Types.basement.name() + ">";
			
		}else if (NluTools.stringContains(room, "schuppen|gartenhaus|"
				+ "shack(s|)|shed(s|)")){
			roomTypeTag =  "<" + Types.shack.name() + ">";
			
		}else if (NluTools.stringContains(room, "winter(-| |)(garten|gaerten)|glasveranda|"
				+ "winter(-| |)garden|sun(-| |)room|conservatory|solarium")){
			roomTypeTag =  "<" + Types.sunroom.name() + ">";
			
		}else if (NluTools.stringContains(room, "garten|"
				+ "garden")){
			roomTypeTag =  "<" + Types.garden.name() + ">";
			
		}else if (NluTools.stringContains(room, "terrasse(n|)|veranda(h|)(s|)|patio|"
				+ "terrace(s|)|patio(s|)|porch(es|)")){
			roomTypeTag =  "<" + Types.terrace.name() + ">";
			
		}else if (NluTools.stringContains(room, "balkon(en|es|e|s|)|"
				+ "balcon(y|ies)")){
			roomTypeTag =  "<" + Types.balcony.name() + ">";
			
		}else if (NluTools.stringContains(room, "(haus|)flur|korridor|diele|"
				+ "hallway|corridor")){
			roomTypeTag =  "<" + Types.hallway.name() + ">";
			
		}else if (NluTools.stringContains(room, "(haupt|)eingang(stuer|)|haustuer(e|)|"
				+ "entrance|front(-| |)door|doorway")){
			roomTypeTag =  "<" + Types.entrance.name() + ">";
			
		}else if (NluTools.stringContains(room, "dach(boden|speicher)|"
				+ "attic|loft")){
			roomTypeTag =  "<" + Types.attic.name() + ">";
			
		/*}else if (NluTools.stringContains(room, "andere(n|es|r|)( |-|)(zimmer(n|)|raum|raeumen)|"
				+ "other room(s|)")){
			return "<" + Types.other.name() + ">";*/
			
		/*}else if (NluTools.stringContains(room, "(zimmer(n|)|raum|raeumen|kammer(n|))|"
				+ "(room|chamber)(s|)")){
			roomTypeTag =  "<" + Types.other.name() + ">";*/

		}else{
			roomTypeTag =  "<" + Types.other.name() + ">";
		}
		
		//reconstruct original phrase to get proper item names
		Normalizer normalizer = Config.inputNormalizers.get(this.language);
		String tag = normalizer.reconstructPhrase(nluInput.textRaw, this.found);
		
		typeAndTag = roomTypeTag + ";;" + tag;
		
		//store it
		pr = new ParameterResult(PARAMETERS.ROOM, typeAndTag, this.found);
		nluInput.addToParameterResultStorage(pr);
		
		return typeAndTag;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}
	
	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		if (language.equals(LANGUAGES.DE)){
			found = "(der |die |das |den |dem |(m|)ein(en|em|er|e) |)" + found;
		}else{
			found = "(a |the |)" + found;
		}
		return NluTools.stringRemoveFirst(input, found);
	}
	
	@Override
	public String responseTweaker(String input){
		if (language.equals(LANGUAGES.DE)){
			return input.replaceAll(".*\\b((m|)ein(en|em|er|e|)|der|die|das|den|dem|ne|ner)\\b", "").trim();
		}else{
			return input.replaceAll(".*\\b(a|the)\\b", "").trim();
		}
	}

	@Override
	public String build(String input) {
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		}
		
		//expects a type!
		String roomFound = "";
		String roomIndexStr = "";
		if (input.contains(";;")){
			String[] typeAndInfo = input.split(";;");
			if (typeAndInfo.length == 2){
				roomFound = typeAndInfo[1];
				roomIndexStr = NluTools.stringFindFirst(roomFound, "\\b\\d+\\b");
				input = typeAndInfo[0];
			}else{
				input = typeAndInfo[0];
			}
		}
		
		//expects a type
		String commonValue = input.replaceAll("^<|>$", "").trim();
		String localValue = getLocal(input, language);
		
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, commonValue);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, localValue);
			JSON.add(itemResultJSON, InterviewData.ROOM_TAG, roomFound);
		//add device index
		if (!roomIndexStr.isEmpty()){
			int roomIndex = Integer.parseInt(roomIndexStr);
			JSON.add(itemResultJSON, InterviewData.ITEM_INDEX, roomIndex);
		}
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess;
	}

}

package nl.hyranasoftware.javagmr.util;
import com.github.underscore.Json;
import org.json.*;
import com.github.underscore.U;


import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JsonHelper {
    public static String TryParseJson(String input)
    {
        try {
            String copy = input;

            // Making sure that certain xml tags transform to arrays
            int countGame = copy.split("<PackagedGame>", -1).length - 1;
            if (countGame == 1) {
                copy = copy.replace("</PackagedGame>", "</PackagedGame><PackagedGame></PackagedGame>");
            }
            int countPlayer = copy.split("<PackagedPlayer>", -1).length - 1;
            if (countPlayer == 1) {
                copy = copy.replace("</PackagedPlayer>", "</PackagedPlayer><PackagedPlayer></PackagedPlayer>");
            }

            // Fix self-closing tag
            int indexStart, indexEnd;
            while(copy.contains("/>")){
                indexEnd = copy.indexOf("/>");
                indexStart = copy.substring(0, indexEnd).lastIndexOf("<");
                String[] keys = copy.substring(indexStart + 1, indexEnd).split(" ", -1);
                if (keys.length > 0) {
                    String tmp = copy.substring(0, indexStart) + "<" + keys[0] + "></" + keys[0] + ">";
                    copy = tmp + copy.substring(indexEnd+2);
                }
            }

            // Retrieve hashmap
            LinkedHashMap xmlMap = U.fromXmlWithoutAttributes(copy);

            // Remove comment keys
            for (Iterator<String> it = xmlMap.keySet().iterator(); it.hasNext(); ) {
                String k = it.next();
                if (k.startsWith("#")) {
                    xmlMap.remove(k);
                }
            }

            // Remove packages
            xmlMap = (LinkedHashMap)RemoveInbetweenObject(xmlMap, "PackagedGame", null);
            xmlMap = (LinkedHashMap)RemoveInbetweenObject(xmlMap, "PackagedUser", null);
            xmlMap = (LinkedHashMap)RemoveInbetweenObject(xmlMap, "PackagedPlayer", null);

            // Transform
            if (countGame == 1) {
                ((ArrayList)xmlMap.get("ArrayOfPackagedGame")).remove(countGame);
            }
            if (countPlayer == 1) {
                ((ArrayList)((LinkedHashMap)xmlMap.get("GamesAndPlayers")).get("Players")).remove(countPlayer);
            }

            // Only return the first object
            String key = (String)xmlMap.keySet().toArray()[0];
            Object first = xmlMap.get(key);
            String output = "";
            if (first instanceof LinkedHashMap) {
                output = Json.toJson((LinkedHashMap)first, Json.JsonStringBuilder.Step.COMPACT);
            }
            else if (first instanceof ArrayList) {
                output = Json.toJson((ArrayList)first, Json.JsonStringBuilder.Step.COMPACT);
            }
            else if (first instanceof String) {
                output = (String)first;
            }
            else {
                output = Json.toJson(xmlMap);
            }
            // Transform empty object to string
            output = output.replace("{}", "\"\"");

            return output;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(JsonHelper.class.getName()).log(Level.SEVERE, null, ex);
            return input; // Assume input was json
        }
    }

    private static Object RemoveInbetweenObject(Object object, String key, Object parent)
    {
        if (object instanceof LinkedHashMap) {
            // Iterate over keys
            for (Iterator<String> it = ((LinkedHashMap)object).keySet().iterator(); it.hasNext(); ) {
                String k = it.next();
                Object o = ((LinkedHashMap)object).get(k);
                if (key.equals(k)) {
                    // Found it
                    return o;
                }
                // Search inside the children
                Object o2 = RemoveInbetweenObject(o, key, object);
                if (o2 != null)
                    ((LinkedHashMap)object).replace(k, o2);
            }
        }
        else if (object instanceof ArrayList) {
            // Iterate over elements
            for (Iterator<Object> it = ((ArrayList)object).iterator(); it.hasNext(); ) {
                Object o = it.next();
                // Search inside the children
                RemoveInbetweenObject(o, key, object);
            }
        }
        if (parent == null)
            return object;
        else
            return null;
    }
}

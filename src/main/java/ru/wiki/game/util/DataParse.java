package ru.wiki.game.util;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataParse {
    public static String parseRandomArticle(String json) {
        JsonParser jParser = new JsonParser();
        JsonElement root = jParser.parse(json);

        // Navigate Root -> Query -> Random
        JsonElement jQuery = root.getAsJsonObject().get("query");
        JsonElement jRandom = jQuery.getAsJsonObject().get("random");

        // Fetch the random array in the JSON data and just get the first
        // element.
        JsonArray randomArray = jRandom.getAsJsonArray();
        JsonObject first = randomArray.get(0).getAsJsonObject();
        return first.get("title").getAsString();
    }

    public static String parseEmbeddedArticle(String data, String target) {
        String preEmbeddedName = "[[" + target + "|";
        int indexOfEmbeddedNameStart = data.indexOf(preEmbeddedName);
        indexOfEmbeddedNameStart += preEmbeddedName.length();
        data = data.substring(indexOfEmbeddedNameStart);

        if (indexOfEmbeddedNameStart < preEmbeddedName.length()) {
            return target;
        }

        int indexOfEmbeddedNameEnd = data.indexOf("]]");
        data = data.substring(0, indexOfEmbeddedNameEnd);

        return data;
    }

    public static String parseBacklinksJSON(String json, ArrayList<String> backlinks, ArrayList<String> targets) {
        JsonParser jParser = new JsonParser();
        JsonElement root = jParser.parse(json);

        JsonElement jQuery = root.getAsJsonObject().get("query");
        JsonElement jBacklinks = jQuery.getAsJsonObject().get("backlinks");
        if (jBacklinks == null) {
            return "";
        }
        JsonArray jBacklinksArray = jBacklinks.getAsJsonArray();

        for (JsonElement e : jBacklinksArray) {
            JsonObject jLinkObj = e.getAsJsonObject();

            String title = jLinkObj.get("title").getAsString();
            backlinks.add(title);
            if (Utilities.containsIgnoreCase(targets, title)) {
                return "";
            }
        }

        String blCont;
        JsonElement cont = root.getAsJsonObject().get("continue");
        if (cont != null) {
            blCont = cont.getAsJsonObject().get("blcontinue").toString();
            blCont = blCont.replaceAll("\"", "");
        } else {
            blCont = "";
        }

        return blCont;
    }

    public static void parseLinksExport(String export, ArrayList<String> links, ArrayList<String> targets) {
        int start = export.indexOf("<text xml:space=\"preserve\"");
        int end = export.lastIndexOf("</text>");
        if (start > 0 && end > 0) {
            export = export.substring(start, end);
        }

        String pattern = "\\[\\[(.*?)\\]\\]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(export);

        while (matcher.find()) {
            String match = matcher.group(0);

            int startIndex = 2;
            int endIndex = match.length() - 2;

            int split = match.indexOf('|');
            if (split >= 0) {
                endIndex = split;
            }

            match = match.substring(startIndex, endIndex);
            links.add(match);
        }
    }

    public static String parseLinksJSON(String json, ArrayList<String> links, ArrayList<String> targets) {
        JsonParser jParser = new JsonParser();
        JsonElement root = jParser.parse(json);

        JsonElement jQuery = root.getAsJsonObject().get("query");
        JsonElement jPages = jQuery.getAsJsonObject().get("pages");

        JsonObject pagesObj = jPages.getAsJsonObject();
        Set<Entry<String, JsonElement>> eSet = pagesObj.entrySet();
        if (eSet.size() != 1) {
            return null;
        }

        JsonElement entry = eSet.iterator().next().getValue();

        JsonElement jLinks = entry.getAsJsonObject().get("links");
        if (jLinks == null) {
            return "";
        }
        JsonArray jLinksArray = jLinks.getAsJsonArray();

        for (JsonElement e : jLinksArray) {
            JsonObject jLinkObj = e.getAsJsonObject();
            String title = jLinkObj.get("title").getAsString();
            links.add(title);
            if (Utilities.containsIgnoreCase(targets, title)) {
                return "";
            }
        }
        String plCont;
        JsonElement cont = root.getAsJsonObject().get("continue");
        if (cont != null) {
            plCont = cont.getAsJsonObject().get("plcontinue").toString();
            plCont = plCont.replaceAll("\"", "");
        } else {
            plCont = "";
        }
        return plCont;
    }
}

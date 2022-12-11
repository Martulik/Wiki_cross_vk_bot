package ru.wiki.game.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class URLFetch {

    public static String getData(String urlLink) throws IOException {
        URL url = new URL(urlLink);
        URLConnection connection = url.openConnection();

        InputStreamReader iSR = new InputStreamReader(connection.getInputStream());
        BufferedReader bR = new BufferedReader(iSR);

        return bR.lines().collect(Collectors.joining("\n"));
    }

    public static String getExportURL(String name) {
        return URLFetch.appendURL(name);
    }

    public static String getLinksURL(String name, String cont) {
        final String PRE_TITLE_URL = "https://en.wikipedia.org/w/api.php?" +
                "action=query&format=json&prop=links&pllimit=max" +
                "&plnamespace=0&titles=";
        String continueToken = "&plcontinue=" + cont;
        return URLFetch.appendURL(PRE_TITLE_URL, name, continueToken);
    }

    public static String getBacklinksURL(String name, String cont) {
        final String PRE_TITLE_URL = "https://en.wikipedia.org/w/api.php?" +
                "action=query&format=json&list=backlinks&bllimit=max" +
                "&blnamespace=0&blfilterredir=nonredirects&bltitle=";
        String continueToken = "&blcontinue=" + cont;
        return URLFetch.appendURL(PRE_TITLE_URL, name, continueToken);
    }

    public static String getRandomURL() {
        return "https://en.wikipedia.org/w/api.php?action=query&list=random" +
                "&rnlimit=1&rnnamespace=0&format=json";
    }

    private static String appendURL(String name) {
        name = name.replaceAll(" ", "_");
        StringBuilder sB = new StringBuilder("https://en.wikipedia.org/wiki/Special:Export/");
        sB.append(name);
        return sB.toString();
    }

    public static String getUrlFromTitle(String title) {
        title = title.replaceAll(" ", "_");
        StringBuilder sB = new StringBuilder("https://en.wikipedia.org/wiki/");
        sB.append(title);
        return sB.toString();
    }
    private static String appendURL(String preTitle, String name, String cont) {
        name = name.replaceAll(" ", "_");
        StringBuilder sB = new StringBuilder(preTitle);
        sB.append(name);
        sB.append(cont);
        return sB.toString();
    }
}

package ru.wiki.game.links;

import ru.wiki.game.util.DataParse;
import ru.wiki.game.util.URLFetch;

import java.io.IOException;
import java.util.ArrayList;

public class JSONLinksFetcher implements AbstractLinkFetcher {
    @Override
    public ArrayList<String> getLinks(String article, ArrayList<String> targets) throws IOException {
        ArrayList<String> allLinks = new ArrayList<>();

        String continueToken = "0|0|0";
        while (!continueToken.isEmpty()) {
            String url = URLFetch.getLinksURL(article, continueToken);
            String json = URLFetch.getData(url);

            continueToken = DataParse.parseLinksJSON(json, allLinks, targets);
        }

        return allLinks;
    }
}
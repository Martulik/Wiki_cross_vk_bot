package ru.wiki.game.links;

import ru.wiki.game.util.DataParse;
import ru.wiki.game.util.URLFetch;

import java.io.IOException;
import java.util.ArrayList;

public class BacklinksFetcher implements AbstractLinkFetcher {
    @Override
    public ArrayList<String> getLinks(String article, ArrayList<String> targets) throws IOException {
        ArrayList<String> allBacklinks = new ArrayList<>();

        String continueToken = "0|0";
        while (!continueToken.isEmpty()) {
            String url = URLFetch.getBacklinksURL(article, continueToken);
            String json = URLFetch.getData(url);
            continueToken = DataParse.parseBacklinksJSON(json, allBacklinks, targets);
        }

        return allBacklinks;
    }
}

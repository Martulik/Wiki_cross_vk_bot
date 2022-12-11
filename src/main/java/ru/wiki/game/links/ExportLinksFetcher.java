package ru.wiki.game.links;

import ru.wiki.game.util.DataParse;
import ru.wiki.game.util.URLFetch;

import java.io.IOException;
import java.util.ArrayList;

public class ExportLinksFetcher implements AbstractLinkFetcher {
    @Override
    public ArrayList<String> getLinks(String article, ArrayList<String> targets) throws IOException {
        ArrayList<String> allLinks = new ArrayList<>();

        String url = URLFetch.getExportURL(article);
        String export = URLFetch.getData(url);

        DataParse.parseLinksExport(export, allLinks, targets);

        return allLinks;
    }
}

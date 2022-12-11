package ru.wiki.game.links;

import java.io.IOException;
import java.util.ArrayList;

public interface AbstractLinkFetcher {
    ArrayList<String> getLinks(String article, ArrayList<String> targets) throws IOException;
}
package ru.wiki.game.util;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utilities {

	public static boolean containsIgnoreCase(List<String> list, String str) {
		for (String element : list) {
			if (str.equalsIgnoreCase(element)) {
				return true;
			}
		}
		return false;
	}
	
	public static String getRandomArticle() throws IOException {
		String url = URLFetch.getRandomURL();
		String json = URLFetch.getData(url);
		return DataParse.parseRandomArticle(json);
	}

	public static List<String> retainAllIgnoreCase(List<String> a, List<String> b) {
		Stream<String> stream = a.stream().filter(s -> containsIgnoreCase(b, s));
		return stream.collect(Collectors.toList());
	}
}

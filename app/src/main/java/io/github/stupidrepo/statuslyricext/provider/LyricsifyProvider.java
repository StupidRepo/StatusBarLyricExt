package io.github.stupidrepo.statuslyricext.provider;

import android.media.MediaMetadata;
import android.util.Log;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Locale;

import io.github.stupidrepo.statuslyricext.provider.utils.HttpRequestUtil;
import io.github.stupidrepo.statuslyricext.provider.utils.LyricSearchUtil;

public class LyricsifyProvider implements ILrcProvider {

    private static final String LYRICSIFY_BASE_URL = "https://www.lyricsify.com/";
    private static final String LYRICSIFY_SEARCH_URL_FORMAT = LYRICSIFY_BASE_URL + "search?q=%s";

    @Override
    public LyricResult getLyric(MediaMetadata data) throws IOException {
        String searchUrl = String.format(Locale.getDefault(), LYRICSIFY_SEARCH_URL_FORMAT, LyricSearchUtil.getSearchKey(data));
        String searchResult = HttpRequestUtil.getTextResponse(searchUrl);
        if (searchResult != null) {
            Document doc = Jsoup.parse(searchResult);
            Elements resultElements = doc.select(".li");
            Pair<String, Long> pair = getBestMatchedLyricUrl(resultElements, data);
            if (pair != null) {
                Log.d("STATE", pair.first);
                String lyricUrl = pair.first;
                String lyricHtml = HttpRequestUtil.getTextResponse(lyricUrl);
                if (lyricHtml != null) {
                    Document lyricDoc = Jsoup.parse(lyricHtml);
                    String lyricsId = lyricUrl.substring(lyricUrl.lastIndexOf('.') + 1);
                    Element lyricsElement = lyricDoc.getElementById("lyrics_" + lyricsId + "_details");
                    if (lyricsElement != null) {
                        LyricResult result = new LyricResult();
                        result.mLyric = Jsoup.parse(lyricsElement.html()).text();
                        result.mDistance = pair.second;
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private static Pair<String, Long> getBestMatchedLyricUrl(Elements resultElements, MediaMetadata mediaMetadata) {
        String bestMatchUrl = "";
        long minDistance = Long.MAX_VALUE;
        for (Element resultElement : resultElements) {
            String title = resultElement.select(".title").text().toLowerCase();
            String link = resultElement.select(".title").attr("href");
            String[] parts = title.split("-");
            if (parts.length >= 2) {
                String artist = parts[0].trim();
                String name = title.substring(artist.length() + 1).trim();
                long distance = LyricSearchUtil.getMetadataDistance(mediaMetadata, name, artist, null);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestMatchUrl = LYRICSIFY_BASE_URL + link;
                }
            }
        }
        return minDistance < Long.MAX_VALUE ? new Pair<>(bestMatchUrl, minDistance) : null;
    }
}
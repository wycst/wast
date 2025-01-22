package com.wast.wiki.json;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

public class NdjsonExample {

    public static void main(String[] args) throws FileNotFoundException {
        String json = "{\"images\":[{\"height\":768,\"size\":\"LARGE\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_large.jpg\",\"width\":1024},{\"height\":240,\"size\":\"SMALL\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_small.jpg\",\"width\":320}],\"media\":{\"bitrate\":262144,\"duration\":18000000,\"format\":\"video/mpg4\",\"height\":480,\"persons\":[\"Bill Gates\",\"Steve Jobs\"],\"player\":\"JAVA\",\"size\":58982400,\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote.mpg\",\"width\":640}}" +
                "{\"images\":[{\"height\":768,\"size\":\"LARGE\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_large.jpg\",\"width\":1024},{\"height\":240,\"size\":\"SMALL\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_small.jpg\",\"width\":320}],\"media\":{\"bitrate\":262144,\"duration\":18000000,\"format\":\"video/mpg4\",\"height\":480,\"persons\":[\"Bill Gates\",\"Steve Jobs\"],\"player\":\"JAVA\",\"size\":58982400,\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote.mpg\",\"width\":640}}" +
                "{\"images\":[{\"height\":768,\"size\":\"LARGE\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_large.jpg\",\"width\":1024},{\"height\":240,\"size\":\"SMALL\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_small.jpg\",\"width\":320}],\"media\":{\"bitrate\":262144,\"duration\":18000000,\"format\":\"video/mpg4\",\"height\":480,\"persons\":[\"Bill Gates\",\"Steve Jobs\"],\"player\":\"JAVA\",\"size\":58982400,\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote.mpg\",\"width\":640}}" +
                "{\"images\":[{\"height\":768,\"size\":\"LARGE\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_large.jpg\",\"width\":1024},{\"height\":240,\"size\":\"SMALL\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_small.jpg\",\"width\":320}],\"media\":{\"bitrate\":262144,\"duration\":18000000,\"format\":\"video/mpg4\",\"height\":480,\"persons\":[\"Bill Gates\",\"Steve Jobs\"],\"player\":\"JAVA\",\"size\":58982400,\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote.mpg\",\"width\":640}}" +
                "{\"images\":[{\"height\":768,\"size\":\"LARGE\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_large.jpg\",\"width\":1024},{\"height\":240,\"size\":\"SMALL\",\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote_small.jpg\",\"width\":320}],\"media\":{\"bitrate\":262144,\"duration\":18000000,\"format\":\"video/mpg4\",\"height\":480,\"persons\":[\"Bill Gates\",\"Steve Jobs\"],\"player\":\"JAVA\",\"size\":58982400,\"title\":\"Javaone Keynote\",\"uri\":\"http://javaone.com/keynote.mpg\",\"width\":640}}" +
                "";
        List results = JSON.parseNdJson(json);
        results.add(123);
        results.add(456);
        System.out.println(JSON.toNdJsonString(results, WriteOption.FormatOut));
        System.out.println(results);
        JSON.writeNdJsonTo(results, new FileOutputStream("e:/tmp/ndjson.ndjson"), WriteOption.FormatOut);

        List list = JSON.parseNdJson(new FileInputStream("e:/tmp/ndjson.ndjson"));
        System.out.println(list);
    }

}

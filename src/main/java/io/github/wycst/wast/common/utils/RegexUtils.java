package io.github.wycst.wast.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 * @Author: wangy
 * @Date: 2020/3/11 21:33
 * @Description:
 */
public class RegexUtils {

    private static Map<String, Pattern> cachePatterns = new ConcurrentHashMap<String, Pattern>();

    public static Pattern getPattern(String regSource) {
        Pattern pattern = cachePatterns.get(regSource);
        if(pattern != null) {
            return pattern;
        }
        synchronized (regSource) {
            pattern = cachePatterns.get(regSource);
            if(pattern == null) {
                pattern = Pattern.compile(regSource);
                cachePatterns.put(regSource, pattern);
            }
            return pattern;
        }
    }

    public static List<String> getMatcherGroups(String source, String groupRegex) {
        return getMatcherGroups(source, groupRegex, true);
    }

    public static List<String> getMatcherGroups(String source, String groupRegex, boolean iterator) {
        List<String> matcherGroups = new ArrayList<String>();
        int begin = groupRegex.indexOf('(');
        if (begin == -1 || groupRegex.indexOf(')') <= begin) {
            return matcherGroups;
        }
        Pattern pattern = getPattern(groupRegex);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int groupCount = matcher.groupCount();
            if (iterator) {
                int i = 0;
                while (i++ < groupCount) {
                    matcherGroups.add(matcher.group(i));
                }
            } else {
                if (groupCount > 0) {
                    matcherGroups.add(matcher.group(1));
                }
            }
        }
        return matcherGroups;
    }


}

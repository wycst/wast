package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.util.Map;

/**
 * <p> A Fast Finder Based on Prefix Matching </p>
 *
 * @Date 2024/4/14 8:59
 * @Created by wangyc
 */
class JSONValueMatcher<T> {

    protected final FixedNameValueMap<T> valueMapForChars;
    protected final FixedNameValueMap<T> valueMapForBytes;

    JSONValueMatcher(FixedNameValueMap valueMapForChars, FixedNameValueMap valueMapForBytes) {
        this.valueMapForChars = valueMapForChars;
        this.valueMapForBytes = valueMapForBytes;
    }

    public T matchValue(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
        int i = offset;
        T result = null;
        char ch, ch1;
        if ((ch = buf[i]) != endToken) {
            long hashValue = ch;
            if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                hashValue = valueMapForChars.hash(hashValue, ch, ch1);
                if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                    hashValue = valueMapForChars.hash(hashValue, ch, ch1);
                    while ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                        hashValue = valueMapForChars.hash(hashValue, ch, ch1);
                    }
                }
            }
            if (ch != endToken) {
                hashValue = valueMapForChars.hash(hashValue, ch);
            }
            result = valueMapForChars.getValue(buf, offset, i, hashValue);
        }
        parseContext.endIndex = i;
        return result;
    }

    public T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
        int i = offset;
        byte b;
        T result = null;
        if ((b = buf[i]) != endToken) {
            long hashValue = b;
            byte b1;
            if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                hashValue = valueMapForBytes.hash(hashValue, b, b1);
                if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                    hashValue = valueMapForBytes.hash(hashValue, b, b1);
                    while ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                        hashValue = valueMapForBytes.hash(hashValue, b, b1);
                    }
                }
            }
            if (b != endToken) {
                hashValue = valueMapForBytes.hash(hashValue, b);
            }
            result = valueMapForBytes.getValue(buf, offset, i, hashValue);
        }
        parseContext.endIndex = i;
        return result;
    }

    public final long hash(long r, int v) {
        return valueMapForChars.hash(r, v);
    }

    public final long hash(long r, int v1, int v2) {
        return valueMapForChars.hash(r, v1, v2);
    }

    protected T getValue(char[] buf, int beginIndex, int endIndex, long hashValue) {
        return valueMapForChars.getValue(buf, beginIndex, endIndex, hashValue);
    }

    protected T getValue(byte[] buf, int beginIndex, int endIndex, long hashValue) {
        return valueMapForBytes.getValue(buf, beginIndex, endIndex, hashValue);
    }

    public final T getValue(String fieldName) {
        return valueMapForChars.getValue(fieldName);
    }

    /**
     * plus hash inline optimization
     *
     * @param <T>
     */
    static class JSONPlusHashValueQuickMatcher<T> extends JSONValueMatcher<T> {
        public <T> JSONPlusHashValueQuickMatcher(FixedNameValueMap<T> valueMapForChars) {
            super(valueMapForChars, valueMapForChars);
        }

        public final T matchValue(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            T result = null;
            char ch;
            if ((ch = buf[i]) != endToken) {
                long hashValue = ch;
                char ch1;
                if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                    hashValue += ch + ch1;
                    if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                        hashValue += ch + ch1;
                        while ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                            hashValue += ch + ch1;
                        }
                    }
                }
                if (ch != endToken) {
                    hashValue += ch;
                }
                result = parseContext.strictMode ? valueMapForChars.getValue(buf, offset, i, hashValue) : valueMapForChars.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }

        public final T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            byte b;
            T result = null;
            if ((b = buf[i]) != endToken) {
                long hashValue = b;
                byte b1;
                if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                    hashValue += b + b1;
                    if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                        hashValue += b + b1;
                        while ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                            hashValue += b + b1;
                        }
                    }
                }
                if (b != endToken) {
                    hashValue += b;
                }
                result = parseContext.strictMode ? valueMapForBytes.getValue(buf, offset, i, hashValue) : valueMapForBytes.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }
    }

    /**
     * bit hash inline optimization
     *
     * @param <T>
     */
    static class JSONBitHashValueQuickMatcher<T> extends JSONValueMatcher<T> {

        private final int bits;
        private final int bitsTwice;

        public <T> JSONBitHashValueQuickMatcher(FixedNameValueMap<T> valueMap, int bits) {
            super(valueMap, valueMap);
            this.bits = bits;
            this.bitsTwice = bits << 1;
        }

        public final T matchValue(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            T result = null;
            char ch;
            if ((ch = buf[i]) != endToken) {
                long hashValue = ch;
                char ch1;
                if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                    hashValue = (hashValue << bitsTwice) + (ch << bits) + ch1;
                    if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                        hashValue = (hashValue << bitsTwice) + (ch << bits) + ch1;
                        while ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                            hashValue = (hashValue << bitsTwice) + (ch << bits) + ch1;
                        }
                    }
                }
                if (ch != endToken) {
                    hashValue = (hashValue << bits) + ch;
                }
                result = parseContext.strictMode ? valueMapForChars.getValue(buf, offset, i, hashValue) : valueMapForChars.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }

        public final T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            byte b;
            T result = null;
            if ((b = buf[i]) != endToken) {
                long hashValue = b;
                byte b1;
                if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                    hashValue = (hashValue << bitsTwice) + (b << bits) + b1;
                    if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                        hashValue = (hashValue << bitsTwice) + (b << bits) + b1;
                        while ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                            hashValue = (hashValue << bitsTwice) + (b << bits) + b1;
                        }
                    }
                }
                if (b != endToken) {
                    hashValue = (hashValue << bits) + b;
                }
                result = parseContext.strictMode ? valueMapForBytes.getValue(buf, offset, i, hashValue) : valueMapForBytes.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }
    }

    /**
     * prime hash inline optimization
     *
     * @param <T>
     */
    static class JSONPrimeHashValueQuickMatcher<T> extends JSONValueMatcher<T> {

        private final int primeValue;
        private final int primeSquare;

        public <T> JSONPrimeHashValueQuickMatcher(FixedNameValueMap<T> valueMap, int primeValue) {
            super(valueMap, valueMap);
            this.primeValue = primeValue;
            this.primeSquare = primeValue * primeValue;
        }

        public final T matchValue(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            T result = null;
            char ch;
            if ((ch = buf[i]) != endToken) {
                long hashValue = ch;
                char ch1;
                if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                    hashValue = hashValue * primeSquare + ch * primeValue + ch1;
                    if ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                        hashValue = hashValue * primeSquare + ch * primeValue + ch1;
                        while ((ch = buf[++i]) != endToken && (ch1 = buf[++i]) != endToken) {
                            hashValue = hashValue * primeSquare + ch * primeValue + ch1;
                        }
                    }
                }
                if (ch != endToken) {
                    hashValue = hashValue * primeValue + ch;
                }
                result = parseContext.strictMode ? valueMapForChars.getValue(buf, offset, i, hashValue) : valueMapForChars.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }

        public final T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
            int i = offset;
            byte b;
            T result = null;
            if ((b = buf[i]) != endToken) {
                long hashValue = b;
                byte b1;
                if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                    hashValue = hashValue * primeSquare + b * primeValue + b1;
                    if ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                        hashValue = hashValue * primeSquare + b * primeValue + b1;
                        while ((b = buf[++i]) != endToken && (b1 = buf[++i]) != endToken) {
                            hashValue = hashValue * primeSquare + b * primeValue + b1;
                        }
                    }
                }
                if (b != endToken) {
                    hashValue = hashValue * primeValue + b;
                }
                result = parseContext.strictMode ? valueMapForBytes.getValue(buf, offset, i, hashValue) : valueMapForBytes.getValueByHash(hashValue);
            }
            parseContext.endIndex = i;
            return result;
        }
    }

//    static class JSONValueHashQuickMatcher<T> extends JSONValueQuickMatcher<T> {
//        JSONValueHashQuickMatcher(FixedNameValueMap valueMapForChars, FixedNameValueMap valueMapForBytes) {
//            super(valueMapForChars, valueMapForBytes);
//        }
//
//        @Override
//        protected final T getValue(char[] buf, int beginIndex, int endIndex, long hashValue) {
//            return valueMapForChars.getValueByHash(hashValue);
//        }
//
//        @Override
//        protected final T getValue(byte[] buf, int beginIndex, int endIndex, long hashValue) {
//            return valueMapForChars.getValueByHash(hashValue);
//        }
//
//        public final T matchValue(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
//            T result = null;
//            char ch;
//            if ((ch = buf[offset]) != endToken) {
//                long hashValue = calHashValue(ch, buf, offset, endToken, parseContext);
//                result = valueMapForChars.getValueByHash(hashValue);
//            } else {
//                parseContext.endIndex = offset;
//            }
//            return result;
//        }
//    }
//
//    static class JSONOneNodeEqualMatcher<T> extends JSONValueMatcher<T> {
//        final T value;
//        // keyLength <= 8
//        final int keyLength;
//        final long keyCharValue;
//        final long keyByteValue;
//
//        JSONOneNodeEqualMatcher(FixedNameValueMap valueMap, int keyLength, long keyCharValue, long keyByteValue, T value) {
//            super(valueMap, valueMap);
//            this.keyLength = keyLength;
//            this.keyCharValue = keyCharValue;
//            this.keyByteValue = keyByteValue;
//            this.value = value;
//        }
//
//        public T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
//            if (UnsafeHelper.getUnsafeLong(buf, offset, keyLength) == keyByteValue) {
//                int endIndex = offset + keyLength;
//                if (buf[endIndex] == endToken) {
//                    parseContext.endIndex = endIndex;
//                    return value;
//                }
//                while (buf[++endIndex] != endToken) ;
//                parseContext.endIndex = endIndex;
//                return null;
//            } else {
//                if (buf[offset] != endToken && buf[++offset] != endToken) {
//                    while (buf[++offset] != endToken && buf[++offset] != endToken) {
//                    }
//                }
//                parseContext.endIndex = offset;
//                return null;
//            }
//        }
//    }
//
//    static class MatcherNode<T> {
//        public final byte[] bytes;
//        public final char[] chars;
//        public final T value;
//        public final long hashValue;
//
//        public MatcherNode(byte[] bytes, char[] chars, T value, long hashValue) {
//            this.bytes = bytes;
//            this.chars = chars;
//            this.value = value;
//            this.hashValue = hashValue;
//        }
//    }

//    // use suffix
//    static class JSONSuffixQuickMatcher<T> extends JSONValueMatcher<T> {
//        final int mask;
//        final MatcherNode[] matcherNodes;
//
//        JSONSuffixQuickMatcher(FixedNameValueMap valueMapForChars, FixedNameValueMap valueMapForBytes, int mask, MatcherNode[] matcherNodes) {
//            super(valueMapForChars, valueMapForBytes);
//            this.mask = mask;
//            this.matcherNodes = matcherNodes;
//        }
//
//        @Override
//        public T matchValue(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
//            int begin = offset;
//            if (buf[offset] != endToken) {
//                // Try to avoid entering the loop as much as possible
//                int endIndex, len;
//                if (buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken) {
//                    if (buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken) {
//                        while (buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken && buf[++offset] != endToken) {
//                        }
//                    }
//                    // len >= 8
//                    endIndex = offset;
//                    len = endIndex - begin;
//                    parseContext.endIndex = endIndex;
//                    long hash = UnsafeHelper.getLong(buf, offset - 8);
//                    int nodeIndex = (int) (hash & mask);
//                    MatcherNode<T> matcherNode = matcherNodes[nodeIndex];
//                    if (matcherNode != null && hash == matcherNode.hashValue && len == matcherNode.bytes.length && MemoryOptimizerUtils.equals(buf, begin, matcherNode.bytes, 0, len - 8)) {
//                        parseContext.endIndex = endIndex;
//                        return matcherNode.value;
//                    }
//                } else {
//                    // len < 8
//                    endIndex = offset;
//                    parseContext.endIndex = endIndex;
//                    len = endIndex - begin;
//                    long hash = UnsafeHelper.getUnsafeLong(buf, begin, len);
//                    int nodeIndex = (int) (hash & mask);
//                    MatcherNode<T> matcherNode = matcherNodes[nodeIndex];
//                    if (matcherNode != null && hash == matcherNode.hashValue) {
//                        return matcherNode.value;
//                    }
//                }
//            } else {
//                parseContext.endIndex = begin;
//            }
//            return null;
//        }
//    }

    static <T> JSONValueMatcher<T> build(FixedNameValueMap valueMapForChars, FixedNameValueMap valueMapForBytes) {
        return new JSONValueMatcher(valueMapForChars, valueMapForBytes);
    }

    public static <T> JSONValueMatcher<T> build(Map<String, T> originalMap) {
        int valueSize = originalMap.size();
        String[] names = originalMap.keySet().toArray(new String[valueSize]);
        FixedNameValueMap<T> valueMapForChars = FixedNameValueMap.build(originalMap);
        boolean isAsciiKeys = checkAsciiKeys(names);
        if (isAsciiKeys) {
            // Single key model
//            if (valueSize == 1 && names[0].length() <= 8) {
//                String key = names[0];
//                long keyByteValue = UnsafeHelper.getUnsafeLong(key.getBytes(), 0, key.length());
//                long keyCharValue = 0;
//                for (int i = 0; i < key.length(); ++i) {
//                    char ch = key.charAt(i);
//                    keyCharValue = keyCharValue << 8 | ch;
//                }
//                return new JSONOneNodeEqualMatcher(valueMapForChars, key.length(), keyCharValue, keyByteValue, originalMap.get(key));
//            }

            // inline optimization
            if (valueMapForChars.isPlusHash()) {
                return new JSONPlusHashValueQuickMatcher(valueMapForChars);
            }
            if (valueMapForChars.isBitHash()) {
                return new JSONBitHashValueQuickMatcher(valueMapForChars, valueMapForChars.getBits());
            }
            if (!valueMapForChars.isCollision()) {
                return new JSONPrimeHashValueQuickMatcher(valueMapForChars, valueMapForChars.getPrimeValue());
            }
        }

        FixedNameValueMap<T> valueMapForBytes = isAsciiKeys ? valueMapForChars : FixedNameValueMap.build(originalMap, true);


//        Set<Map.Entry<String, T>> entries = originalMap.entrySet();
//        if (valueSize == 1) {
//            // 单key模型
//            Iterator<Map.Entry<String, T>> entryIterator = entries.iterator();
//            entryIterator.hasNext();
//            Map.Entry<String, T> entry = entryIterator.next();
//            String key = entry.getKey();
//            T value = entry.getValue();
//            byte[] bytes = key.getBytes();
//            int keyLength = bytes.length;
//            if (keyLength <= 8) {
//                return new JSONOneNodeEqualMatcher(defaultValueMap, keyLength, UnsafeHelper.getUnsafeLong(bytes, 0, keyLength), value);
//            }
//        }
//
//        int minByteLength = Integer.MAX_VALUE, minCharLength = Integer.MAX_VALUE;
//        byte[][] keyBytesArray = new byte[valueSize][];
//        char[][] keyCharsArray = new char[valueSize][];
//        int i = 0;
//        long[] suffixForBytes = new long[valueSize];
//        Set<Object> valueSet = new HashSet<Object>();
//        Object[] values = new Object[valueSize];
//        boolean asciiFlag = true;
//        for (Map.Entry<String, T> entry : entries) {
//            String key = entry.getKey();
//            T value = entry.getValue();
//            byte[] keyBytes = key.getBytes();
//            char[] keyChars = key.toCharArray();
//            if (keyBytes.length != keyChars.length) {
//                asciiFlag = false;
//            }
//            keyBytesArray[i] = keyBytes;
//            keyCharsArray[i] = keyChars;
//            values[i] = value;
//            int keyLength = keyBytes.length;
//            if(keyLength >= 8) {
//                suffixForBytes[i] = UnsafeHelper.getUnsafeLong(keyBytes, keyLength - 8, 8);
//            } else {
//                suffixForBytes[i] = UnsafeHelper.getUnsafeLong(keyBytes, 0, keyLength);
//            }
//            minByteLength = Math.min(minByteLength, keyBytes.length);
//            minCharLength = Math.min(minCharLength, keyChars.length);
//            ++i;
//        }
//        if (isAllDifferent(suffixForBytes, valueSet)) {
//            int mask = tryFindMask(suffixForBytes, valueSize);
//            if (mask != -1) {
//                MatcherNode[] matcherNodes = new MatcherNode[mask + 1];
//                for (int j = 0; j < valueSize; ++j) {
//                    long hv = suffixForBytes[j];
//                    int index = (int) (hv & mask);
//                    matcherNodes[index] = new MatcherNode(keyBytesArray[j], keyCharsArray[j], values[j], hv);
//                }
//                return new JSONSuffixQuickMatcher(defaultValueMap, mask, matcherNodes);
//            }
//        }

        // back default matcher model
        return build(valueMapForChars, valueMapForBytes);
    }

    private static boolean checkAsciiKeys(String[] keys) {
        for (String key : keys) {
            int keyLength = key.length();
            for (int i = 0; i < keyLength; ++i) {
                if (key.charAt(i) >= 0x80) return false;
            }
        }
        return true;
    }
//
//    private static boolean isAllDifferent(long[] values, Set<Object> valueSet) {
//        valueSet.clear();
//        for (long val : values) {
//            if (!valueSet.add(val)) {
//                return false;
//            }
//        }
//        return true;
//    }

//    private static int tryFindMask(long[] values, int size) {
//        int mask = (1 << (32 - Integer.numberOfLeadingZeros(size))) - 1;
//        int maxMask = Math.max(size << 4, 511);
//        Set<Integer> rems = new HashSet<Integer>();
//        while (mask <= maxMask) {
//            rems.clear();
//            for (long val : values) {
//                rems.add((int) (val & mask));
//            }
//            if (rems.size() == size) {
//                return mask;
//            }
//            mask = mask << 1 | 1;
//        }
//        return -1;
//    }
}

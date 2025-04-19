package io.github.wycst.wast.json;

import java.util.Map;

/**
 * <p> A Fast Finder </p>
 *
 * @Date 2024/4/14 8:59
 * @Created by wangyc
 */
class JSONValueMatcher<T> {

    protected final JSONKeyValueMap<T> valueMapForChars;
    protected final JSONKeyValueMap<T> valueMapForBytes;

    JSONValueMatcher(JSONKeyValueMap valueMapForChars, JSONKeyValueMap valueMapForBytes) {
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

    final static class PlhvImpl<T> extends JSONValueMatcher<T> {
        public <T> PlhvImpl(JSONKeyValueMap<T> valueMapForChars) {
            super(valueMapForChars, valueMapForChars);
        }

        @Override
        boolean isPlhv() {
            return true;
        }

        boolean isSupportedOptimize() {
            return true;
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

    final static class BihvImpl<T> extends JSONValueMatcher<T> {

        private final int bits;
        private final int bitsTwice;

        boolean isSupportedOptimize() {
            return true;
        }

        public <T> BihvImpl(JSONKeyValueMap<T> valueMap, int bits) {
            super(valueMap, valueMap);
            this.bits = bits;
            this.bitsTwice = bits << 1;
        }

        boolean isBihv() {
            return true;
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

    final static class PrhvImpl<T> extends JSONValueMatcher<T> {

        private final int primeValue;
        private final int primeSquare;

        public <T> PrhvImpl(JSONKeyValueMap<T> valueMap, int primeValue) {
            super(valueMap, valueMap);
            this.primeValue = primeValue;
            this.primeSquare = primeValue * primeValue;
        }

        boolean isSupportedOptimize() {
            return true;
        }

        boolean isPrhv() {
            return true;
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

    static <T> JSONValueMatcher<T> build(JSONKeyValueMap valueMapForChars, JSONKeyValueMap valueMapForBytes) {
        return new JSONValueMatcher(valueMapForChars, valueMapForBytes);
    }

    public static <T> JSONValueMatcher<T> build(Map<String, T> originalMap) {
        int valueSize = originalMap.size();
        String[] names = originalMap.keySet().toArray(new String[valueSize]);
        JSONKeyValueMap<T> valueMapForChars = JSONKeyValueMap.build(originalMap);
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
                return new PlhvImpl(valueMapForChars);
            }
            if (valueMapForChars.isBitHash()) {
                return new BihvImpl(valueMapForChars, valueMapForChars.getBits());
            }
            if (!valueMapForChars.isCollision()) {
                return new PrhvImpl(valueMapForChars, valueMapForChars.getPrimeValue());
            }
        }

        JSONKeyValueMap<T> valueMapForBytes = isAsciiKeys ? valueMapForChars : JSONKeyValueMap.build(originalMap, true);

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

    boolean isPlhv() {
        return false;
    }

    boolean isPrhv() {
        return false;
    }

    boolean isBihv() {
        return false;
    }

    boolean isSupportedOptimize() {
        return false;
    }
}

package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.NumberUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.UUID;

/**
 * @Date 2024/3/21 16:32
 * @Created by wangyc
 */
class JSONWrapWriter extends JSONWriter {

    final Writer writer;

    JSONWrapWriter(Writer writer) {
        this.writer = writer;
    }

    public static JSONWrapWriter wrap(Writer writer) {
        return new JSONWrapWriter(writer);
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    @Override
    StringBuffer toStringBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    StringBuilder toStringBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void toOutputStream(OutputStream os) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    void writeShortChars(char[] chars, int offset, int len) throws IOException {
        write(chars, offset, len);
    }

    @Override
    public void writeLong(long numValue) throws IOException {
        if (numValue == 0) {
            writer.write('0');
            return;
        }
        if (numValue < 0) {
            if (numValue == Long.MIN_VALUE) {
                writer.write("-9223372036854775808");
                return;
            }
            numValue = -numValue;
            writer.write('-');
        }
        NumberUtils.writePositiveLong(numValue, writer);
    }

    @Override
    void writeUUID(UUID uuid) throws IOException {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        writer.write('"');
        char[] chars = JSONGeneral.CACHED_CHARS_24.get();
        NumberUtils.writeUUIDMostSignificantBits(mostSigBits, chars, 0);
        writer.write(chars, 0, 18);

        NumberUtils.writeUUIDLeastSignificantBits(leastSigBits, chars, 0);
        writer.write(chars, 0, 18);
        writer.write('"');
    }

    @Override
    public void writeDouble(double numValue) throws IOException {
        char[] chars = JSONGeneral.CACHED_CHARS_24.get();
        int len = NumberUtils.writeDouble(numValue, chars, 0);
        writer.write(chars, 0, len);
    }

    @Override
    public void writeFloat(float numValue) throws IOException {
        char[] chars = JSONGeneral.CACHED_CHARS_24.get();
        int len = NumberUtils.writeFloat(numValue, chars, 0);
        writer.write(chars, 0, len);
    }

    @Override
    public void writeLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano) throws IOException {
        int y1 = year / 100, y2 = year - y1 * 100;
        writer.write(JSONGeneral.DigitTens[y1]);
        writer.write(JSONGeneral.DigitOnes[y1]);
        writer.write(JSONGeneral.DigitTens[y2]);
        writer.write(JSONGeneral.DigitOnes[y2]);
        writer.write('-');
        writer.write(JSONGeneral.DigitTens[month]);
        writer.write(JSONGeneral.DigitOnes[month]);
        writer.write('-');
        writer.write(JSONGeneral.DigitTens[day]);
        writer.write(JSONGeneral.DigitOnes[day]);
        writer.write('T');
        writer.write(JSONGeneral.DigitTens[hour]);
        writer.write(JSONGeneral.DigitOnes[hour]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[minute]);
        writer.write(JSONGeneral.DigitOnes[minute]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[second]);
        writer.write(JSONGeneral.DigitOnes[second]);
        writeNano(nano, writer);
    }

    @Override
    public void writeLocalDate(int year, int month, int day) throws IOException {
        int y1 = year / 100, y2 = year - y1 * 100;
        char[] chars = JSONGeneral.CACHED_CHARS_DATE_19.get();
        chars[1] = JSONGeneral.DigitTens[y1];
        chars[2] = JSONGeneral.DigitOnes[y1];
        chars[3] = JSONGeneral.DigitTens[y2];
        chars[4] = JSONGeneral.DigitOnes[y2];

        chars[6] = JSONGeneral.DigitTens[month];
        chars[7] = JSONGeneral.DigitOnes[month];

        chars[9] = JSONGeneral.DigitTens[day];
        chars[10] = JSONGeneral.DigitOnes[day];
        // yyyy-MM-dd
        writer.write(chars, 0, 10);
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) throws IOException {
        writer.write(JSONGeneral.DigitTens[hourOfDay]);
        writer.write(JSONGeneral.DigitOnes[hourOfDay]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[minute]);
        writer.write(JSONGeneral.DigitOnes[minute]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[second]);
        writer.write(JSONGeneral.DigitOnes[second]);
    }

    @Override
    public void writeTimeWithNano(int hourOfDay, int minute, int second, int nano) throws IOException {
        writer.write(JSONGeneral.DigitTens[hourOfDay]);
        writer.write(JSONGeneral.DigitOnes[hourOfDay]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[minute]);
        writer.write(JSONGeneral.DigitOnes[minute]);
        writer.write(':');
        writer.write(JSONGeneral.DigitTens[second]);
        writer.write(JSONGeneral.DigitOnes[second]);
        writeNano(nano, writer);
    }

    @Override
    public void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) throws IOException {
        int y1 = year / 100, y2 = year - y1 * 100;
        char[] chars = JSONGeneral.CACHED_CHARS_DATE_19.get();
        chars[1] = JSONGeneral.DigitTens[y1];
        chars[2] = JSONGeneral.DigitOnes[y1];
        chars[3] = JSONGeneral.DigitTens[y2];
        chars[4] = JSONGeneral.DigitOnes[y2];

        chars[6] = JSONGeneral.DigitTens[month];
        chars[7] = JSONGeneral.DigitOnes[month];

        chars[9] = JSONGeneral.DigitTens[day];
        chars[10] = JSONGeneral.DigitOnes[day];

        chars[12] = JSONGeneral.DigitTens[hourOfDay];
        chars[13] = JSONGeneral.DigitOnes[hourOfDay];

        chars[15] = JSONGeneral.DigitTens[minute];
        chars[16] = JSONGeneral.DigitOnes[minute];

        chars[18] = JSONGeneral.DigitTens[second];
        chars[19] = JSONGeneral.DigitOnes[second];
        writer.write(chars);
    }

    @Override
    public void writeBigInteger(BigInteger bigInteger) throws IOException {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        char[] chars = new char[increment];
        int len = NumberUtils.writeBigInteger(bigInteger, chars, 0);
        writer.write(chars, 0, len);
    }

    @Override
    void writeJSONStringKey(String value) throws IOException {
        writer.append('"').append(value).append('"').append(':');
    }

    @Override
    public void write(int c) throws IOException {
        writer.write(c);
    }

    @Override
    public Writer append(char c) throws IOException {
        return writer.append(c);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        writer.write(cbuf);
    }

    @Override
    public void write(String str) throws IOException {
        writer.write(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        writer.write(str, off, len);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

package io.github.wycst.wast.json;

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
    protected boolean endsWith(int c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeShortChars(char[] chars, int offset, int len) throws IOException {
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
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int count = writeLong(numValue, buf, 0);
        writer.write(buf, 0, count);
    }

    @Override
    public void writeInt(int numValue) throws IOException {
        if (numValue == 0) {
            writer.write('0');
            return;
        }
        if (numValue < 0) {
            if (numValue == Integer.MIN_VALUE) {
                writer.write("-2147483648");
                return;
            }
            numValue = -numValue;
            writer.write('-');
        }
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int count = writeInteger(numValue, buf, 0);
        writer.write(buf, 0, count);
    }

    @Override
    public void writeUUID(UUID uuid) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        writeUUID(uuid, buf, 0);
        writer.write('"');
        writer.write(buf, 0, 36);
        writer.write('"');
    }

    @Override
    public void writeDouble(double numValue) throws IOException {
        char[] chars = JSONGeneral.CACHED_CHARS_36.get();
        int len = writeDouble(numValue, chars, 0);
        writer.write(chars, 0, len);
    }

    @Override
    public void writeFloat(float numValue) throws IOException {
        char[] chars = JSONGeneral.CACHED_CHARS_36.get();
        int len = writeFloat(numValue, chars, 0);
        writer.write(chars, 0, len);
    }

    @Override
    public void writeJSONLocalDateTime(int year, int month, int day, int hour, int minute, int second, int nano, String zoneId) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int off = 0;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeInteger(year, buf, off);
        }
        off += JSONMemoryHandle.putLong(buf, off, ((long) '-') << 48 | ((long) TWO_DIGITS_32_BITS[month]) << 16 | '-');
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        off += JSONMemoryHandle.putLong(buf, off, mergeInt64(hour, 'T', ':'));
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[minute]);
        buf[off++] = ':';
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        if (nano > 0) {
            off = writeNano(nano, buf, off);
        }
        if (zoneId.length() == 1) {
            off += JSONMemoryHandle.putInt(buf, off, Z_QUOT_INT);
            writer.write(buf, 0, off);
        } else {
            writer.write(buf, 0, off);
            writeZoneId(zoneId);
            writer.write('"');
        }
    }

    @Override
    public void writeJSONLocalDate(int year, int month, int day) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int off = 0;
        buf[off++] = '"';
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeLong(year, buf, off);
        }
        off += JSONMemoryHandle.putLong(buf, off, ((long) '-') << 48 | ((long) TWO_DIGITS_32_BITS[month]) << 16 | '-');
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        buf[off++] = '"';
        writer.write(buf, 0, off);
    }

    @Override
    public void writeTime(int hourOfDay, int minute, int second) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int off = 0;
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[hourOfDay]);
        off += JSONMemoryHandle.putLong(buf, off, mergeInt64(minute, ':', ':'));
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        writer.write(buf, 0, off);
    }

    @Override
    public void writeJSONTimeWithNano(int hourOfDay, int minute, int second, int nano) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int off = 0;
        buf[off++] = '"';
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[hourOfDay]);
        off += JSONMemoryHandle.putLong(buf, off, mergeInt64(minute, ':', ':')); // writeTwoDigitsAndPreSuffix(minute, ':', ':', buf, off);
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        if (nano > 0) {
            off = writeNano(nano, buf, off);
        }
        buf[off++] = '"';
        writer.write(buf, 0, off);
    }

    @Override
    public void writeDate(int year, int month, int day, int hourOfDay, int minute, int second) throws IOException {
        char[] buf = JSONGeneral.CACHED_CHARS_36.get();
        int off = 0;
        if (year < 0) {
            buf[off++] = '-';
            year = -year;
        }
        if (year < 10000) {
            off += JSONMemoryHandle.putLong(buf, off, FOUR_DIGITS_64_BITS[year]);
        } else {
            off += writeLong(year, buf, off);
        }
        off += JSONMemoryHandle.putLong(buf, off, mergeInt64(month, '-', '-'));
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[day]);
        off += JSONMemoryHandle.putLong(buf, off, mergeInt64(hourOfDay, ' ', ':'));
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[minute]);
        buf[off++] = ':';
        off += JSONMemoryHandle.putInt(buf, off, TWO_DIGITS_32_BITS[second]);
        writer.write(buf, 0, off);
    }

    @Override
    public void writeBigInteger(BigInteger bigInteger) throws IOException {
        int increment = ((bigInteger.bitLength() / 60) + 1) * 18;
        char[] chars = new char[increment];
        int len = writeBigInteger(bigInteger, chars, 0);
        writer.write(chars, 0, len);
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

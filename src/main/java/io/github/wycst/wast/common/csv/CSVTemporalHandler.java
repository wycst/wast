package io.github.wycst.wast.common.csv;

import io.github.wycst.wast.common.beans.DateParser;
import io.github.wycst.wast.common.beans.GregorianDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @Date 2024/1/12 10:08
 * @Created by wangyc
 */
public interface CSVTemporalHandler {

    class CSVLocalDateHandler extends CSVTypeHandler<LocalDate> {
        @Override
        public LocalDate handle(String input, Class<LocalDate> type) throws Throwable {
            if (input == null || input.length() == 0) return null;
            GregorianDate date = DateParser.parseDate(input);
            return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
        }
    }

    class CSVLocalDateTimeHandler extends CSVTypeHandler<LocalDateTime> {
        @Override
        public LocalDateTime handle(String input, Class<LocalDateTime> type) throws Throwable {
            if (input == null || input.length() == 0) return null;
            GregorianDate date = DateParser.parseDate(input);
            return LocalDateTime.of(date.getYear(), date.getMonth(), date.getDay(), date.getHourOfDay(), date.getMinute(), date.getSecond());
        }
    }

    class CSVLocalTimeHandler extends CSVTypeHandler<LocalTime> {
        @Override
        public LocalTime handle(String input, Class<LocalTime> type) throws Throwable {
            if (input == null || input.length() == 0) return null;
            return LocalTime.parse(input);
        }
    }

}

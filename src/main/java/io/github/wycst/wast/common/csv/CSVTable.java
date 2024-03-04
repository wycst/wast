package io.github.wycst.wast.common.csv;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class CSVTable {

    private final CSVRow columns;
    private final List<CSVRow> rows;

    CSVTable(List<CSVRow> csvRows) {
        columns = csvRows.get(0);
        rows = csvRows.subList(1, csvRows.size());
    }

    CSVTable() {
        columns = new CSVRow(this, new ArrayList<String>());
        rows = new ArrayList<CSVRow>();
    }

    CSVTable(String[] columnNames) {
        columns = new CSVRow(this, Arrays.asList(columnNames));
        rows = new ArrayList<CSVRow>();
    }

    public static CSVTable create() {
        return new CSVTable();
    }

    public static CSVTable create(String[] columnNames) {
        return new CSVTable(columnNames);
    }

    void setRows(List<CSVRow> csvRows) {
        if(csvRows.size() > 0) {
            rows.clear();
            CSVRow header = csvRows.get(0);
            List<CSVRow> dataRows = csvRows.subList(1, csvRows.size());
            columns.values = header.values;
            rows.addAll(dataRows);
        }
    }

    public void setColumnNames(String[] columnNames) {
        columns.values = Arrays.asList(columnNames);
    }

    public void setColumnNames(List<String> columnNames) {
        columns.values = columnNames;
    }

    public void addRow(List<String> values) {
        rows.add(new CSVRow(this, values));
    }

    public void set(int index, List<String> values) {
        rows.set(index, new CSVRow(this, values));
    }

    public void clearRows() {
        rows.clear();
    }

    public void removeAt(int index) {
        rows.remove(index);
    }

    public CSVRow getColumns() {
        return columns;
    }

    public List<CSVRow> getRows() {
        return rows;
    }

    public CSVRow getRow(int index) {
        return rows.get(index);
    }

    public int size() {
        return rows.size();
    }

    public int getColumnIndex(String column) {
        return columns.indexOf(column);
    }

    /**
     * 转化为列表数据
     *
     * @param eClass
     * @return
     * @param <E>
     */
    public <E> List<E> asEntityList(Class<E> eClass) {
        return asEntityList(eClass, null);
    }

    /**
     * 转化为列表数据
     *
     * @param eClass
     * @param eClass
     * @return
     * @param <E>
     */
    public <E> List<E> asEntityList(Class<E> eClass, Map<String, String> columnMapping) {
        List<E> list = new ArrayList<E>();
        for (CSVRow csvRow : rows) {
            list.add(csvRow.toBean(eClass, columnMapping));
        }
        return list;
    }

    public void setValue(int rowIndex, int columnIndex, String value) {
        rows.get(rowIndex).set(columnIndex, value);
    }

    public void writeTo(File file) {
        try {
            if(!file.exists()) {
                file.createNewFile();
            }
            writeTo(new FileOutputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTo(OutputStream os) {
        writeTo(os, Charset.defaultCharset());
    }

    public void writeTo(OutputStream os, String charsetName) {
        writeTo(os, Charset.forName(charsetName));
    }

    void writeTo(OutputStream os, Charset charset) {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os, charset));
        try {
            writeTo(bufferedWriter);
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void writeTo(Appendable appendable) {
        try {
            CSV.writeRow(appendable, columns.values);
            for (CSVRow row : rows) {
                CSV.writeRow(appendable, row.values);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toCSVString() {
        StringBuilder builder = new StringBuilder();
        writeTo(builder);
        return builder.toString();
    }
}

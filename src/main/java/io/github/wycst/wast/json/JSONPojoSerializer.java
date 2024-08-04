package io.github.wycst.wast.json;

import io.github.wycst.wast.common.compiler.JavaSourceObject;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;

import java.io.IOException;
import java.util.Collection;

/**
 *
 * @Date 2024/3/15 8:59
 * @Created by wangyc
 */
public class JSONPojoSerializer<T> extends JSONTypeSerializer {

    protected final JSONPojoStructure pojoStructure;
    protected final Class<?> pojoClass;

    protected JSONPojoSerializer(Class<T> pojoClass) {
        this.pojoClass = pojoClass;
        pojoStructure = JSONPojoStructure.get(pojoClass);
    }

    protected JSONPojoSerializer(JSONPojoStructure pojoStructure) {
        this.pojoStructure = pojoStructure;
        this.pojoClass = pojoStructure.getSourceClass();
    }

    @Override
    final void initialize() {
        pojoStructure.ensureInitializedFieldSerializers();
    }

    public void serializePojoCompact(T entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        boolean writeFullProperty = jsonConfig.isFullProperty();
        boolean writeClassName = jsonConfig.isWriteClassName();

        boolean isEmptyFlag = !checkWriteClassName(writeClassName, writer, pojoClass, false, indentLevel, jsonConfig);
        JSONPojoFieldSerializer[] fieldSerializers = pojoStructure.getFieldSerializers(jsonConfig.isUseFields());

        boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
        boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();
        for (JSONPojoFieldSerializer fieldSerializer : fieldSerializers) {
            GetterInfo getterInfo = fieldSerializer.getterInfo;
            if (!getterInfo.existField() && skipGetterOfNoExistField) {
                continue;
            }
            Object value = JSON_SECURE_TRUSTED_ACCESS.get(getterInfo, entity); // getterInfo.invoke(entity);
            if (value == null && !writeFullProperty)
                continue;
            if (isEmptyFlag) {
                isEmptyFlag = false;
            } else {
                writer.writeJSONToken(',');
            }
            if (value != null) {
                if (unCamelCaseToUnderline) {
                    fieldSerializer.writeFieldNameAndColonTo(writer);
                } else {
                    writer.append('"').append(getterInfo.getUnderlineName()).append("\":");
                }
                fieldSerializer.serializer.serialize(value, writer, jsonConfig, -1);
            } else {
                if (unCamelCaseToUnderline) {
                    fieldSerializer.writeJSONFieldNameWithNull(writer);
                } else {
                    writer.append('"').append(getterInfo.getUnderlineName()).append("\":null");
                }
            }
        }
    }

    public void serializePojoFormatOut(T entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        boolean writeFullProperty = jsonConfig.isFullProperty();
        boolean writeClassName = jsonConfig.isWriteClassName();
        boolean formatOutColonSpace = jsonConfig.isFormatOutColonSpace();
        boolean isEmptyFlag = !checkWriteClassName(writeClassName, writer, pojoClass, true, indentLevel, jsonConfig);
        JSONPojoFieldSerializer[] fieldSerializers = pojoStructure.getFieldSerializers(jsonConfig.isUseFields());

        boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
        boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();
        int indentPlus = indentLevel + 1;
        for (JSONPojoFieldSerializer fieldSerializer : fieldSerializers) {
            GetterInfo getterInfo = fieldSerializer.getterInfo;
            if (!getterInfo.existField() && skipGetterOfNoExistField) {
                continue;
            }
            Object value = JSON_SECURE_TRUSTED_ACCESS.get(getterInfo, entity); // getterInfo.invoke(entity);
            if (value == null && !writeFullProperty)
                continue;
            if (isEmptyFlag) {
                isEmptyFlag = false;
            } else {
                writer.writeJSONToken(',');
            }
            writeFormatOutSymbols(writer, indentPlus, true, jsonConfig);
            if (value != null) {
                if (unCamelCaseToUnderline) {
                    fieldSerializer.writeFieldNameAndColonTo(writer);
                } else {
                    writer.append('"').append(getterInfo.getUnderlineName()).append("\":");
                }
                if (formatOutColonSpace) {
                    writer.writeJSONToken(' ');
                }
                fieldSerializer.serializer.serialize(value, writer, jsonConfig, indentPlus);
            } else {
                if (unCamelCaseToUnderline) {
                    if (formatOutColonSpace) {
                        fieldSerializer.writeFieldNameAndColonTo(writer);
                        writer.write(" null");
                    } else {
                        fieldSerializer.writeJSONFieldNameWithNull(writer);
                    }
                } else {
                    writer.writeJSONToken('"');
                    writer.write(getterInfo.getUnderlineName());
                    writer.writeJSONToken('"');
                    if (formatOutColonSpace) {
                        writer.write(": null");
                    } else {
                        writer.write(":null");
                    }
                }
            }
        }
        if (!isEmptyFlag) {
            writeFormatOutSymbols(writer, indentLevel, true, jsonConfig);
        }
    }

    @Override
    protected final void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        Class<?> entityClass = obj.getClass();
        if (entityClass == pojoClass) {
            int hashcode = -1;
            if (jsonConfig.skipCircularReference) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.writeNull();
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            writer.writeJSONToken('{');
            boolean formatOut = jsonConfig.formatOut;
            T entity = (T) obj;
            if (formatOut) {
                serializePojoFormatOut(entity, writer, jsonConfig, indentLevel);
            } else {
                serializePojoCompact(entity, writer, jsonConfig, indentLevel);
            }
            writer.write('}');
            if (jsonConfig.skipCircularReference) {
                jsonConfig.setStatus(hashcode, -1);
            }
        } else {
            JSONTypeSerializer serializer = getTypeSerializer(entityClass);
            serializer.serialize(obj, writer, jsonConfig, indentLevel);
        }
    }

    protected final static void doSerialize(JSONTypeSerializer serializer, Object fieldValue, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        serializer.serialize(fieldValue, writer, jsonConfig, indentLevel);
    }

    protected final static Object invokeValue(JSONPojoFieldSerializer fieldSerializer, Object pojo) throws Exception {
        return JSON_SECURE_TRUSTED_ACCESS.get(fieldSerializer.getterInfo, pojo);
    }

    protected final static <T> T invokeValue(JSONPojoFieldSerializer fieldSerializer, Object pojo, Class<T> tClass) throws Exception {
        return (T) JSON_SECURE_TRUSTED_ACCESS.get(fieldSerializer.getterInfo, pojo);
    }

    protected final static void writeMemory(JSONWriter jsonWriter, long fourChars1, long fourChars2, long fourBytes, int len) throws IOException {
        jsonWriter.writeMemory(fourChars1, fourChars2, fourBytes, len);
    }

    protected final static void writeMemory(JSONWriter jsonWriter, long fourChars, int fourBytes, int len) throws IOException {
        jsonWriter.writeMemory(fourChars, fourBytes, len);
    }

    protected final static void writeStringArrayFormatOut(JSONWriter jsonWriter, String[] values, JSONConfig jsonConfig, int level) throws IOException {
        final int levelPlus = level + 1;
        int len = values.length;
        if (len > 0) {
            jsonWriter.writeJSONToken('[');
            writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
            int i = 1;
            jsonWriter.writeStringCompatibleNull(values[0]);
            if ((len & 1) == 0) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeStringCompatibleNull(values[1]);
                ++i;
            }
            for (; i < len; i = i + 2) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeStringCompatibleNull(values[i]);
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeStringCompatibleNull(values[i + 1]);
            }
            writeFormatOutSymbols(jsonWriter, level, true, jsonConfig);
            jsonWriter.writeJSONToken(']');
        } else {
            jsonWriter.writeEmptyArray();
        }
    }

    protected final static void writeStringCollectionFormatOut(JSONWriter jsonWriter, Collection values, JSONConfig jsonConfig, int level) throws IOException {
        int size = values.size();
        final int levelPlus = level + 1;
        if (size > 0) {
            jsonWriter.writeJSONToken('[');
            boolean hasAddFlag = false;
            for (Object value : values) {
                if (hasAddFlag) {
                    jsonWriter.writeJSONToken(',');
                } else {
                    hasAddFlag = true;
                }
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeStringCompatibleNull((String) value);
            }
            writeFormatOutSymbols(jsonWriter, level, true, jsonConfig);
            jsonWriter.writeJSONToken(']');
        } else {
            jsonWriter.writeEmptyArray();
        }
    }

    protected final static void writeLongArrayFormatOut(JSONWriter jsonWriter, long[] values, JSONConfig jsonConfig, int level) throws IOException {
        final int levelPlus = level + 1;
        int len = values.length;
        if (len > 0) {
            jsonWriter.writeJSONToken('[');
            int i = 1;
            writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
            jsonWriter.writeLong(values[0]);
            if ((len & 1) == 0) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeLong(values[1]);
                ++i;
            }
            for (; i < len; i = i + 2) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeLong(values[i]);
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeLong(values[i + 1]);
            }
            writeFormatOutSymbols(jsonWriter, level, jsonConfig);
            jsonWriter.writeJSONToken(']');
        } else {
            jsonWriter.writeEmptyArray();
        }
    }

    protected final static void writeDoubleArrayFormatOut(JSONWriter jsonWriter, double[] values, JSONConfig jsonConfig, int level) throws IOException {
        final int levelPlus = level + 1;
        int len = values.length;
        if (len > 0) {
            jsonWriter.writeJSONToken('[');
            int i = 1;
            writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
            jsonWriter.writeDouble(values[0]);
            if ((len & 1) == 0) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeDouble(values[1]);
                ++i;
            }
            for (; i < len; i = i + 2) {
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeDouble(values[i]);
                jsonWriter.writeJSONToken(',');
                writeFormatOutSymbols(jsonWriter, levelPlus, jsonConfig);
                jsonWriter.writeDouble(values[i + 1]);
            }
            writeFormatOutSymbols(jsonWriter, level, jsonConfig);
            jsonWriter.writeJSONToken(']');
        } else {
            jsonWriter.writeEmptyArray();
        }
    }

    static JavaSourceObject generateRuntimeJavaCodeSource(JSONPojoStructure jsonPojoStructure) {
        return JSONPojoSerializerCodeGen.generateJavaCodeSource(jsonPojoStructure, false, true);
    }

    static JavaSourceObject generateJavaCodeSource(JSONPojoStructure jsonPojoStructure, boolean printJavaSource) {
        return JSONPojoSerializerCodeGen.generateJavaCodeSource(jsonPojoStructure, printJavaSource, false);
    }

    /**
     * Generate serialized Java source code based on the pojo class
     * Using Java compilation can improve performance 20%
     *
     * @param pojoClass
     * @param printJavaSource if print the gen code
     * @return
     */
    public static JavaSourceObject generateJavaCodeSource(Class<?> pojoClass, boolean printJavaSource) {
        return generateJavaCodeSource(pojoClass, printJavaSource, false);
    }

    /**
     * Generate serialized Java source code based on the pojo class
     * Using Java compilation can improve performance 20%
     *
     * @param pojoClass
     * @param printJavaSource if print the gen code
     * @param runtime
     * @return
     */
    public static JavaSourceObject generateJavaCodeSource(Class<?> pojoClass, boolean printJavaSource, boolean runtime) {
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(pojoClass);
        if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
            throw new UnsupportedOperationException(pojoClass + " is not a pojo class");
        }
        JSONPojoStructure jsonPojoStructure = JSONPojoStructure.get(pojoClass);
        if (!jsonPojoStructure.isSupportedJavaBeanConvention()) {
            throw new UnsupportedOperationException(pojoClass + " is not supported for code generator");
        }
        return JSONPojoSerializerCodeGen.generateJavaCodeSource(jsonPojoStructure, printJavaSource, runtime);
    }

    /**
     * Generate serialized Java source code based on the pojo class
     * Using Java compilation can improve performance 20%
     *
     * @param pojoClass
     * @return
     */
    public static JavaSourceObject generateJavaCodeSource(Class<?> pojoClass) {
        return generateJavaCodeSource(pojoClass, false);
    }
}

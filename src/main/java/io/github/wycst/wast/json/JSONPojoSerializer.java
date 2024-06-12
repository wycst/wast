package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.compiler.JavaSourceObject;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Still under testing
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
        init();
    }

    protected JSONPojoSerializer(JSONPojoStructure pojoStructure) {
        this.pojoStructure = pojoStructure;
        this.pojoClass = pojoStructure.getSourceClass();
        init();
    }

    public void init() {
    }

    public void serializePojoCompact(T entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        boolean writeFullProperty = jsonConfig.isFullProperty();
        boolean writeClassName = jsonConfig.isWriteClassName();

        boolean isEmptyFlag = !checkWriteClassName(writeClassName, writer, pojoClass, false, indentLevel, jsonConfig);
        JSONPojoFieldSerializer[] fieldSerializers = pojoStructure.getFieldSerializers(jsonConfig.isUseFields());

        boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
        boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();
        for (JSONPojoFieldSerializer fieldSerializer : fieldSerializers) {
            GetterInfo getterInfo = fieldSerializer.getGetterInfo();
            if (!getterInfo.existField() && skipGetterOfNoExistField) {
                continue;
            }
            Object value = getterInfo.invoke(entity);
            if (value == null && !writeFullProperty)
                continue;
            if (isEmptyFlag) {
                isEmptyFlag = false;
            } else {
                writer.writeJSONToken(',');
            }
            if (value != null) {
                if (unCamelCaseToUnderline) {
                    fieldSerializer.writeJSONFieldName(writer);
                } else {
                    writer.append('"').append(getterInfo.getUnderlineName()).append("\":");
                }
                JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                serializer.serialize(value, writer, jsonConfig, -1);
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
            GetterInfo getterInfo = fieldSerializer.getGetterInfo();
            if (!getterInfo.existField() && skipGetterOfNoExistField) {
                continue;
            }
            Object value = getterInfo.invoke(entity);
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
                    fieldSerializer.writeJSONFieldName(writer);
                } else {
                    writer.append('"').append(getterInfo.getUnderlineName()).append("\":");
                }
                if(formatOutColonSpace) {
                    writer.writeJSONToken(' ');
                }
                JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                serializer.serialize(value, writer, jsonConfig, indentPlus);
            } else {
                if (unCamelCaseToUnderline) {
                    if (formatOutColonSpace) {
                        fieldSerializer.writeJSONFieldName(writer);
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
            if (jsonConfig.isSkipCircularReference()) {
                if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                    writer.write(NULL);
                    return;
                }
                jsonConfig.setStatus(hashcode, 0);
            }
            pojoStructure.ensureInitialized();
            writer.writeJSONToken('{');
            boolean formatOut = jsonConfig.isFormatOut();
            T entity = (T) obj;
            if (formatOut) {
                serializePojoFormatOut(entity, writer, jsonConfig, indentLevel);
            } else {
                serializePojoCompact(entity, writer, jsonConfig, indentLevel);
            }
            writeEndPojo(writer);
            jsonConfig.setStatus(hashcode, -1);
        } else {
            JSONTypeSerializer serializer = getTypeSerializer(entityClass);
            serializer.serialize(obj, writer, jsonConfig, indentLevel);
        }
    }

    protected void writeEndPojo(JSONWriter writer) throws IOException {
        writer.write('}');
    }

    protected final void doSerialize(JSONTypeSerializer serializer, Object fieldValue, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        serializer.serialize(fieldValue, writer, jsonConfig, indentLevel);
    }

    final static String IMPORT_CODE_TEXT =
            "import io.github.wycst.wast.json.JSONConfig;\n" +
                    "import io.github.wycst.wast.json.JSONPojoFieldSerializer;\n" +
                    "import io.github.wycst.wast.json.JSONPojoStructure;\n\n" +
                    "import io.github.wycst.wast.json.JSONWriter;\n\n";

    final static AtomicLong SEQ = new AtomicLong(1);


    static JavaSourceObject generateRuntimeJavaCodeSource(JSONPojoStructure jsonPojoStructure) {
        return generateJavaCodeSource(jsonPojoStructure, false, true);
    }

    static JavaSourceObject generateJavaCodeSource(JSONPojoStructure jsonPojoStructure, boolean printSource, boolean runtime) {
        Class<?> pojoClass = jsonPojoStructure.getSourceClass();
        String simpleName = pojoClass.getSimpleName();
        String canonicalName = pojoClass.getCanonicalName();
        String genClassName = "__JPS_" + simpleName + "_" + SEQ.getAndIncrement();
        String packageName = pojoClass.getPackage().getName();
        StringBuilder codeBuilder = new StringBuilder(2048);
        codeBuilder.append("package ").append(packageName).append(";\n\n");
        codeBuilder.append(IMPORT_CODE_TEXT);
        if(!runtime) {
            codeBuilder.append("/**\n");
            codeBuilder.append(" * pojo serializer \n");
            codeBuilder.append(" * @Date " + new GregorianDate() + "\n");
            codeBuilder.append(" * @Created by code generator\n");
            codeBuilder.append(" */\n");
        }
        codeBuilder.append("public final class ").append(genClassName).append(" extends io.github.wycst.wast.json.JSONPojoSerializer<").append(canonicalName).append("> {\n\n");
        // fieldset
        StringBuilder fieldsDefinitionBuilder = new StringBuilder(64);
        StringBuilder fieldSetBuilder = new StringBuilder(64);

        StringBuilder serializePojoCompactHeaderBuilder = new StringBuilder(1024);
        serializePojoCompactHeaderBuilder.append("\t\tboolean isEmptyFlag = !checkWriteClassName(jsonConfig.isWriteClassName(), writer, pojoClass, false, indentLevel, jsonConfig);\n");

        StringBuilder serializePojoCompactBodyBuilder = new StringBuilder(1024);


        JSONPojoFieldSerializer[] fieldSerializerUseMethods = jsonPojoStructure.getFieldSerializers(false);
        fieldsDefinitionBuilder.append("\n");

        StringBuilder fieldNameTempBuilder = new StringBuilder();

        boolean appendFieldSerializersFlag = false;
        boolean appendUnCamelCaseToUnderlineFlag = false;
        if (fieldSerializerUseMethods.length > 0) {
            int fieldIndex = 0;
            boolean ensureNotEmptyFlag = false;
            for (JSONPojoFieldSerializer fieldSerializer : fieldSerializerUseMethods) {
                boolean firstFlag = fieldIndex == 0;
                GetterInfo getterInfo = fieldSerializer.getGetterInfo();
                String name = getterInfo.getName();
                String underlineName = getterInfo.getUnderlineName();
                boolean nameEqualUnderlineName = name.equals(underlineName);
                if(!nameEqualUnderlineName) {
                    appendUnCamelCaseToUnderlineFlag = true;
                }
                boolean primitive = getterInfo.isPrimitive();
                Class<?> returnType = getterInfo.getReturnType();
                String returnTypeName = returnType.getName().intern();
                boolean accessFlag = getterInfo.isAccess() && Modifier.isPublic(returnType.getModifiers());

                String fieldSerializerName = name + "UseMethodSerializer";
                String valueVar = fieldSerializer.getName();
                byte[] bytes = valueVar.getBytes();
                boolean isFieldNameAscii = bytes.length == valueVar.length();
                long[] longs = null, longsWithComma = null;
                int[] ints = null, intsWithComma = null;
                int fieldNameTokenLength = 0;
                boolean useUnsafe = runtime && isFieldNameAscii && bytes.length <= 12;
                if (useUnsafe) {
                    fieldNameTempBuilder.setLength(0);
                    fieldNameTempBuilder.append(",\"").append(valueVar).append("\":");
                    fieldNameTokenLength = fieldNameTempBuilder.length();
                    longs = UnsafeHelper.getLongs(fieldNameTempBuilder.substring(1));
                    longsWithComma = UnsafeHelper.getLongs(fieldNameTempBuilder.toString());
                    ints = UnsafeHelper.getInts(fieldNameTempBuilder.substring(1));
                    intsWithComma = UnsafeHelper.getInts(fieldNameTempBuilder.toString());
                }

                if (accessFlag) {
                    serializePojoCompactBodyBuilder.append("\t\t" + returnType.getCanonicalName() + " " + valueVar + " = entity." + getterInfo.generateCode() + ";\n");
                    if (primitive) {
                        boolean isBoolean = returnType == boolean.class;
                        if (firstFlag) {
                            if (isBoolean) {
                                // bool
                                serializePojoCompactBodyBuilder.append("\t\tif(" + valueVar + ") {\n");
                                if (nameEqualUnderlineName) {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":true\");\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":true\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":true\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                }
                                serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                if (nameEqualUnderlineName) {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":false\");\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":false\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":false\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                }
                                serializePojoCompactBodyBuilder.append("\t\t}\n");
                            } else {
                                // double/float/long/int/short/byte/char
                                if (nameEqualUnderlineName) {
                                    serializePojoCompactBodyBuilder.append("\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\tif (unCamelCaseToUnderline) {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                }
                                if (returnType == double.class) {
//                                    serializePojoCompactBodyBuilder.append("\t\tif(jsonConfig.isWriteDecimalUseToString()) {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(Double.toString(" + valueVar + "));\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeDouble(" + valueVar + ");\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeDouble(" + valueVar + ");\n");
                                } else if (returnType == float.class) {
//                                    serializePojoCompactBodyBuilder.append("\t\tif(jsonConfig.isWriteDecimalUseToString()) {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(Float.toString(" + valueVar + "));\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeFloat(" + valueVar + ");\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeFloat(" + valueVar + ");\n");
                                } else if (returnType == long.class || returnType == int.class || returnType == short.class || returnType == byte.class) {
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeLong(" + valueVar + ");\n");
                                } else {
                                    // char
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeJSONToken('\"');\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.write(" + valueVar + ");\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeJSONToken('\"');\n");
                                }
                            }
                        } else {
                            if (isBoolean) {
                                if (ensureNotEmptyFlag) {
                                    serializePojoCompactBodyBuilder.append("\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":true\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":false\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\tif(isEmptyFlag) {\n");
                                    // serializePojoCompactBodyBuilder.append("\t\t\tisEmptyFlag = false;\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":true\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":false\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":true\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":true\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":false\");\n");
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":false\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                }
                            } else {
                                if (ensureNotEmptyFlag) {
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longsWithComma.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longsWithComma.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                        }
                                        serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t}\n");
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\tif(isEmptyFlag) {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength - 1;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longs.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength - 1;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longs.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                        }
                                        serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longsWithComma.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe && (total & 3) != 1) {
                                            for (int i = 0; i < longsWithComma.length; ++i) {
                                                int len = total > 4 ? 4 : total;
                                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        } else {
                                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                        }
                                        serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                        serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                }
                                if (returnType == double.class) {
//                                    serializePojoCompactBodyBuilder.append("\t\tif(jsonConfig.isWriteDecimalUseToString()) {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(Double.toString(" + valueVar + "));\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeDouble(" + valueVar + ");\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeDouble(" + valueVar + ");\n");
                                } else if (returnType == float.class) {
//                                    serializePojoCompactBodyBuilder.append("\t\tif(jsonConfig.isWriteDecimalUseToString()) {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(Float.toString(" + valueVar + "));\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t} else {\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeFloat(" + valueVar + ");\n");
//                                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeFloat(" + valueVar + ");\n");
                                } else if (returnType == long.class || returnType == int.class || returnType == short.class || returnType == byte.class) {
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeLong(" + valueVar + ");\n");
                                } else if (returnType == char.class) {
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeJSONToken('\"');\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.write(" + valueVar + ");\n");
                                    serializePojoCompactBodyBuilder.append("\t\twriter.writeJSONToken('\"');\n");
                                }
                            }
                        }
                        ensureNotEmptyFlag = true;
                    } else {
                        serializePojoCompactBodyBuilder.append("\t\tif(" + valueVar + " != null) {\n");
                        // field name
                        if (firstFlag) {
                            if (nameEqualUnderlineName) {
                                int total = fieldNameTokenLength - 1;
                                if (useUnsafe && (total & 3) != 1) {
                                    for (int i = 0; i < longs.length; ++i) {
                                        int len = total > 4 ? 4 : total;
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                        total -= 4;
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                }
                            } else {
                                serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                int total = fieldNameTokenLength - 1;
                                if (useUnsafe && (total & 3) != 1) {
                                    for (int i = 0; i < longs.length; ++i) {
                                        int len = total > 4 ? 4 : total;
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                        total -= 4;
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                }
                                serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                            }
                            serializePojoCompactBodyBuilder.append("\t\t\tisEmptyFlag = false;\n");
                        } else {
                            if (ensureNotEmptyFlag) {
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longsWithComma.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longsWithComma.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                                }
                            } else {
                                serializePojoCompactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength - 1;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longs.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength - 1;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longs.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.writeUnsafe(" + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                }
                                serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longsWithComma.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                    }
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe && (total & 3) != 1) {
                                        for (int i = 0; i < longsWithComma.length; ++i) {
                                            int len = total > 4 ? 4 : total;
                                            serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.writeUnsafe(" + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    } else {
                                        serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                    }
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                    serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                }
                                serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                            }
                        }
                        boolean useSerializerInvokeFlag = false;
                        // field value
                        if (returnType == String.class) {
                            if (runtime) {
                                if (EnvUtils.JDK_9_PLUS) {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONStringBytes(" + valueVar + ", (byte[]) getStringValue(" + valueVar + "));\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONChars(getChars(" + valueVar + "));\n");
                                }
                            } else {
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONString(" + valueVar + ");\n");
                            }
                        } else if (returnType == BigDecimal.class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(" + valueVar + ".toString());\n");
                        } else if (returnType == BigInteger.class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeBigInteger(" + valueVar + ");\n");
                        } else if (returnType == String[].class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeStringArray(" + valueVar + ");\n");
                        } else if (returnType == double[].class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeDoubleArray(" + valueVar + ");\n"); }
                        else if (returnType == long[].class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeLongArray(" + valueVar + ");\n");
                        } else if (returnType == UUID.class) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.writeUUID(" + valueVar + ");\n");
                        } else if (returnTypeName == "java.time.Instant") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                // use default pattern
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONInstant(" + valueVar + ".getEpochSecond(), " + valueVar + ".getNano());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if(returnTypeName == "java.time.LocalTime") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONTimeWithNano(" + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.LocalDate") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONLocalDate(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.LocalDateTime") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), null);\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.ZonedDateTime" || returnTypeName == "java.time.OffsetDateTime") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                if(returnTypeName == "java.time.ZonedDateTime") {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getZone().getId());\n");
                                } else {
                                    serializePojoCompactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getOffset().getId());\n");
                                }
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else {
                            useSerializerInvokeFlag = true;
                        }
                        if(useSerializerInvokeFlag) {
                            if (!appendFieldSerializersFlag) {
                                appendFieldSerializersFlag = true;
                                fieldSetBuilder.append("\t\tJSONPojoFieldSerializer[] fieldSerializerUseMethods = pojoStructure.getFieldSerializers(false);\n");
                            }
                            // use unsafe & field offset
                            fieldsDefinitionBuilder.append("\tfinal JSONPojoFieldSerializer ").append(fieldSerializerName).append(";\n");
                            fieldSetBuilder.append("\t\tthis.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseMethods[").append(fieldIndex).append("];\n");

                            serializePojoCompactBodyBuilder.append("\t\t\tdoSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", writer, jsonConfig, -1);\n");
                        }
                        serializePojoCompactBodyBuilder.append("\t\t}\n");
                    }
                } else {
                    if (!appendFieldSerializersFlag) {
                        appendFieldSerializersFlag = true;
                        fieldSetBuilder.append("\t\tJSONPojoFieldSerializer[] fieldSerializerUseMethods = pojoStructure.getFieldSerializers(false);\n");
                    }
                    // use unsafe & field offset
                    fieldsDefinitionBuilder.append("\tfinal JSONPojoFieldSerializer ").append(fieldSerializerName).append(";\n");
                    fieldSetBuilder.append("\t\tthis.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseMethods[").append(fieldIndex).append("];\n");

                    serializePojoCompactBodyBuilder.append("\t\tObject " + valueVar + " = " + fieldSerializerName + ".invoke(entity);\n");
                    serializePojoCompactBodyBuilder.append("\t\tif(" + valueVar + " != null) {\n");
                    if (firstFlag) {
                        if (nameEqualUnderlineName) {
                            serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                        } else {
                            serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                            serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                            serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                            serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                        }
                        serializePojoCompactBodyBuilder.append("\t\t\tisEmptyFlag = false;\n");
                    } else {
                        if (ensureNotEmptyFlag) {
                            if (nameEqualUnderlineName) {
                                serializePojoCompactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                            } else {
                                serializePojoCompactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                            }
                        } else {
                            if (nameEqualUnderlineName) {
                                serializePojoCompactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                            } else {
                                serializePojoCompactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");

                                serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + valueVar + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");

                                serializePojoCompactBodyBuilder.append("\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + valueVar + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t} else {\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t\t}\n");
                                serializePojoCompactBodyBuilder.append("\t\t\t}\n");
                            }
                        }
                    }
                    serializePojoCompactBodyBuilder.append("\t\t\tdoSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", writer, jsonConfig, -1);\n");
                    serializePojoCompactBodyBuilder.append("\t\t}\n");
                }

                serializePojoCompactBodyBuilder.append("\n");
                fieldIndex++;
            }
        }

        if(appendUnCamelCaseToUnderlineFlag) {
            serializePojoCompactHeaderBuilder.append("\t\tboolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();\n\n");
        } else {
            serializePojoCompactHeaderBuilder.append("\n");
        }
        if(!runtime) {
            codeBuilder.append("\tpublic ").append(genClassName).append("() {\n");
            codeBuilder.append("\t\tthis(JSONPojoStructure.get(" + canonicalName + ".class));\n");
            codeBuilder.append("\t}\n\n");
        }

        // create fun
        codeBuilder.append("\tprotected ").append(genClassName).append("(JSONPojoStructure pojoStructure) {\n");
        codeBuilder.append("\t\tsuper(pojoStructure);\n");
        codeBuilder.append(fieldSetBuilder);
        codeBuilder.append("\t}\n");
        codeBuilder.append(fieldsDefinitionBuilder).append("\n");

        codeBuilder.append("\tpublic void serializePojoCompact(").append(canonicalName).append(" entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {\n\n");
        codeBuilder.append(serializePojoCompactHeaderBuilder);
        codeBuilder.append(serializePojoCompactBodyBuilder);
        codeBuilder.append("\t}\n");
        codeBuilder.append("}\n");
        codeBuilder.append("\n");

        String code = codeBuilder.toString();
        if (printSource) {
            System.out.println(code);
        }

        return new JavaSourceObject(packageName, genClassName, code);
    }

    static JavaSourceObject generateJavaCodeSource(JSONPojoStructure jsonPojoStructure, boolean printJavaSource) {
        return generateJavaCodeSource(jsonPojoStructure, printJavaSource, false);
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
        return generateJavaCodeSource(jsonPojoStructure, printJavaSource, runtime);
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

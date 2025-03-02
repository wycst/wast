package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.compiler.JavaSourceObject;
import io.github.wycst.wast.common.idgenerate.providers.IdGenerator;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Created by wangyc
 */
final class JSONPojoSerializerCodeGen {

    final static String IMPORT_CODE_TEXT =
            "import io.github.wycst.wast.json.JSONConfig;\n" +
                    "import io.github.wycst.wast.json.JSONPojoFieldSerializer;\n" +
                    "import io.github.wycst.wast.json.JSONPojoStructure;\n\n" +
                    "import io.github.wycst.wast.json.JSONWriter;\n\n";

    final static AtomicLong SEQ = new AtomicLong(1);

    static JavaSourceObject generateJavaCodeSource(JSONPojoStructure jsonPojoStructure, boolean printSource, boolean runtime) {
        final Class<?> pojoClass = jsonPojoStructure.getSourceClass();
        final String simpleName = pojoClass.getSimpleName();
        final String canonicalName = pojoClass.getCanonicalName();
        final String genClassName = "__JPS_" + simpleName + "_" + IdGenerator.hex();
        final String packageName = pojoClass.getPackage().getName();
        StringBuilder codeBuilder = new StringBuilder(2048);
        codeBuilder.append("package ").append(packageName).append(";\n\n");
        codeBuilder.append(IMPORT_CODE_TEXT);
        if (!runtime) {
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

        StringBuilder compactHeaderBuilder = new StringBuilder(1024);
        StringBuilder compactBodyBuilder = new StringBuilder(1024);

        StringBuilder fmatOutHeaderBuilder = new StringBuilder(1024);
        StringBuilder fmatOutBodyBuilder = new StringBuilder(1024);

        if (!jsonPojoStructure.isForceUseFields()) {
            compactHeaderBuilder.append("\t\tif(jsonConfig.isUseFields()) {\n");
            compactHeaderBuilder.append("\t\t\tsuper.serializePojoCompact(entity, writer, jsonConfig, indentLevel);\n");
            compactHeaderBuilder.append("\t\t\treturn;\n");
            compactHeaderBuilder.append("\t\t}\n");

            fmatOutHeaderBuilder.append("\t\tif(jsonConfig.isUseFields()) {\n");
            fmatOutHeaderBuilder.append("\t\t\tsuper.serializePojoFormatOut(entity, writer, jsonConfig, indentLevel);\n");
            fmatOutHeaderBuilder.append("\t\t\treturn;\n");
            fmatOutHeaderBuilder.append("\t\t}\n");
        }
        compactHeaderBuilder.append("\t\tboolean isEmptyFlag = !checkWriteClassName(jsonConfig.isWriteClassName(), writer, pojoClass, false, indentLevel, jsonConfig);\n");

        fmatOutHeaderBuilder.append("\t\tboolean isEmptyFlag = !checkWriteClassName(jsonConfig.isWriteClassName(), writer, pojoClass, true, indentLevel, jsonConfig);\n");
        fmatOutHeaderBuilder.append("\t\tint indentPlus = indentLevel + 1;\n");
        fmatOutHeaderBuilder.append("\t\tboolean formatOutColonSpace = jsonConfig.formatOutColonSpace;\n");

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
                GetterInfo getterInfo = fieldSerializer.getterInfo;
                String name = getterInfo.getName();
                String underlineName = getterInfo.getUnderlineName();
                boolean nameEqualUnderlineName = name.equals(underlineName);
                if (!nameEqualUnderlineName) {
                    appendUnCamelCaseToUnderlineFlag = true;
                }
                boolean primitive = getterInfo.isPrimitive();
                Class<?> returnType = getterInfo.getReturnType();
                String returnTypeName = returnType.getName().intern();
                boolean accessFlag = getterInfo.isAccess() && Modifier.isPublic(returnType.getModifiers());
                boolean isBoolean = returnType == boolean.class;

                String fieldSerializerName = name + "UseMethodSerializer";
                final String fieldKey = fieldSerializer.getName();
                final String valueVar = "__" + fieldKey;
                byte[] bytes = valueVar.getBytes();
                boolean isFieldNameAscii = bytes.length == valueVar.length();
                long[] longs = null, longsWithComma = null, longsWithCommaBoolFalse = null;
                int[] ints = null, intsWithComma = null, intsWithCommaBoolFalse = null;
                int fieldNameTokenLength = 0, fieldNameTokenBoolFalseLength = 0;

                long[] longsFormatOut = null;
                int[] intsFormatOut = null;
                int fieldNameFormatOutTokenLength = 0;

                boolean useUnsafe = runtime && isFieldNameAscii /*&& bytes.length <= 16*/;
                if (useUnsafe) {
                    fieldNameTempBuilder.setLength(0);
                    fieldNameTempBuilder.append(",\"").append(fieldKey).append("\":");
                    longsFormatOut = UnsafeHelper.getCharLongs(fieldNameTempBuilder.substring(1));
                    intsFormatOut = UnsafeHelper.getByteInts(fieldNameTempBuilder.substring(1));
                    fieldNameFormatOutTokenLength = fieldNameTempBuilder.length() - 1;
                    if (isBoolean) {
                        fieldNameTempBuilder.append("true");
                    }
                    fieldNameTokenLength = fieldNameTempBuilder.length();
                    fieldNameTokenBoolFalseLength = fieldNameTokenLength + 1;
                    longs = isBoolean ? UnsafeHelper.getCharLongs(fieldNameTempBuilder.substring(1)) : longsFormatOut;
                    longsWithComma = UnsafeHelper.getCharLongs(fieldNameTempBuilder.toString());
                    ints = isBoolean ? UnsafeHelper.getByteInts(fieldNameTempBuilder.substring(1)) : intsFormatOut;
                    intsWithComma = UnsafeHelper.getByteInts(fieldNameTempBuilder.toString());

                    if (isBoolean) {
                        fieldNameTempBuilder.setLength(fieldNameTempBuilder.length() - 4);
                        fieldNameTempBuilder.append("false");
                        longsWithCommaBoolFalse = UnsafeHelper.getCharLongs(fieldNameTempBuilder.toString());
                        intsWithCommaBoolFalse = UnsafeHelper.getByteInts(fieldNameTempBuilder.toString());
                    }
                }

                if (accessFlag) {
                    compactBodyBuilder.append("\t\t" + returnType.getCanonicalName() + " " + valueVar + " = entity." + getterInfo.generateCode() + ";\n");
                    fmatOutBodyBuilder.append("\t\t" + returnType.getCanonicalName() + " " + valueVar + " = entity." + getterInfo.generateCode() + ";\n");

                    // generate format code
                    generateSerializeFieldFormatOutCode(ensureNotEmptyFlag, primitive, fieldSerializerName, fieldKey, valueVar, nameEqualUnderlineName, underlineName, useUnsafe, longsFormatOut, intsFormatOut, fieldNameFormatOutTokenLength, returnType, fieldSerializer, runtime, fmatOutBodyBuilder);

                    if (primitive) {
                        if (firstFlag) {
                            if (isBoolean) {
                                // bool
                                compactBodyBuilder.append("\t\tif(" + valueVar + ") {\n");
                                if (nameEqualUnderlineName) {
                                    compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":true\");\n");
                                } else {
                                    compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":true\");\n");
                                    compactBodyBuilder.append("\t\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":true\");\n");
                                    compactBodyBuilder.append("\t\t\t}\n");
                                }
                                compactBodyBuilder.append("\t\t} else {\n");
                                if (nameEqualUnderlineName) {
                                    compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":false\");\n");
                                } else {
                                    compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":false\");\n");
                                    compactBodyBuilder.append("\t\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":false\");\n");
                                    compactBodyBuilder.append("\t\t\t}\n");
                                }
                                compactBodyBuilder.append("\t\t}\n");
                            } else {
                                // double/float/long/int/short/byte/char
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength - 1;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longs.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longs[i], l2 = longs[i + 1];
                                                int i1 = ints[i], i2 = ints[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\tif (unCamelCaseToUnderline) {\n");
                                    compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                    compactBodyBuilder.append("\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                    compactBodyBuilder.append("\t\t}\n");
                                }
                                if (returnType == double.class) {
                                    compactBodyBuilder.append("\t\twriter.writeDouble(" + valueVar + ");\n");
                                } else if (returnType == float.class) {
                                    compactBodyBuilder.append("\t\twriter.writeFloat(" + valueVar + ");\n");
                                } else if (returnType == long.class) {
                                    compactBodyBuilder.append("\t\twriter.writeLong(" + valueVar + ");\n");
                                } else if (returnType == int.class || returnType == short.class || returnType == byte.class) {
                                    compactBodyBuilder.append("\t\twriter.writeInt(" + valueVar + ");\n");
                                } else {
                                    // char
                                    compactBodyBuilder.append("\t\twriter.writeJSONChar(" + valueVar + ");\n");
                                }
                            }
                        } else {
                            if (isBoolean) {
                                if (ensureNotEmptyFlag) {
                                    compactBodyBuilder.append("\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":true\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":true\");\n");
                                        }
                                        compactBodyBuilder.append("\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":true\");\n");
                                        compactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenBoolFalseLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithCommaBoolFalse.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithCommaBoolFalse[i], l2 = longsWithCommaBoolFalse[i + 1];
                                                    int i1 = intsWithCommaBoolFalse[i], i2 = intsWithCommaBoolFalse[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longsWithCommaBoolFalse[i] + "L, " + intsWithCommaBoolFalse[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":false\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenBoolFalseLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithCommaBoolFalse.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithCommaBoolFalse[i], l2 = longsWithCommaBoolFalse[i + 1];
                                                    int i1 = intsWithCommaBoolFalse[i], i2 = intsWithCommaBoolFalse[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithCommaBoolFalse[i] + "L, " + intsWithCommaBoolFalse[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":false\");\n");
                                        }
                                        compactBodyBuilder.append("\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":false\");\n");
                                        compactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t}\n");
                                } else {
                                    compactBodyBuilder.append("\t\tif(isEmptyFlag) {\n");
                                    compactBodyBuilder.append("\t\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength - 1;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longs.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longs[i], l2 = longs[i + 1];
                                                    int i1 = ints[i], i2 = ints[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":true\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":true\");\n");
                                        compactBodyBuilder.append("\t\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":true\");\n");
                                        compactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":false\");\n");
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":false\");\n");
                                        compactBodyBuilder.append("\t\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":false\");\n");
                                        compactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t}\n");
                                    compactBodyBuilder.append("\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\tif(" + valueVar + ") {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken('e');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":true\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":true\");\n");
                                        compactBodyBuilder.append("\t\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":true\");\n");
                                        compactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":false\");\n");
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":false\");\n");
                                        compactBodyBuilder.append("\t\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":false\");\n");
                                        compactBodyBuilder.append("\t\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t}\n");
                                    compactBodyBuilder.append("\t\t}\n");
                                }
                            } else {
                                if (ensureNotEmptyFlag) {
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                        compactBodyBuilder.append("\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                        compactBodyBuilder.append("\t\t}\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\tif(isEmptyFlag) {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength - 1;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longs.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longs[i], l2 = longs[i + 1];
                                                    int i1 = ints[i], i2 = ints[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength - 1;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longs.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longs[i], l2 = longs[i + 1];
                                                    int i1 = ints[i], i2 = ints[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                        compactBodyBuilder.append("\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                        compactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t} else {\n");
                                    if (nameEqualUnderlineName) {
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                        int total = fieldNameTokenLength;
                                        if (useUnsafe /*&& (total & 3) != 1*/) {
                                            int arrLength = longsWithComma.length;
                                            boolean remOne = (total & 3) == 1;
                                            if (remOne) {
                                                --arrLength;
                                            }
                                            for (int i = 0; i < arrLength; ++i) {
                                                if (i + 1 < arrLength) {
                                                    int len = total > 8 ? 8 : total;
                                                    long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                    int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                    long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                    total -= 8;
                                                    ++i;
                                                } else {
                                                    int len = total > 4 ? 4 : total;
                                                    compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                    total -= 4;
                                                }
                                            }
                                            if (remOne) {
                                                compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                            }
                                        } else {
                                            compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                        }
                                        compactBodyBuilder.append("\t\t\t} else {\n");
                                        compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                        compactBodyBuilder.append("\t\t\t}\n");
                                    }
                                    compactBodyBuilder.append("\t\t}\n");
                                }
                                if (returnType == double.class) {
                                    compactBodyBuilder.append("\t\twriter.writeDouble(" + valueVar + ");\n");
                                } else if (returnType == float.class) {
                                    compactBodyBuilder.append("\t\twriter.writeFloat(" + valueVar + ");\n");
                                } else if (returnType == long.class) {
                                    compactBodyBuilder.append("\t\twriter.writeLong(" + valueVar + ");\n");
                                } else if (returnType == int.class || returnType == short.class || returnType == byte.class) {
                                    compactBodyBuilder.append("\t\twriter.writeInt(" + valueVar + ");\n");
                                } else {
                                    compactBodyBuilder.append("\t\twriter.writeJSONChar(" + valueVar + ");\n");
                                }
                            }
                        }
                        ensureNotEmptyFlag = true;
                    } else {
                        compactBodyBuilder.append("\t\tif(" + valueVar + " != null) {\n");
                        // field name
                        if (firstFlag) {
                            if (nameEqualUnderlineName) {
                                int total = fieldNameTokenLength - 1;
                                if (useUnsafe /*&& (total & 3) != 1*/) {
                                    int arrLength = longs.length;
                                    boolean remOne = (total & 3) == 1;
                                    if (remOne) {
                                        --arrLength;
                                    }
                                    for (int i = 0; i < arrLength; ++i) {
                                        if (i + 1 < arrLength) {
                                            int len = total > 8 ? 8 : total;
                                            long l1 = longs[i], l2 = longs[i + 1];
                                            int i1 = ints[i], i2 = ints[i + 1];
                                            long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                            compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                            total -= 8;
                                            ++i;
                                        } else {
                                            int len = total > 4 ? 4 : total;
                                            compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    }
                                    if (remOne) {
                                        compactBodyBuilder.append("\t\t\twriter.writeJSONToken(':');\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                }
                            } else {
                                compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                int total = fieldNameTokenLength - 1;
                                if (useUnsafe /*&& (total & 3) != 1*/) {
                                    int arrLength = longs.length;
                                    boolean remOne = (total & 3) == 1;
                                    if (remOne) {
                                        --arrLength;
                                    }
                                    for (int i = 0; i < arrLength; ++i) {
                                        if (i + 1 < arrLength) {
                                            int len = total > 8 ? 8 : total;
                                            long l1 = longs[i], l2 = longs[i + 1];
                                            int i1 = ints[i], i2 = ints[i + 1];
                                            long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                            compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                            total -= 8;
                                            ++i;
                                        } else {
                                            int len = total > 4 ? 4 : total;
                                            compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                            total -= 4;
                                        }
                                    }
                                    if (remOne) {
                                        compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                }
                                compactBodyBuilder.append("\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t}\n");
                            }
                            compactBodyBuilder.append("\t\t\tisEmptyFlag = false;\n");
                        } else {
                            if (ensureNotEmptyFlag) {
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longsWithComma.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longsWithComma.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                    compactBodyBuilder.append("\t\t\t}\n");
                                }
                            } else {
                                compactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                compactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength - 1;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longs.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longs[i], l2 = longs[i + 1];
                                                int i1 = ints[i], i2 = ints[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength - 1;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longs.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longs[i], l2 = longs[i + 1];
                                                int i1 = ints[i], i2 = ints[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\t\t\twriteMemory(writer, " + longs[i] + "L, " + ints[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                    compactBodyBuilder.append("\t\t\t\t}\n");
                                }
                                compactBodyBuilder.append("\t\t\t} else {\n");
                                if (nameEqualUnderlineName) {
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longsWithComma.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                } else {
                                    compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                    int total = fieldNameTokenLength;
                                    if (useUnsafe /*&& (total & 3) != 1*/) {
                                        int arrLength = longsWithComma.length;
                                        boolean remOne = (total & 3) == 1;
                                        if (remOne) {
                                            --arrLength;
                                        }
                                        for (int i = 0; i < arrLength; ++i) {
                                            if (i + 1 < arrLength) {
                                                int len = total > 8 ? 8 : total;
                                                long l1 = longsWithComma[i], l2 = longsWithComma[i + 1];
                                                int i1 = intsWithComma[i], i2 = intsWithComma[i + 1];
                                                long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                                                compactBodyBuilder.append("\t\t\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                                                total -= 8;
                                                ++i;
                                            } else {
                                                int len = total > 4 ? 4 : total;
                                                compactBodyBuilder.append("\t\t\t\t\twriteMemory(writer, " + longsWithComma[i] + "L, " + intsWithComma[i] + ", " + len + ");\n");
                                                total -= 4;
                                            }
                                        }
                                        if (remOne) {
                                            compactBodyBuilder.append("\t\t\t\t\twriter.writeJSONToken(':');\n");
                                        }
                                    } else {
                                        compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                    }
                                    compactBodyBuilder.append("\t\t\t\t} else {\n");
                                    compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                    compactBodyBuilder.append("\t\t\t\t}\n");
                                }
                                compactBodyBuilder.append("\t\t\t}\n");
                            }
                        }
                        boolean useSerializerInvokeFlag = false;
                        // field value
                        if (returnType == String.class) {
                            if (runtime) {
                                if (EnvUtils.JDK_9_PLUS) {
                                    compactBodyBuilder.append("\t\t\twriter.writeJSONStringBytes(" + valueVar + ", (byte[]) getStringValue(" + valueVar + "));\n");
                                } else {
                                    compactBodyBuilder.append("\t\t\twriter.writeJSONChars(getChars(" + valueVar + "));\n");
                                }
                            } else {
                                compactBodyBuilder.append("\t\t\twriter.writeJSONString(" + valueVar + ");\n");
                            }
                        } else if (returnType == Double.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeDouble(" + valueVar + ");\n");
                        } else if (returnType == Float.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeFloat(" + valueVar + ");\n");
                        } else if (returnType == Character.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeJSONChar(" + valueVar + ");\n");
                        } else if (returnType == Long.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeLong(" + valueVar + ");\n");
                        } else if (returnType == Integer.class || returnType == Short.class || returnType == Byte.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeInt(" + valueVar + ");\n");
                        } else if (returnType == BigDecimal.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeLatinString(" + valueVar + ".toString());\n");
                        } else if (returnType == BigInteger.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeBigInteger(" + valueVar + ");\n");
                        } else if (returnType == String[].class) {
                            compactBodyBuilder.append("\t\t\twriter.writeStringArray(" + valueVar + ");\n");
                        } else if (returnType == double[].class) {
                            compactBodyBuilder.append("\t\t\twriter.writeDoubleArray(" + valueVar + ");\n");
                        } else if (returnType == long[].class) {
                            compactBodyBuilder.append("\t\t\twriter.writeLongArray(" + valueVar + ");\n");
                        } else if (fieldSerializer.isStringCollection()) {
                            compactBodyBuilder.append("\t\t\twriter.writeStringCollection(" + valueVar + ");\n");
                        } else if (returnType == UUID.class) {
                            compactBodyBuilder.append("\t\t\twriter.writeUUID(" + valueVar + ");\n");
                        } else if (returnTypeName == "java.time.Instant") {
                            JsonProperty jsonProperty = fieldSerializer.getJsonProperty();
                            if (jsonProperty == null || (jsonProperty.pattern().length() == 0 && !jsonProperty.asTimestamp())) {
                                // use default pattern
                                compactBodyBuilder.append("\t\t\twriter.writeJSONInstant(" + valueVar + ".getEpochSecond(), " + valueVar + ".getNano());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.LocalTime") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                compactBodyBuilder.append("\t\t\twriter.writeJSONTimeWithNano(" + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.LocalDate") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                compactBodyBuilder.append("\t\t\twriter.writeJSONLocalDate(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth());\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.LocalDateTime") {
                            JsonProperty jsonProperty = fieldSerializer.getJsonProperty();
                            if (jsonProperty == null || (jsonProperty.pattern().length() == 0 && !jsonProperty.asTimestamp())) {
                                compactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), \"\");\n");
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else if (returnTypeName == "java.time.ZonedDateTime" || returnTypeName == "java.time.OffsetDateTime") {
                            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                                if (returnTypeName == "java.time.ZonedDateTime") {
                                    compactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getZone().getId());\n");
                                } else {
                                    compactBodyBuilder.append("\t\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getOffset().getId());\n");
                                }
                            } else {
                                useSerializerInvokeFlag = true;
                            }
                        } else {
                            useSerializerInvokeFlag = true;
                        }
                        if (useSerializerInvokeFlag) {
                            if (!appendFieldSerializersFlag) {
                                appendFieldSerializersFlag = true;
                                fieldSetBuilder.append("\t\tJSONPojoFieldSerializer[] fieldSerializerUseMethods = pojoStructure.getFieldSerializers(false);\n");
                            }
                            // use unsafe & field offset
                            fieldsDefinitionBuilder.append("\tfinal JSONPojoFieldSerializer ").append(fieldSerializerName).append(";\n");
                            fieldSetBuilder.append("\t\tthis.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseMethods[").append(fieldIndex).append("];\n");

                            compactBodyBuilder.append("\t\t\tdoSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", writer, jsonConfig, -1);\n");
                        }
                        compactBodyBuilder.append("\t\t}\n");
                    }
                } else {
                    if (!appendFieldSerializersFlag) {
                        appendFieldSerializersFlag = true;
                        fieldSetBuilder.append("\t\tJSONPojoFieldSerializer[] fieldSerializerUseMethods = pojoStructure.getFieldSerializers(false);\n");
                    }
                    // use unsafe & field offset
                    fieldsDefinitionBuilder.append("\tfinal JSONPojoFieldSerializer ").append(fieldSerializerName).append(";\n");
                    fieldSetBuilder.append("\t\tthis.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseMethods[").append(fieldIndex).append("];\n");

                    compactBodyBuilder.append("\t\tObject " + valueVar + " = invokeValue(" + fieldSerializerName + ", entity);\n");

                    // generate format code
                    if(Modifier.isPublic(returnType.getModifiers())) {
                        if(EnvUtils.JDK_7_BELOW) {
                            fmatOutBodyBuilder.append("\t\t" + returnType.getCanonicalName() + " " + valueVar + " = invokeValue(" + fieldSerializerName + ", entity, " + returnType.getCanonicalName() + ".class);\n");
                        } else {
                            fmatOutBodyBuilder.append("\t\t" + returnType.getCanonicalName() + " " + valueVar + " = (" + returnType.getCanonicalName() + ") invokeValue(" + fieldSerializerName + ", entity);\n");
                        }
                    } else {
                        fmatOutBodyBuilder.append("\t\tObject " + valueVar + " = invokeValue(" + fieldSerializerName + ", entity);\n");
                    }
                    generateSerializeFieldFormatOutCode(ensureNotEmptyFlag, primitive, fieldSerializerName, fieldKey, valueVar, nameEqualUnderlineName, underlineName, useUnsafe, longsFormatOut, intsFormatOut, fieldNameFormatOutTokenLength, returnType, fieldSerializer, runtime, fmatOutBodyBuilder);

                    compactBodyBuilder.append("\t\tif(" + valueVar + " != null) {\n");
                    if (firstFlag) {
                        if (nameEqualUnderlineName) {
                            compactBodyBuilder.append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                        } else {
                            compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                            compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                            compactBodyBuilder.append("\t\t\t} else {\n");
                            compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                            compactBodyBuilder.append("\t\t\t}\n");
                        }
                        compactBodyBuilder.append("\t\t\tisEmptyFlag = false;\n");
                    } else {
                        if (ensureNotEmptyFlag) {
                            if (nameEqualUnderlineName) {
                                compactBodyBuilder.append("\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                            } else {
                                compactBodyBuilder.append("\t\t\tif (unCamelCaseToUnderline) {\n");
                                compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t}\n");
                            }
                        } else {
                            if (nameEqualUnderlineName) {
                                compactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                compactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");
                                compactBodyBuilder.append("\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t}\n");
                            } else {
                                compactBodyBuilder.append("\t\t\tif(isEmptyFlag) {\n");
                                compactBodyBuilder.append("\t\t\t\tisEmptyFlag = false;\n");

                                compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t\t}\n");

                                compactBodyBuilder.append("\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\tif (unCamelCaseToUnderline) {\n");
                                compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + fieldKey + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t\t} else {\n");
                                compactBodyBuilder.append("\t\t\t\t\twriter.write(\",\\\"" + underlineName + "\\\":\");\n");
                                compactBodyBuilder.append("\t\t\t\t}\n");
                                compactBodyBuilder.append("\t\t\t}\n");
                            }
                        }
                    }
                    compactBodyBuilder.append("\t\t\tdoSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", writer, jsonConfig, -1);\n");
                    compactBodyBuilder.append("\t\t}\n");

                    if(primitive) {
                        ensureNotEmptyFlag = true;
                    }
                }

                compactBodyBuilder.append("\n");
                fmatOutBodyBuilder.append("\n");
                fieldIndex++;
            }
            if (ensureNotEmptyFlag) {
                fmatOutBodyBuilder.append("\t\twriteFormatOutSymbols(writer, indentLevel, jsonConfig);\n");
            } else {
                fmatOutBodyBuilder.append("\t\tif (!isEmptyFlag) {\n");
                fmatOutBodyBuilder.append("\t\t\twriteFormatOutSymbols(writer, indentLevel, jsonConfig);\n");
                fmatOutBodyBuilder.append("\t\t}\n");
            }
        }

        if (appendUnCamelCaseToUnderlineFlag) {
            compactHeaderBuilder.append("\t\tboolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();\n\n");
            fmatOutHeaderBuilder.append("\t\tboolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();\n\n");
        } else {
            compactHeaderBuilder.append("\n");
            fmatOutHeaderBuilder.append("\n");
        }
        if (!runtime) {
//            codeBuilder.append("\tpublic ").append(genClassName).append("() {\n");
//            codeBuilder.append("\t\tthis(JSONPojoStructure.get(" + canonicalName + ".class));\n");
//            codeBuilder.append("\t}\n\n");
        }

        // create fun
        codeBuilder.append("\tprotected ").append(genClassName).append("(JSONPojoStructure pojoStructure) {\n");
        codeBuilder.append("\t\tsuper(pojoStructure);\n");
        codeBuilder.append(fieldSetBuilder);
        codeBuilder.append("\t}\n");

        // fieldset
        codeBuilder.append(fieldsDefinitionBuilder).append("\n");

        // compact method code
        codeBuilder.append("\tpublic void serializePojoCompact(").append(canonicalName).append(" entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {\n\n");
        codeBuilder.append(compactHeaderBuilder);
        codeBuilder.append(compactBodyBuilder);
        codeBuilder.append("\t}\n\n");

        // formatOut method code
        codeBuilder.append("\tpublic void serializePojoFormatOut(").append(canonicalName).append(" entity, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {\n\n");
        codeBuilder.append(fmatOutHeaderBuilder);
        codeBuilder.append(fmatOutBodyBuilder);
        codeBuilder.append("\t}\n");

        codeBuilder.append("}\n");
        codeBuilder.append("\n");

        String code = codeBuilder.toString();
        if (printSource) {
            System.out.println(code);
        }

        return new JavaSourceObject(packageName, genClassName, code);
    }

    private static void generateSerializeFieldFormatOutCode(boolean ensureNotEmptyFlag, boolean primitive, String fieldSerializerName, String fieldKey, String valueVar, boolean nameEqualUnderlineName, String underlineName, boolean useUnsafe, long[] longsFormatOut, int[] intsFormatOut, int fieldNameFormatOutTokenLength, Class<?> returnType, JSONPojoFieldSerializer fieldSerializer, boolean runtime, StringBuilder fmatOutBodyBuilder) {
        boolean checkNullFlag = !primitive;
        String tabFlag = checkNullFlag ? "\t" : "";
        if (checkNullFlag) {
            fmatOutBodyBuilder.append("\t\tif(" + valueVar + " != null) {\n");
        }
        // generate indent symbols
        if (ensureNotEmptyFlag) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONToken(',');\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteFormatOutSymbols(writer, indentPlus, jsonConfig);\n");
        } else {
            fmatOutBodyBuilder.append(tabFlag).append("\t\tif(isEmptyFlag) {\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t\tisEmptyFlag = false;\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t} else {\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriter.writeJSONToken(',');\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t} \n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteFormatOutSymbols(writer, indentPlus, jsonConfig);\n");
        }
        // generate \"${field}\":
        if (nameEqualUnderlineName) {
            if (useUnsafe) {
                int totalf = fieldNameFormatOutTokenLength;
                int arrLengthf = longsFormatOut.length;
                boolean remOnef = (totalf & 3) == 1;
                if (remOnef) {
                    --arrLengthf;
                }
                for (int i = 0; i < arrLengthf; ++i) {
                    if (i + 1 < arrLengthf) {
                        int len = totalf > 8 ? 8 : totalf;
                        long l1 = longsFormatOut[i], l2 = longsFormatOut[i + 1];
                        int i1 = intsFormatOut[i], i2 = intsFormatOut[i + 1];
                        long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                        fmatOutBodyBuilder.append(tabFlag).append("\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                        totalf -= 8;
                        ++i;
                    } else {
                        int len = totalf > 4 ? 4 : totalf;
                        fmatOutBodyBuilder.append(tabFlag).append("\t\twriteMemory(writer, " + longsFormatOut[i] + "L, " + intsFormatOut[i] + ", " + len + ");\n");
                        totalf -= 4;
                    }
                }
                if (remOnef) {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONToken(':');\n");
                }
            } else {
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
            }
        } else {
            fmatOutBodyBuilder.append(tabFlag).append("\t\tif(unCamelCaseToUnderline) {\n");
            if (useUnsafe) {
                int totalf = fieldNameFormatOutTokenLength;
                int arrLengthf = longsFormatOut.length;
                boolean remOnef = (totalf & 3) == 1;
                if (remOnef) {
                    --arrLengthf;
                }
                for (int i = 0; i < arrLengthf; ++i) {
                    if (i + 1 < arrLengthf) {
                        int len = totalf > 8 ? 8 : totalf;
                        long l1 = longsFormatOut[i], l2 = longsFormatOut[i + 1];
                        int i1 = intsFormatOut[i], i2 = intsFormatOut[i + 1];
                        long l3 = EnvUtils.BIG_ENDIAN ? ((long) i1) << 32 | i2 : ((long) i2) << 32 | i1;
                        fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriteMemory(writer, " + l1 + "L, " + l2 + "L, " + l3 + "L, " + len + ");\n");
                        totalf -= 8;
                        ++i;
                    } else {
                        int len = totalf > 4 ? 4 : totalf;
                        fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriteMemory(writer, " + longsFormatOut[i] + "L, " + intsFormatOut[i] + ", " + len + ");\n");
                        totalf -= 4;
                    }
                }
                if (remOnef) {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriter.writeJSONToken(':');\n");
                }
            } else {
                fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriter.write(\"\\\"" + fieldKey + "\\\":\");\n");
            }
            fmatOutBodyBuilder.append(tabFlag).append("\t\t} else {\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriter.write(\"\\\"" + underlineName + "\\\":\");\n");
            fmatOutBodyBuilder.append(tabFlag).append("\t\t}\n");
        }

        fmatOutBodyBuilder.append(tabFlag).append("\t\tif(formatOutColonSpace) {\n");
        fmatOutBodyBuilder.append(tabFlag).append("\t\t\twriter.writeJSONToken(' ');\n");
        fmatOutBodyBuilder.append(tabFlag).append("\t\t}\n");

        boolean useSerializerInvokeFlag = false;
        String returnTypeName = returnType.getName().intern();
        // field value
        if (returnType == String.class) {
            if (runtime) {
                if (EnvUtils.JDK_9_PLUS) {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONStringBytes(" + valueVar + ", (byte[]) getStringValue(" + valueVar + "));\n");
                } else {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONChars(getChars(" + valueVar + "));\n");
                }
            } else {
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONString(" + valueVar + ");\n");
            }
        } else if (returnType == Double.class || returnType == double.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeDouble(" + valueVar + ");\n");
        } else if (returnType == Float.class || returnType == float.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeFloat(" + valueVar + ");\n");
        } else if (returnType == Character.class || returnType == char.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONChar(" + valueVar + ");\n");
        } else if (returnType == Long.class || returnType == long.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeLong(" + valueVar + ");\n");
        } else if (returnType == Integer.class || returnType == Short.class || returnType == Byte.class || returnType == int.class || returnType == short.class || returnType == byte.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeInt(" + valueVar + ");\n");
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.write(String.valueOf(" + valueVar + "));\n");
        } else if (returnType == BigDecimal.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeLatinString(" + valueVar + ".toString());\n");
        } else if (returnType == BigInteger.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeBigInteger(" + valueVar + ");\n");
        } else if (returnType == String[].class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteStringArrayFormatOut(writer, " + valueVar + ", jsonConfig, indentPlus);\n");
        } else if (returnType == double[].class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteDoubleArrayFormatOut(writer, " + valueVar + ", jsonConfig, indentPlus);\n");
        } else if (returnType == long[].class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteLongArrayFormatOut(writer, " + valueVar + ", jsonConfig, indentPlus);\n");
        } else if (fieldSerializer.isStringCollection()) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriteStringCollectionFormatOut(writer, " + valueVar + ", jsonConfig, indentPlus);\n");
        } else if (returnType == UUID.class) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeUUID(" + valueVar + ");\n");
        } else if (returnTypeName == "java.time.Instant") {
            JsonProperty jsonProperty = fieldSerializer.getJsonProperty();
            if (jsonProperty == null || (jsonProperty.pattern().length() == 0 && !jsonProperty.asTimestamp())) {
                // use default pattern
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONInstant(" + valueVar + ".getEpochSecond(), " + valueVar + ".getNano());\n");
            } else {
                useSerializerInvokeFlag = true;
            }
        } else if (returnTypeName == "java.time.LocalTime") {
            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONTimeWithNano(" + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano());\n");
            } else {
                useSerializerInvokeFlag = true;
            }
        } else if (returnTypeName == "java.time.LocalDate") {
            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONLocalDate(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth());\n");
            } else {
                useSerializerInvokeFlag = true;
            }
        } else if (returnTypeName == "java.time.LocalDateTime") {
            JsonProperty jsonProperty = fieldSerializer.getJsonProperty();
            if (jsonProperty == null || (jsonProperty.pattern().length() == 0 && !jsonProperty.asTimestamp())) {
                fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), \"\");\n");
            } else {
                useSerializerInvokeFlag = true;
            }
        } else if (returnTypeName == "java.time.ZonedDateTime" || returnTypeName == "java.time.OffsetDateTime") {
            if (fieldSerializer.getJsonProperty() == null || fieldSerializer.getJsonProperty().pattern().length() == 0) {
                if (returnTypeName == "java.time.ZonedDateTime") {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getZone().getId());\n");
                } else {
                    fmatOutBodyBuilder.append(tabFlag).append("\t\twriter.writeJSONLocalDateTime(" + valueVar + ".getYear(), " + valueVar + ".getMonthValue(), " + valueVar + ".getDayOfMonth(), " + valueVar + ".getHour(), " + valueVar + ".getMinute(), " + valueVar + ".getSecond(), " + valueVar + ".getNano(), " + valueVar + ".getOffset().getId());\n");
                }
            } else {
                useSerializerInvokeFlag = true;
            }
        } else {
            useSerializerInvokeFlag = true;
        }
        if (useSerializerInvokeFlag) {
            fmatOutBodyBuilder.append(tabFlag).append("\t\tdoSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", writer, jsonConfig, indentPlus);\n");
        }
        if (checkNullFlag) {
            fmatOutBodyBuilder.append("\t\t}\n");
        }
    }
}

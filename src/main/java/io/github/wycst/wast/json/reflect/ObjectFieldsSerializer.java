//package io.github.wycst.wast.json.reflect;
//
//import io.github.wycst.wast.common.reflect.GetterInfo;
//import io.github.wycst.wast.json.JSONTypeSerializer;
//import io.github.wycst.wast.json.options.JsonConfig;
//
//import java.io.IOException;
//import java.io.Writer;
//
///**
// * 线性序列化实现
// * object fields.length <= 20
// *
// * @Author wangyunchao
// * @Date 2023/5/23 22:50
// */
//public class ObjectFieldsSerializer extends JSONTypeSerializer {
//
//    private final FieldSerializer fieldSerializer1;
//    private final FieldSerializer fieldSerializer2;
//    private final FieldSerializer fieldSerializer3;
//    private final FieldSerializer fieldSerializer4;
//    private final FieldSerializer fieldSerializer5;
//    private final FieldSerializer fieldSerializer6;
//    private final FieldSerializer fieldSerializer7;
//    private final FieldSerializer fieldSerializer8;
//    private final FieldSerializer fieldSerializer9;
//    private final FieldSerializer fieldSerializer10;
//    private final FieldSerializer fieldSerializer11;
//    private final FieldSerializer fieldSerializer12;
//    private final FieldSerializer fieldSerializer13;
//    private final FieldSerializer fieldSerializer14;
//    private final FieldSerializer fieldSerializer15;
//    private final FieldSerializer fieldSerializer16;
//    private final FieldSerializer fieldSerializer17;
//    private final FieldSerializer fieldSerializer18;
//    private final FieldSerializer fieldSerializer19;
//    private final FieldSerializer fieldSerializer20;
//
//    ObjectFieldsSerializer(FieldSerializer[] fieldSerializers) {
//        int len = fieldSerializers.length;
//        fieldSerializer1 = len > 0 ? fieldSerializers[0] : null;
//        fieldSerializer2 = len > 1 ? fieldSerializers[1] : null;
//        fieldSerializer3 = len > 2 ? fieldSerializers[2] : null;
//        fieldSerializer4 = len > 3 ? fieldSerializers[3] : null;
//        fieldSerializer5 = len > 4 ? fieldSerializers[4] : null;
//        fieldSerializer6 = len > 5 ? fieldSerializers[5] : null;
//        fieldSerializer7 = len > 6 ? fieldSerializers[6] : null;
//        fieldSerializer8 = len > 7 ? fieldSerializers[7] : null;
//        fieldSerializer9 = len > 8 ? fieldSerializers[8] : null;
//        fieldSerializer10 = len > 9 ? fieldSerializers[9] : null;
//        fieldSerializer11 = len > 10 ? fieldSerializers[10] : null;
//        fieldSerializer12 = len > 11 ? fieldSerializers[11] : null;
//        fieldSerializer13 = len > 12 ? fieldSerializers[12] : null;
//        fieldSerializer14 = len > 13 ? fieldSerializers[13] : null;
//        fieldSerializer15 = len > 14 ? fieldSerializers[14] : null;
//        fieldSerializer16 = len > 15 ? fieldSerializers[15] : null;
//        fieldSerializer17 = len > 16 ? fieldSerializers[16] : null;
//        fieldSerializer18 = len > 17 ? fieldSerializers[17] : null;
//        fieldSerializer19 = len > 18 ? fieldSerializers[18] : null;
//        fieldSerializer20 = len > 19 ? fieldSerializers[19] : null;
//    }
//
//    /**
//     * 序列化
//     *
//     * @param obj
//     * @param content
//     * @param jsonConfig
//     * @param indentLevel
//     * @throws Exception
//     */
//    public void serialize(Object obj, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {
//        boolean formatOut = jsonConfig.isFormatOut();
//        try {
//            boolean writeClassName = jsonConfig.isWriteClassName();
//            Class clazz = obj.getClass();
//            int cnt = checkWriteClassName(writeClassName, content, clazz, false, indentLevel) ? 1 : 0;
//            cnt += handleFieldSerialize(fieldSerializer1, obj, content, jsonConfig, indentLevel, cnt);
//            if(fieldSerializer2 != null) {
//                cnt += handleFieldSerialize(fieldSerializer2, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer3 != null) {
//                cnt += handleFieldSerialize(fieldSerializer3, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer4 != null) {
//                cnt += handleFieldSerialize(fieldSerializer4, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer5 != null) {
//                cnt += handleFieldSerialize(fieldSerializer5, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer6 != null) {
//                cnt += handleFieldSerialize(fieldSerializer6, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer7 != null) {
//                cnt += handleFieldSerialize(fieldSerializer7, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer8 != null) {
//                cnt += handleFieldSerialize(fieldSerializer8, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer9 != null) {
//                cnt += handleFieldSerialize(fieldSerializer9, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer10 != null) {
//                cnt += handleFieldSerialize(fieldSerializer10, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer11 != null) {
//                cnt += handleFieldSerialize(fieldSerializer11, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer12 != null) {
//                cnt += handleFieldSerialize(fieldSerializer12, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer13 != null) {
//                cnt += handleFieldSerialize(fieldSerializer13, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer14 != null) {
//                cnt += handleFieldSerialize(fieldSerializer14, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer15 != null) {
//                cnt += handleFieldSerialize(fieldSerializer15, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer16 != null) {
//                cnt += handleFieldSerialize(fieldSerializer16, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer17 != null) {
//                cnt += handleFieldSerialize(fieldSerializer17, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer18 != null) {
//                cnt += handleFieldSerialize(fieldSerializer18, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer19 != null) {
//                cnt += handleFieldSerialize(fieldSerializer19, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//            if(fieldSerializer20 != null) {
//                handleFieldSerialize(fieldSerializer20, obj, content, jsonConfig, indentLevel, cnt);
//            } else {
//                return;
//            }
//        } finally {
//            writeFormatSymbolOut(content, indentLevel, formatOut);
//        }
//    }
//
//    private int handleFieldSerialize(FieldSerializer fieldSerializer, Object obj, Writer content, JsonConfig jsonConfig, int indentLevel, int cnt) throws Exception {
//        boolean writeFullProperty = jsonConfig.isFullProperty();
//        boolean formatOut = jsonConfig.isFormatOut();
//        boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();
//        boolean camelCaseToUnderline = jsonConfig.isCamelCaseToUnderline();
//
//        GetterInfo getterInfo = fieldSerializer.getGetterInfo();
//        if (!getterInfo.existField() && skipGetterOfNoExistField) {
//            return 0;
//        }
//
//        Object value = getterInfo.invoke(obj);
//        if (value == null && !writeFullProperty) {
//            return 0;
//        }
//
//        if (cnt > 0) {
//            content.append(",");
//        }
//
//        char[] quotBuffers = fieldSerializer.getFixedFieldName();
//        writeFormatSymbolOut(content, indentLevel + 1, formatOut);
//        if (value == null) {
//            if (camelCaseToUnderline) {
//                content.append('"').append(getterInfo.getUnderlineName()).append("\":null");
//            } else {
//                content.write(quotBuffers, 1, quotBuffers.length - 2);
//            }
//        } else {
//            if (camelCaseToUnderline) {
//                content.append('"').append(getterInfo.getUnderlineName()).append("\":");
//            } else {
//                content.write(quotBuffers, 1, quotBuffers.length - 6);
//            }
//            // Custom serialization
//            JSONTypeSerializer serializer = fieldSerializer.getSerializer();
//            doSerialize(serializer, value, content, jsonConfig, formatOut ? indentLevel + 1 : -1);
//        }
//
//        return 1;
//    }
//}

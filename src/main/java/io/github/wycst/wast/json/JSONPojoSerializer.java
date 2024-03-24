package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.compiler.JavaSourceObject;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;

/**
 * Still under testing
 *
 * @Date 2024/3/15 8:59
 * @Created by wangyc
 */
public class JSONPojoSerializer<T> extends JSONTypeSerializer {

    protected final JSONPojoStructure pojoStructureWrapper;
    protected final Class<?> pojoClass;

    protected JSONPojoSerializer(Class<?> pojoClass) {
        this.pojoClass = pojoClass;
        pojoStructureWrapper = JSONPojoStructure.get(pojoClass);
        init();
    }

    protected JSONPojoSerializer(JSONPojoStructure pojoStructureWrapper) {
        this.pojoStructureWrapper = pojoStructureWrapper;
        this.pojoClass = pojoStructureWrapper.getSourceClass();
        init();
    }

    public void init() {
    }

    public void serializePojoBody(T entity, JSONWriter content, JSONConfig jsonConfig, int indentLevel) throws Exception {
        Class<?> entityClass = entity.getClass();
        if (entityClass != pojoClass) {
            JSONPojoSerializer jsonPojoSerializer = (JSONPojoSerializer) getTypeSerializer(entityClass);
            jsonPojoSerializer.serializePojoBody(entity, content, jsonConfig, indentLevel);
        } else {
            boolean writeFullProperty = jsonConfig.isFullProperty();
            boolean formatOut = jsonConfig.isFormatOut();
            boolean writeClassName = jsonConfig.isWriteClassName();

            boolean isFirstKey = !checkWriteClassName(writeClassName, content, entityClass, formatOut, indentLevel, jsonConfig);
            JSONPojoStructure pojoStructureWrapper = getObjectStructureWrapper(entityClass);
            JSONPojoFieldSerializer[] fieldSerializers = pojoStructureWrapper.getFieldSerializers(jsonConfig.isUseFields());

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
                if (isFirstKey) {
                    isFirstKey = false;
                } else {
                    content.write(',');
                }
                writeFormatOutSymbols(content, indentPlus, formatOut, jsonConfig);
                if (value != null) {
                    if (unCamelCaseToUnderline) {
                        fieldSerializer.writeFieldNameToken(content, 0, 4);
                    } else {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":");
                    }
                    // Custom serialization
                    JSONTypeSerializer serializer = fieldSerializer.getSerializer();
                    serializer.serialize(value, content, jsonConfig, formatOut ? indentPlus : -1);
                } else {
                    if (unCamelCaseToUnderline) {
                        fieldSerializer.writeFieldNameToken(content, 0, 0);
                    } else {
                        content.append('"').append(getterInfo.getUnderlineName()).append("\":null");
                    }
                }
            }
            if (!isFirstKey) {
                writeFormatOutSymbols(content, indentLevel, formatOut, jsonConfig);
            }
        }
    }

    JSONPojoStructure getObjectStructureWrapper(Class clazz) {
        return clazz == pojoClass ? pojoStructureWrapper : JSONPojoStructure.get(clazz);
    }

    @Override
    protected final void serialize(Object obj, JSONWriter writer, JSONConfig jsonConfig, int indentLevel) throws Exception {
        int hashcode = -1;
        if (jsonConfig.isSkipCircularReference()) {
            if (jsonConfig.getStatus(hashcode = System.identityHashCode(obj)) == 0) {
                writer.write(NULL);
                return;
            }
            jsonConfig.setStatus(hashcode, 0);
        }
        writer.write('{');
        serializePojoBody((T) obj, writer, jsonConfig, indentLevel);
        writer.write('}');
        jsonConfig.setStatus(hashcode, -1);
    }

    protected final void doSerialize(JSONTypeSerializer serializer, Object fieldValue, JSONWriter content, JSONConfig jsonConfig, int indentLevel) throws Exception {
        serializer.serialize(fieldValue, content, jsonConfig, indentLevel);
    }

    /**
     * Generate serialized Java source code based on the pojo class
     * Using Java compilation can improve performance by approximately only 8%
     *
     * @param pojoClass
     * @return
     */
    public static JavaSourceObject generateJavaCodeSource(Class<?> pojoClass) {
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(pojoClass);
        if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
            throw new UnsupportedOperationException(pojoClass + " is not a pojo class");
        }
        JSONPojoStructure structureWrapper = JSONPojoStructure.get(pojoClass);
        boolean classPrivate = structureWrapper.isPrivate();
        if (classPrivate) {
            throw new UnsupportedOperationException(pojoClass + " is not supported for private access ");
        }
        boolean forceUseFields = structureWrapper.isForceUseFields();

        String simpleName = pojoClass.getSimpleName();
        String canonicalName = pojoClass.getCanonicalName();
        String genClassName = "JSONPojoSerializer_" + simpleName + "_" + System.identityHashCode(pojoClass);
        String packageName = pojoClass.getPackage().getName();
        StringBuilder codeBuilder = new StringBuilder("package ");
        codeBuilder.append(packageName);
        codeBuilder.append(";\r\n\r\n");
        codeBuilder.append("import io.github.wycst.wast.common.reflect.GetterInfo;\n");
        codeBuilder.append("import io.github.wycst.wast.common.reflect.ReflectConsts;\n");
        codeBuilder.append("import io.github.wycst.wast.json.options.JsonConfig;\n");
        codeBuilder.append("import io.github.wycst.wast.json.reflect.FieldSerializer;\n");
        codeBuilder.append("import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;\n\n");
        codeBuilder.append("import java.io.Writer;\n\n");
        codeBuilder.append("/**\n");
        codeBuilder.append(" * pojo serializer \n");
        codeBuilder.append(" * @Date " + new GregorianDate() + "\n");
        codeBuilder.append(" * @Created by code generator\n");
        codeBuilder.append(" */\n");
        codeBuilder.append("public class ").append(genClassName).append(" extends io.github.wycst.wast.json.JSONPojoSerializer<").append(canonicalName).append("> {\r\n");

        // fieldset
        StringBuilder fieldsDefinitionBuilder = new StringBuilder();
        StringBuilder fieldSetBuilder = new StringBuilder();
        StringBuilder serializePojoBodyBuilder = new StringBuilder();

        serializePojoBodyBuilder.append(
                "        boolean writeFullProperty = jsonConfig.isFullProperty();\n" +
                        "        boolean formatOut = jsonConfig.isFormatOut();\n" +
                        "        boolean writeClassName = jsonConfig.isWriteClassName();\n" +
                        "        boolean isFirstKey = !checkWriteClassName(writeClassName, content, pojoClass, formatOut, indentLevel, jsonConfig);\n" +
                        "        boolean unCamelCaseToUnderline = !jsonConfig.isCamelCaseToUnderline();\n" +
                        "        int indentPlus = indentLevel + 1;\n");

        JSONPojoFieldSerializer[] fieldSerializerUseFields = structureWrapper.getFieldSerializers(true);
        JSONPojoFieldSerializer[] fieldSerializerUseMethods = structureWrapper.getFieldSerializers(false);

        serializePojoBodyBuilder.append("        GetterInfo getterInfo;\n");
        serializePojoBodyBuilder.append("        Object value;\n");
        if (!forceUseFields) {
            serializePojoBodyBuilder.append("        boolean skipGetterOfNoExistField = jsonConfig.isSkipGetterOfNoneField();\n\n");
        }
        if (fieldSerializerUseFields.length > 0) {
            fieldSetBuilder.append("        FieldSerializer[] fieldSerializerUseFields = pojoStructureWrapper.getFieldSerializers(true);\n");
            int fieldIndex = 0;
            for (JSONPojoFieldSerializer fieldSerializer : fieldSerializerUseFields) {
                GetterInfo getterInfo = fieldSerializer.getGetterInfo();
                String name = getterInfo.getName();
                String fieldSerializerName = name + "Serializer";
                fieldsDefinitionBuilder.append("    final FieldSerializer ").append(fieldSerializerName).append(";\n");
                fieldSetBuilder.append("        this.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseFields[").append(fieldIndex++).append("];\n");

                if (forceUseFields) {
                    serializePojoBodyBuilder.append(
                            "        getterInfo = " + fieldSerializerName + ".getGetterInfo();\n" +
                                    "        value = getterInfo.invoke(entity);\n" +
                                    "        if (value != null || writeFullProperty) {\n" +
                                    "            if (isFirstKey) {\n" +
                                    "                isFirstKey = false;\n" +
                                    "            } else {\n" +
                                    "                content.write(',');\n" +
                                    "            }\n" +
                                    "            writeFormatOutSymbols(content, indentPlus, formatOut, jsonConfig);\n" +
                                    "            if (value != null) {\n" +
                                    "                if (unCamelCaseToUnderline) {\n" +
                                    "                    " + fieldSerializerName + ".writeFieldNameToken(content, 0, 4);\n" +
                                    "                } else {\n" +
                                    "                    content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":\");\n" +
                                    "                }\n" +
                                    "                // Custom serialization\n" +
                                    "                doSerialize(" + fieldSerializerName + ".getSerializer(), value, content, jsonConfig, formatOut ? indentPlus : -1);\n" +
                                    "            } else {\n" +
                                    "                if (unCamelCaseToUnderline) {\n" +
                                    "                    " + fieldSerializerName + ".writeFieldNameToken(content, 0, 0);\n" +
                                    "                } else {\n" +
                                    "                    content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":null\");\n" +
                                    "                }\n" +
                                    "            }\n" +
                                    "        }\n\n"
                    );
                }
            }
        }

        if (!forceUseFields) {
            fieldsDefinitionBuilder.append("\n");
            fieldSetBuilder.append("        FieldSerializer[] fieldSerializerUseMethods = pojoStructureWrapper.getFieldSerializers(false);\n");
            if (fieldSerializerUseMethods.length > 0) {
                int fieldIndex = 0;
                for (JSONPojoFieldSerializer fieldSerializer : fieldSerializerUseMethods) {
                    GetterInfo getterInfo = fieldSerializer.getGetterInfo();
                    String name = getterInfo.getName();
                    String fieldSerializerName = name + "UseMethodSerializer";
                    fieldsDefinitionBuilder.append("    final FieldSerializer ").append(fieldSerializerName).append(";\n");
                    fieldSetBuilder.append("        this.").append(fieldSerializerName).append(" = ").append("fieldSerializerUseMethods[").append(fieldIndex++).append("];\n");

                    if (!getterInfo.isPrivate()) {
                        String valueVar = getterInfo.getName();
                        // use getter
                        serializePojoBodyBuilder.append(
                                "        getterInfo = " + fieldSerializerName + ".getGetterInfo();\n" +
                                        "        if (getterInfo.existField() || !skipGetterOfNoExistField) {\n" +
                                        "           Object " + valueVar + " = entity." + getterInfo.getMethodName() + "();\n" +
                                        "           if (" + valueVar + " != null || writeFullProperty) {\n" +
                                        "               if (isFirstKey) {\n" +
                                        "                   isFirstKey = false;\n" +
                                        "               } else {\n" +
                                        "                   content.write(',');\n" +
                                        "               }\n" +
                                        "               writeFormatOutSymbols(content, indentPlus, formatOut, jsonConfig);\n" +
                                        "               if (" + valueVar + " != null) {\n" +
                                        "                   if (unCamelCaseToUnderline) {\n" +
                                        "                       content.write(\"\\\"" + valueVar + "\\\":\");\n" +
                                        "                   } else {\n" +
                                        "                       content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":\");\n" +
                                        "                   }\n" +
                                        "                   // Custom serialization\n" +
                                        "                   doSerialize(" + fieldSerializerName + ".getSerializer(), " + valueVar + ", content, jsonConfig, formatOut ? indentPlus : -1);\n" +
                                        "               } else {\n" +
                                        "                   if (unCamelCaseToUnderline) {\n" +
                                        "                       " + fieldSerializerName + ".writeFieldNameToken(content, 0, 0);\n" +
                                        "                   } else {\n" +
                                        "                       content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":null\");\n" +
                                        "                   }\n" +
                                        "               }\n" +
                                        "           }\n" +
                                        "        }\n\n"
                        );
                    } else {
                        // if private then use unsafe
                        serializePojoBodyBuilder.append(
                                "        getterInfo = " + fieldSerializerName + ".getGetterInfo();\n" +
                                        "        value = getterInfo.invoke(entity);\n" +
                                        "        if (getterInfo.existField() || !skipGetterOfNoExistField) {\n" +
                                        "           if (value != null || writeFullProperty) {\n" +
                                        "               if (isFirstKey) {\n" +
                                        "                   isFirstKey = false;\n" +
                                        "               } else {\n" +
                                        "                   content.write(',');\n" +
                                        "               }\n" +
                                        "               writeFormatOutSymbols(content, indentPlus, formatOut, jsonConfig);\n" +
                                        "               if (value != null) {\n" +
                                        "                   if (unCamelCaseToUnderline) {\n" +
                                        "                       " + fieldSerializerName + ".writeFieldNameToken(content, 0, 4);\n" +
                                        "                   } else {\n" +
                                        "                       content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":\");\n" +
                                        "                   }\n" +
                                        "                   // Custom serialization\n" +
                                        "                   doSerialize(" + fieldSerializerName + ".getSerializer(), value, content, jsonConfig, formatOut ? indentPlus : -1);\n" +
                                        "               } else {\n" +
                                        "                   if (unCamelCaseToUnderline) {\n" +
                                        "                       " + fieldSerializerName + ".writeFieldNameToken(content, 0, 0);\n" +
                                        "                   } else {\n" +
                                        "                       content.append('\"').append(getterInfo.getUnderlineName()).append(\"\\\":null\");\n" +
                                        "                   }\n" +
                                        "               }\n" +
                                        "           }\n\n" +
                                        "        }"
                        );
                    }
                }
            }
        }

        serializePojoBodyBuilder.append(
                "        if (!isFirstKey) {\n" +
                        "            writeFormatOutSymbols(content, indentLevel, formatOut, jsonConfig);\n" +
                        "        }\n");

        // create fun
        codeBuilder.append("    protected ").append(genClassName).append("(ObjectStructureWrapper pojoStructureWrapper) {\r\n");
        codeBuilder.append("        super(pojoStructureWrapper);\r\n");
        codeBuilder.append(fieldSetBuilder);
        codeBuilder.append("    }\n\n");
        codeBuilder.append(fieldsDefinitionBuilder).append("\n");

        codeBuilder.append("    public void serializePojoBody(").append(canonicalName).append(" entity, Writer content, JsonConfig jsonConfig, int indentLevel) throws Exception {\r\n");
        codeBuilder.append(serializePojoBodyBuilder);
        codeBuilder.append("    }\r\n");


        codeBuilder.append("}");

        return new JavaSourceObject(packageName, genClassName, codeBuilder.toString());
    }
}

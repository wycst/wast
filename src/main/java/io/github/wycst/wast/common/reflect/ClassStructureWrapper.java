package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.annotation.MethodInvokePriority;
import io.github.wycst.wast.common.exceptions.InvokeReflectException;
import io.github.wycst.wast.common.tools.FNV;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class序列化和反序列化结构包装
 *
 * @author wangy
 */
public final class ClassStructureWrapper {

    // cache
    private static Map<Class<?>, ClassStructureWrapper> classStructureWarppers = new ConcurrentHashMap<Class<?>, ClassStructureWrapper>();
    private static final Map<Class<?>, List> COMPATIBLE_TYPES = new HashMap<Class<?>, List>();
    // 内置类默认使用field序列化，可维护名称列表控制使用getter method
    private static String[] USE_GETTER_METHOD_TYPE_NAME_LIST = {
    };

    // 内置类默认使用field序列化，可维护超类列表控制使用getter method
    private static Class[] USE_GETTER_METHOD_TYPE_LIST = {
            Throwable.class,
            Error.class
    };

    static {
        COMPATIBLE_TYPES.put(double.class, Arrays.asList(Double.class, long.class, Long.class, Float.class, float.class, Integer.class, int.class, Short.class, short.class, byte.class, Byte.class));
        COMPATIBLE_TYPES.put(float.class, Arrays.asList(long.class, Long.class, Float.class, Integer.class, int.class, Short.class, short.class, byte.class, Byte.class));
        COMPATIBLE_TYPES.put(long.class, Arrays.asList(Long.class, Integer.class, int.class, Short.class, short.class, byte.class, Byte.class));
        COMPATIBLE_TYPES.put(int.class, Arrays.asList(Integer.class, Short.class, short.class, byte.class, Byte.class));
        COMPATIBLE_TYPES.put(short.class, Arrays.asList(Short.class, byte.class, Byte.class));
        COMPATIBLE_TYPES.put(byte.class, Arrays.asList(Byte.class));

        COMPATIBLE_TYPES.put(Double.class, Arrays.asList(double.class));
        COMPATIBLE_TYPES.put(Float.class, Arrays.asList(float.class));
        COMPATIBLE_TYPES.put(Long.class, Arrays.asList(long.class));
        COMPATIBLE_TYPES.put(Integer.class, Arrays.asList(int.class));
        COMPATIBLE_TYPES.put(Short.class, Arrays.asList(short.class));
        COMPATIBLE_TYPES.put(Byte.class, Arrays.asList(byte.class));
    }

    private ClassStructureWrapper(Class<?> sourceClass) {
        this.sourceClass = sourceClass;
        this.privateFlag = Modifier.isPrivate(sourceClass.getModifiers());
        this.assignableFromMap = Map.class.isAssignableFrom(sourceClass);

        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
        addAnnotations(annotationMap, sourceClass.getDeclaredAnnotations());
        this.annotationMap = annotationMap;
    }

    /**
     * 最大类结构缓存数（计划使用）
     */
    private static final int MAX_STRUCTURE_COUNT = 10000;

    // jdk invoke
    private final Class<?> sourceClass;
    private final boolean privateFlag;
    private final boolean assignableFromMap;
    private final Map<Class<? extends Annotation>, Annotation> annotationMap;

    // type
    private ClassWrapperType classWrapperType = ClassWrapperType.Normal;

    // is built in module
    private boolean javaBuiltInModule;

    // force use fields
    private boolean forceUseFields;

    // is record(jdk14+)
    private boolean record;

    // is Temporal
    private boolean temporal;

    private boolean subEnum;

    private int fieldCount;

    // setter的属性和SetterMethodInfo映射
    private Map<String, SetterInfo> setterInfos = new LinkedHashMap<String, SetterInfo>();

    /**
     * getter方法有序集合
     */
    private List<GetterInfo> getterInfos;

    /**
     * fieldAgent方法
     */
    private List<GetterInfo> getterInfoOfFields;

    // getter的属性和GetInfo映射
    private Map<String, GetterInfo> getterInfoMap = new HashMap<String, GetterInfo>();

    private Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();

    private long fieldsCheckCode;

    /**
     * 构造方法参数
     */
    private Object[] constructorArgs;

    /**
     * 构造方法
     */
    private Constructor<?> defaultConstructor;

    /**
     * public 方法集合
     */
    private Map<String, List<Method>> publicMethods = null;

    /**
     * 获取所有getter方法映射的GetterMethodInfo信息
     */
    public List<GetterInfo> getGetterInfos() {
        return getterInfos;
    }

    /**
     * 获取使用属性代理的所有GetterMethodInfo信息
     */
    public List<GetterInfo> getGetterInfos(boolean fieldAgent) {
        if (fieldAgent || javaBuiltInModule) {
            return getterInfoOfFields;
        }
        return getterInfos;
    }

    // public getter 方法 or field
    public GetterInfo getGetterInfo(String name) {
        return getterInfoMap.get(name);
    }

    private void fillGetterInfoMap() {
        for (GetterInfo getterInfo : getterInfos) {
            if (getterInfo.existField()) {
                String name = getterInfo.getField().getName();
                FieldInfo fieldInfo = fieldInfoMap.get(name);
                if (fieldInfo != null) {
                    getterInfo.setGenericParameterizedType(fieldInfo.getSetterInfo().getGenericParameterizedType());
                }
            }
            getterInfoMap.put(getterInfo.getName(), getterInfo);
        }
        for (GetterInfo getterInfo : getterInfoOfFields) {
            getterInfoMap.put(getterInfo.getName(), getterInfo);
            getterInfoMap.put(getterInfo.getField().getName(), getterInfo);
        }
    }

    public SetterInfo getSetterInfo(String name) {
        return setterInfos.get(name);
    }

    public boolean containsSetterKey(String fieldName) {
        return setterInfos.containsKey(fieldName);
    }

    public Class<?> getSourceClass() {
        return sourceClass;
    }

    public Object newInstance() throws Exception {
//        return UnsafeHelper.getUnsafe().allocateInstance(sourceClass);
        return defaultConstructor.newInstance(constructorArgs);
    }

    public Object newInstance(Object[] constructorArgs) throws Exception {
        return defaultConstructor.newInstance(constructorArgs);
    }

    public boolean isAssignableFromMap() {
        return assignableFromMap;
    }

    public boolean isRecord() {
        return record;
    }

    public boolean isTemporal() {
        return temporal;
    }

    public boolean isSubEnum() {
        return subEnum;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public boolean isForceUseFields() {
        return forceUseFields;
    }

    public ClassWrapperType getClassWrapperType() {
        return classWrapperType;
    }

    public Object[] createConstructorArgs() {
        Object[] constructorArgs = new Object[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            constructorArgs[i] = this.constructorArgs[i];
        }
        return constructorArgs;
    }

    public static ClassStructureWrapper get(Class<?> sourceClass) {
        if (sourceClass == null) {
            throw new IllegalArgumentException("sourceClass is null");
        }
        ClassStructureWrapper wrapper = classStructureWarppers.get(sourceClass);
        if (wrapper != null) {
            return wrapper;
        }
        if (sourceClass.isInterface() || sourceClass.isEnum() || sourceClass.isArray() || sourceClass.isPrimitive()) {
            return null;
        }
        if (wrapper == null) {
            synchronized (sourceClass) {
                if (classStructureWarppers.containsKey(sourceClass)) {
                    return classStructureWarppers.get(sourceClass);
                }
                wrapper = createBy(sourceClass);
                classStructureWarppers.put(sourceClass, wrapper);
            }
        }
        return wrapper;
    }

    public static ClassStructureWrapper ofEnumClass(Class<? extends Enum> enumClass) {
        if(!enumClass.isEnum()) {
            throw new UnsupportedOperationException("not enum class " + enumClass);
        }
        ClassStructureWrapper wrapper = createBy(enumClass);
        wrapper.forceUseFields = true;
        return wrapper;
    }

    private static ClassStructureWrapper createBy(Class<?> sourceClass) {
        ClassStructureWrapper wrapper = new ClassStructureWrapper(sourceClass);
        wrapper.checkClassStructure();
        // parse genericClass
        Type genericSuperclass = sourceClass.getGenericSuperclass();
        Map<String, Class<?>> superGenericClassMap = new HashMap<String, Class<?>>();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] types = parameterizedType.getActualTypeArguments();
            Class<?> superclass = (Class<?>) parameterizedType.getRawType();
            TypeVariable[] typeParameters = superclass.getTypeParameters();
            int i = 0;
            for (TypeVariable typeVariable : typeParameters) {
                String name = typeVariable.getName();
                Type actualTypeArgument = types[i++];
                if (actualTypeArgument instanceof Class) {
                    superGenericClassMap.put(name, (Class<?>) actualTypeArgument);
                }
            }
        }
        if (wrapper.record) {
            // Initialize the wrapper by constructing information
            wrapperWithRecordConstructor(wrapper, superGenericClassMap);
        } else {
            // Initialize the wrapper through the specifications (conventions) of pojo or Javabeans, namely method or field
            wrapperWithMethodAndField(wrapper, superGenericClassMap);
        }
        return wrapper;
    }

    private static void wrapperWithRecordConstructor(ClassStructureWrapper wrapper, Map<String, Class<?>> superGenericClassMap) {
        // sourceClass
        Class<?> sourceClass = wrapper.sourceClass;
        Constructor<?>[] constructors = sourceClass.getDeclaredConstructors();
        if (constructors.length == 0) return;
        Constructor<?> constructor = null; // constructors[0];
        int maxParameterCount = 0;
        for (Constructor<?> c : constructors) {
            if (constructor == null || c.getParameterCount() > maxParameterCount) {
                constructor = c;
                maxParameterCount = constructor.getParameterCount();
            }
        }

        wrapper.defaultConstructor = constructor;

        List<GetterInfo> getterInfoOfFields = new ArrayList<GetterInfo>();
        wrapper.getterInfoOfFields = getterInfoOfFields;
        wrapper.getterInfos = getterInfoOfFields;
        try {
            // parameters数组
            Object[] parameters = (Object[]) getParametersMethod.invoke(constructor);
            int len = parameters.length;
            wrapper.fieldCount = len;
            Method parameterNameMethod = null;
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();

            Object[] constructorArgs = new Object[len];
            wrapper.constructorArgs = constructorArgs;

            Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();
            long fieldsCheckCode = 0;
            for (int i = 0; i < len; i++) {
                Object parameter = parameters[i];
                if (parameterNameMethod == null) {
                    parameterNameMethod = parameter.getClass().getMethod("getName");
                    setAccessible(parameterNameMethod);
                }
                // invoke name
                String name = (String) parameterNameMethod.invoke(parameter);
                if (fieldsCheckCode == 0) {
                    fieldsCheckCode = FNV.hash64(name);
                } else {
                    fieldsCheckCode = FNV.hash64(fieldsCheckCode, name);
                }
                FieldInfo fieldInfo = new FieldInfo();
                fieldInfo.setName(name);
                fieldInfo.setIndex(i);

                Field nameField = sourceClass.getDeclaredField(name);
                Method nameMethod = sourceClass.getDeclaredMethod(name);
                setAccessible(nameField);
                setAccessible(nameMethod);

                Class<?> fieldType = nameField.getType();
                constructorArgs[i] = defaulTypeValue(fieldType);

                // 构建getter
                GetterInfo getterInfo = new GetterInfo();
                getterInfo.setField(nameField);
                getterInfo.setRecord(true);

                getterInfo.setName(name);
                getterInfo.setUnderlineName(StringUtils.camelCaseToSymbol(name));

                Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                addAnnotations(annotationMap, nameMethod.getAnnotations());

                getterInfo.setAnnotations(annotationMap);
                getterInfoOfFields.add(getterInfo);

                // 构建setter
                SetterInfo setterInfo = SetterInfo.fromField(nameField);
                setterInfo.setName(name);
                setterInfo.setField(nameField);
                setterInfo.setParameterType(fieldType);
                setterInfo.setIndex(i);
                Type genericType = genericParameterTypes[i];
                Class<?> declaringClass = nameField.getDeclaringClass();
                // parse
                parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, fieldType);
                setterInfo.setAnnotations(annotationMap);
                // put to setterInfos
                wrapper.setterInfos.put(name, setterInfo);

                fieldInfo.setGetterInfo(getterInfo);
                fieldInfo.setSetterInfo(setterInfo);
                fieldInfoMap.put(name, fieldInfo);
            }
            wrapper.fieldInfoMap = fieldInfoMap;
            wrapper.fieldsCheckCode = fieldsCheckCode;
        } catch (Throwable throwable) {
        }
    }

    private static void wrapperWithMethodAndField(ClassStructureWrapper wrapper, Map<String, Class<?>> superGenericClassMap) {
        // sourceClass
        Class<?> sourceClass = wrapper.sourceClass;
        boolean globalMIP = wrapper.annotationMap.containsKey(MethodInvokePriority.class);
        /** 获取构造方法参数最少的作为默认构造方法 */
        Constructor<?>[] constructors = sourceClass.getDeclaredConstructors();
        Constructor<?> defaultConstructor = null;
        int minParamCount = -1;
        Class<?>[] constructorParameterTypes = null;
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            int parameterCount = parameterTypes.length;
            if (minParamCount == -1 || minParamCount > parameterCount) {
                minParamCount = parameterCount;
                defaultConstructor = constructor;
                constructorParameterTypes = parameterTypes;
            }
            if (minParamCount == 0) {
                break;
            }
            if (minParamCount == parameterCount) {
                // 优先使用基本类型构造，防止在构造函数中出现NPE
                for (int i = 0; i < parameterCount; i++) {
                    if (parameterTypes[i].isPrimitive() && !constructorParameterTypes[i].isPrimitive()) {
                        defaultConstructor = constructor;
                        constructorParameterTypes = parameterTypes;
                        break;
                    }
                }
            }
        }

        setAccessible(defaultConstructor);
        Object[] args = new Object[minParamCount];
        for (int i = 0; i < minParamCount; i++) {
            Class<?> type = constructorParameterTypes[i];
            args[i] = defaulTypeValue(type);
        }

        wrapper.defaultConstructor = defaultConstructor;
        wrapper.constructorArgs = args;

        List<GetterInfo> getterInfos = new ArrayList<GetterInfo>();

        // public methods
        Method[] methods = sourceClass.getMethods();
        for (Method method : methods) {

            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class)
                continue;

            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();

            boolean startsWithGet;
            boolean isVoid = returnType == void.class;
            if (parameterTypes.length == 0 && ((startsWithGet = methodName.startsWith("get")) || methodName.startsWith("is"))
                    && !isVoid) {
                int startIndex = startsWithGet ? 3 : 2;
                if (methodName.length() == startIndex)
                    continue;
                boolean boolGetter = !startsWithGet;
                if (boolGetter && returnType != boolean.class) {
                    // isXXX only supported boolean
                    continue;
                }
                // getter方法
                setAccessible(method);
                GetterMethodInfo getterInfo = new GetterMethodInfo(method);

                String fieldName = new String(methodName.substring(startIndex));
                char[] fieldNameChars = UnsafeHelper.getChars(fieldName);
                if (fieldNameChars.length == 1 || !Character.isUpperCase(fieldNameChars[1])) {
                    fieldNameChars[0] = Character.toLowerCase(fieldNameChars[0]);
                    fieldName = new String(fieldNameChars);
                }

                getterInfo.setName(fieldName);
                getterInfo.setUnderlineName(StringUtils.camelCaseToSymbol(fieldName));

                // load annotations
                Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                addAnnotations(annotationMap, method.getAnnotations());
                if(!globalMIP && !annotationMap.containsKey(MethodInvokePriority.class)) {
                    try {
                        // declared field, not considering inheriting
                        Field field = sourceClass.getDeclaredField(fieldName);
                        if (!Modifier.isStatic(field.getModifiers())) {
                            // 当声明属性的类型和getter方法返回的类型不一致时，如果触发invoke，则以method的call为准
                            if (setAccessible(field) && compatibleType(returnType, field.getType())) {
                                getterInfo.setField(field);
                            }
                        }
                        addAnnotations(annotationMap, field.getAnnotations());
                    } catch (Exception e) {
                        if (boolGetter) {
                            // isXXX
                            try {
                                Field field = sourceClass.getDeclaredField(methodName);
                                if (!Modifier.isStatic(field.getModifiers())) {
                                    // 当声明属性的类型和getter方法返回的类型不一致时，如果触发invoke，则以method的call为准
                                    if (setAccessible(field) && field.getType() == boolean.class) {
                                        getterInfo.setField(field);
                                        getterInfo.setName(field.getName());
                                    }
                                }
                            } catch (Exception exception) {
                            }
                        }
                    }
                }
                getterInfo.setAnnotations(annotationMap);
                getterInfos.add(getterInfo);

            } else if (parameterTypes.length == 1 && methodName.startsWith("set")
                    && isVoid) {

                if (methodName.length() == 3)
                    continue;

                // setter方法
                setAccessible(method);
                SetterMethodInfo setterInfo = new SetterMethodInfo(method);

//                String setFieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                String setFieldName = new String(methodName.substring(3));
                char[] fieldNameChars = UnsafeHelper.getChars(setFieldName);
                if (fieldNameChars.length == 1 || !Character.isUpperCase(fieldNameChars[1])) {
                    fieldNameChars[0] = Character.toLowerCase(fieldNameChars[0]);
                    setFieldName = new String(fieldNameChars);
                }

                wrapper.setterInfos.put(setFieldName, setterInfo);
                // Support underline to camelCase
                String underlineName = StringUtils.camelCaseToSymbol(setFieldName);
                wrapper.setterInfos.put(underlineName, setterInfo);

                setterInfo.setName(setFieldName);
                Class<?> parameterType = parameterTypes[0];
                setterInfo.setParameterType(parameterType);

                Type genericType = method.getGenericParameterTypes()[0];
                parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, parameterType);

                // 解析setter和field注解集合
                Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();

                Annotation[] methodAnnotations = method.getAnnotations();
                addAnnotations(annotationMap, methodAnnotations);
                if(!globalMIP && !annotationMap.containsKey(MethodInvokePriority.class)) {
                    try {
                        Field field = sourceClass.getDeclaredField(setFieldName);
                        if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                            if (setAccessible(field) && compatibleType(field.getType(), parameterType)) {
                                setterInfo.setField(field);
                            } else {
                                setterInfo.setFieldDisabled(true);
                            }
                        }
                        Annotation[] fieldAnnotations = field.getAnnotations();
                        addAnnotations(annotationMap, fieldAnnotations);
                    } catch (Exception e) {
                    }
                }
                setterInfo.setAnnotations(annotationMap);
            }
        }

        // Resolve all fields
        parseWrapperFields(wrapper, sourceClass, superGenericClassMap);

        // Sort output to prevent inconsistent serialization order after each restart of the JVM
        Collections.sort(getterInfos, new Comparator<GetterInfo>() {
            public int compare(GetterInfo o1, GetterInfo o2) {
//                if(o1.getName().charAt(0) == o2.getName().charAt(0)) {
//                    if(o1.isPrimitive()) {
//                        return -1;
//                    }
//                    if(o2.isPrimitive()) {
//                        return 1;
//                    }
//                }
                return o1.getName().compareTo(o2.getName());
            }
        });

        wrapper.getterInfos = Collections.unmodifiableList(getterInfos);
        wrapper.setterInfos = Collections.unmodifiableMap(wrapper.setterInfos);
        wrapper.fillGetterInfoMap();
        if (wrapper.getterInfos.size() == 0 && wrapper.getterInfoOfFields != null && wrapper.getterInfoOfFields.size() > 0) {
            wrapper.forceUseFields = true;
        }
    }

    private static boolean compatibleType(Class<?> type, Class<?> parameterType) {
        if (type.isAssignableFrom(parameterType)) return true;
        List<Class<?>> types = COMPATIBLE_TYPES.get(type);
        if (types == null) return false;
        return types.indexOf(parameterType) > -1;
    }

    private static Object defaulTypeValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        } else if (type.isPrimitive()) {
            if (type == char.class) {
                return (char) 0;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == short.class) {
                return (short) 0;
            }
            return 0;
        } else if (type == String.class) {
            return "";
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        } else {
            return null;
        }
    }

    private void checkClassStructure() {
        String pckName = sourceClass.getPackage().getName();
        if (pckName.startsWith("java.") || pckName.startsWith("sun.")) {
            this.javaBuiltInModule = true;
        }

        // jdk17 java.lang.Record
        Class<?> theSuperClass = sourceClass.getSuperclass();
        if (theSuperClass.getName().equals("java.lang.Record")) {
            this.record = true;
            this.javaBuiltInModule = true;
            this.classWrapperType = ClassWrapperType.Record;
        }

        if (theSuperClass.isEnum()) {
            this.subEnum = true;
        }

        if (javaBuiltInModule) {
            forceUseFields = true;
            for (Class superClass : USE_GETTER_METHOD_TYPE_LIST) {
                if (superClass.isAssignableFrom(sourceClass)) {
                    forceUseFields = false;
                    break;
                }
            }
            String className = sourceClass.getName();
            if (className.equals("java.time.LocalDate")) {
                this.classWrapperType = ClassWrapperType.TemporalLocalDate;
                this.temporal = true;
            } else if (className.equals("java.time.LocalTime")) {
                this.classWrapperType = ClassWrapperType.TemporalLocalTime;
                this.temporal = true;
            } else if (className.equals("java.time.LocalDateTime")) {
                this.classWrapperType = ClassWrapperType.TemporalLocalDateTime;
                this.temporal = true;
            } else if (className.equals("java.time.Instant")) {
                this.classWrapperType = ClassWrapperType.TemporalInstant;
                this.temporal = true;
            } else if (className.equals("java.time.ZonedDateTime")) {
                this.classWrapperType = ClassWrapperType.TemporalZonedDateTime;
                this.temporal = true;
            } else if (className.equals("java.time.OffsetDateTime")) {
                this.classWrapperType = ClassWrapperType.TemporalOffsetDateTime;
                this.temporal = true;
            }
        }
    }

    private static void parseSetterGenericType(Map<String, Class<?>> superGenericClassMap, Class<?> sourceClass, Class<?> declaringClass, SetterInfo setterInfo, Type genericType, Class<?> parameterType) {

        GenericParameterizedType genericParameterizedType = null;
        if (Collection.class.isAssignableFrom(parameterType)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type type = pt.getActualTypeArguments()[0];
                if (type instanceof Class<?>) {
                    setterInfo.setActualTypeArgument((Class<?>) type);
                }
                genericParameterizedType = GenericParameterizedType.genericCollectionType(parameterType, type);
            } else {
                // 没有泛型将集合视作普通实体类创建泛型结构
                genericParameterizedType = GenericParameterizedType.newActualType(parameterType);
            }
        } else if (parameterType.isArray()) {
            Class<?> componentType = parameterType.getComponentType();
            setterInfo.setActualTypeArgument(componentType);
            if (genericType instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType) genericType;
                Type genericComponentType = genericArrayType.getGenericComponentType();
                genericParameterizedType = GenericParameterizedType.genericArrayType(genericComponentType);
            } else {
                genericParameterizedType = GenericParameterizedType.arrayType(componentType);
            }
        } else if (Map.class.isAssignableFrom(parameterType)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] actualTypeArguments = pt.getActualTypeArguments();
                if (actualTypeArguments.length == 2) {
                    genericParameterizedType = GenericParameterizedType.genericMapType(parameterType, actualTypeArguments[0], actualTypeArguments[1]);
                }
            } else {
                // 没有泛型创建普通实体类泛型结构
                genericParameterizedType = GenericParameterizedType.newActualType(parameterType);
            }
        } else {
            if (parameterType.isInterface() || Modifier.isAbstract(parameterType.getModifiers())) {
                // Map(LinkHashMap, HashMap)和Collection(ArayList)都有缺省实现类，其他接口或者抽象类,无法通过newInstance反射创建实例
                // 可以根据设置属性默认值来获取实际实例化的类型
                // 基本类型需要排除
                if (!parameterType.isPrimitive()) {
                    setterInfo.setNonInstanceType(true);
                }
            }
            if (genericType instanceof TypeVariable) {
                // 伪泛型
                TypeVariable typeVariable = (TypeVariable) genericType;
                String name = typeVariable.getName();
                if (declaringClass != sourceClass) {
                    // maybe parent method
                    Class<?> superGenericClass = superGenericClassMap.get(name);
                    genericParameterizedType = GenericParameterizedType.newActualType(superGenericClass);
                } else {
                    genericParameterizedType = GenericParameterizedType.genericEntityType(parameterType, typeVariable.getName());
                }
            } else if (genericType instanceof ParameterizedType) {
                // 实泛型
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] actualTypeArguments = pt.getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    Type actualTypeArgument = actualTypeArguments[0];
                    if (actualTypeArgument instanceof Class) {
                        genericParameterizedType = GenericParameterizedType.entityType(parameterType, (Class<?>) actualTypeArgument);
                    } else {
                        genericParameterizedType = GenericParameterizedType.newActualType(parameterType);
                    }
                } else {
                    TypeVariable[] typeParameters = parameterType.getTypeParameters();
                    int i = 0;
                    Map<String, Class<?>> genericClassMap = new HashMap<String, Class<?>>();
                    for (TypeVariable typeVariable : typeParameters) {
                        String name = typeVariable.getName();
                        Type actualTypeArgument = actualTypeArguments[i++];
                        if (actualTypeArgument instanceof Class) {
                            genericClassMap.put(name, (Class<?>) actualTypeArgument);
                        }
                    }
                    genericParameterizedType = GenericParameterizedType.entityType(parameterType, genericClassMap);
                }

            } else {
                genericParameterizedType = GenericParameterizedType.newActualType(parameterType);
            }
        }

        if (genericParameterizedType != null) {
            setterInfo.setGenericParameterizedType(genericParameterizedType);
        }
    }

    /**
     * 解析类的所有字段（包含父类字段）
     */
    private static void parseWrapperFields(ClassStructureWrapper wrapper, Class<?> sourceClass, Map<String, Class<?>> superGenericClassMap) {
        Class<?> target = sourceClass;
        Set<String> fieldNames = new HashSet<String>();
        List<GetterInfo> getterInfoOfFields = new ArrayList<GetterInfo>();
        Map<String, FieldInfo> fieldInfoMap = new HashMap<String, FieldInfo>();
        int cnt = 0, index = 0;
        long fieldsCheckCode = 0;
        while (target != Object.class) {
            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                if (field.isSynthetic()) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
                String fieldName = field.getName();
                String underlineName = StringUtils.camelCaseToSymbol(fieldName);
                if (fieldNames.add(fieldName)) {
                    setAccessible(field);
                    clearFinalModifiers(field);

                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.setName(fieldName);
                    fieldInfo.setIndex(index++);

                    if (fieldsCheckCode == 0) {
                        fieldsCheckCode = FNV.hash64(fieldName);
                    } else {
                        fieldsCheckCode = FNV.hash64(fieldsCheckCode, fieldName);
                    }
                    Class<?> fieldType = field.getType();
                    // 构建getter
                    GetterInfo getterInfo = new GetterInfo();
                    getterInfo.setField(field);
                    getterInfo.setName(fieldName);
                    getterInfo.setUnderlineName(underlineName);

                    Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                    addAnnotations(annotationMap, field.getAnnotations());

                    getterInfo.setAnnotations(annotationMap);
                    getterInfoOfFields.add(getterInfo);
                    fieldInfo.setGetterInfo(getterInfo);

                    // create setter
                    SetterInfo setterInfo = SetterInfo.fromField(field);
                    setterInfo.setName(fieldName);
                    setterInfo.setField(field);
                    setterInfo.setParameterType(fieldType);

                    Type genericType = field.getGenericType();
                    Class<?> declaringClass = field.getDeclaringClass();
                    // parse Generic Type
                    parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, fieldType);
                    setterInfo.setAnnotations(annotationMap);

                    // Consistent getter and setter generic information reflected by attributes
                    getterInfo.setGenericParameterizedType(setterInfo.getGenericParameterizedType());

                    SetterInfo oldSetterInfo = wrapper.setterInfos.get(fieldName);
                    if (oldSetterInfo == null || !oldSetterInfo.isFieldDisabled()) {
                        wrapper.setterInfos.put(fieldName, setterInfo);
                    }

                    if (!underlineName.equals(fieldName)) {
                        wrapper.setterInfos.put(underlineName, setterInfo);
                    }
                    fieldInfo.setSetterInfo(setterInfo);
                    fieldInfoMap.put(fieldName, fieldInfo);
                }
            }
            target = target.getSuperclass();
            // Avoid deadlock
            if (++cnt == 100) {
                break;
            }
        }
        wrapper.getterInfoOfFields = Collections.unmodifiableList(getterInfoOfFields);
        wrapper.fieldInfoMap = fieldInfoMap;
        wrapper.fieldsCheckCode = fieldsCheckCode;
    }

    static final Field modifierField;
    static final Method getParametersMethod;

    static {
        Field field = null;
        try {
            field = Field.class.getDeclaredField("modifiers");
            setAccessible(field);
        } catch (Exception e) {
        }
        modifierField = field;

        // jdk8+ supported
        Method parametersMethod = null;
        try {
            parametersMethod = Method.class.getMethod("getParameters");
            parametersMethod.setAccessible(true);
            setAccessible(parametersMethod);
        } catch (Exception e) {
        }
        getParametersMethod = parametersMethod;
    }

    private static void clearFinalModifiers(Field field) {
        if (modifierField != null) {
            try {
                modifierField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (Exception e) {
            }
        }
    }

    private static boolean setAccessible(AccessibleObject accessibleObject) {

        try {
            boolean accessible = UnsafeHelper.setAccessible(accessibleObject);
            if (accessible) {
                return true;
            }
        } catch (Throwable e1) {
        }
        try {
            accessibleObject.setAccessible(true);
            return true;
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 获取所有setter信息的名称set
     *
     * @return
     */
    public Set<String> setterNames() {
        return setterInfos.keySet();
    }

    private static void addAnnotations(Map<Class<? extends Annotation>, Annotation> annotationMap,
                                       Annotation[] annotationArr) {
        if (annotationMap == null || annotationArr == null)
            return;
        for (Annotation annotation : annotationArr) {
            annotationMap.put(annotation.annotationType(), annotation);
        }
    }

    public Object invokePublic(Object invoker, String methodName, Object[] params) {
        if (publicMethods == null) {
            synchronized (this) {
                if (publicMethods == null) {
                    publicMethods = new HashMap<String, List<Method>>();
                    Method[] methods = sourceClass.getMethods();
                    for (Method method : methods) {
                        String name = method.getName();
                        List<Method> nameMethods = publicMethods.get(name);
                        if (nameMethods == null) {
                            nameMethods = new ArrayList<Method>();
                            publicMethods.put(name.intern(), nameMethods);
                        }
                        setAccessible(method);
                        nameMethods.add(method);
                    }
                }
            }
        }
        List<Method> nameMethods = publicMethods.get(methodName);
        if (nameMethods == null) {
            throw new UnsupportedOperationException("method " + methodName + " is not exist or not a public method ");
        }
        try {
            if (nameMethods.size() == 1) {
                Method method = nameMethods.get(0);
                Class[] parameterTypes = method.getParameterTypes();
                if(parameterTypes.length != params.length) {
                    throw new IllegalArgumentException("argument mismatch");
                }
                for (int i = 0, n = params.length; i < n; ++i) {
                    Object value = params[i];
                    Class parameterType = parameterTypes[i];
                    if(!parameterType.isInstance(value)) {
                        try {
                            params[i] = ObjectUtils.toType(value, parameterType);
                        } catch (Throwable throwable) {
                            throw new IllegalArgumentException("argument mismatch: " + parameterType + " ");
                        }
                    }
                }
                return nameMethods.get(0).invoke(invoker, params);
            }
            for (Method method : nameMethods) {
                Class[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == params.length) {
                    boolean matched = true;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (params[i] != null && !parameterTypes[i].isInstance(params[i])) {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) {
                        return method.invoke(invoker, params);
                    }
                }
            }
            throw new UnsupportedOperationException("method " + methodName + " of " + sourceClass + " Parameter mismatch ");
        } catch (Throwable throwable) {
            throw new InvokeReflectException(throwable);
        }
    }

    public Set<SetterInfo> setterSet() {
        return new HashSet<SetterInfo>(setterInfos.values());
    }

    public FieldInfo[] getFieldInfos() {
        FieldInfo[] fieldInfos = new FieldInfo[fieldInfoMap.size()];
        return fieldInfoMap.values().toArray(fieldInfos);
    }

    public long getFieldsCheckCode() {
        return fieldsCheckCode;
    }

    public boolean isPrivate() {
        return privateFlag;
    }

    public boolean isJavaBuiltInModule() {
        return javaBuiltInModule;
    }

    public Annotation getDeclaredAnnotation(Class<? extends Annotation> annotationClass) {
        return annotationMap.get(annotationClass);
    }

    public enum ClassWrapperType {
        /**
         * 普通的pojo
         */
        Normal,

        /**
         * record(jdk15+) support
         */
        Record,

        /**
         * LocalDate(jdk8+) support
         */
        TemporalLocalDate,

        /**
         * LocalDate(jdk8+) support
         */
        TemporalLocalDateTime,

        /**
         * LocalTime(jdk8+) support
         */
        TemporalLocalTime,

        /**
         * instant(jdk8+) support
         */
        TemporalInstant,

        /**
         * ZonedDateTime(jdk8+) support
         */
        TemporalZonedDateTime,

        /**
         * OffsetDateTime(jdk8+) support
         */
        TemporalOffsetDateTime
    }
}

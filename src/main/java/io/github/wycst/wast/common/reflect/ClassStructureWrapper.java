package io.github.wycst.wast.common.reflect;

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

    private ClassStructureWrapper() {
    }

    // cache
    private static Map<Class<?>, ClassStructureWrapper> classStructureWarppers = new ConcurrentHashMap<Class<?>, ClassStructureWrapper>();

    // 内置类默认使用field序列化，可维护名称列表控制使用getter method
    private static String[] USE_GETTER_METHOD_TYPE_NAME_LIST = {
    };

    // 内置类默认使用field序列化，可维护超类列表控制使用getter method
    private static Class[] USE_GETTER_METHOD_TYPE_LIST = {
            Throwable.class,
            Error.class
    };

    /**
     * 最大类结构缓存数（计划使用）
     */
    private static final int MAX_STRUCTURE_COUNT = 10000;

    // jdk invoke
    private Class<?> sourceClass;

    // is built in module
    private boolean javaBuiltInModule;

    // force use fields
    private boolean forceUseFields;

    // is record(jdk14+)
    private boolean record;

    private int fieldCount;

    private boolean assignableFromMap;

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

    /**
     * 构造方法参数
     */
    private Object[] constructorArgs;

    /**
     * 构造方法
     */
    private Constructor<?> defaultConstructor;

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

    public int getFieldCount() {
        return fieldCount;
    }

    public boolean isForceUseFields() {
        return forceUseFields;
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
//            throw new UnsupportedOperationException("sourceClass " + sourceClass + " is not supported to create wrapper");
            return null;
        }

//        boolean anonymousClass = sourceClass.isAnonymousClass();
//        if(anonymousClass) {
//            Class<?> parentClass = sourceClass.getSuperclass();
//            // 使用父类代理（通常情况下父类一定不是匿名类）
//            while (parentClass.isAnonymousClass()) {
//                parentClass = parentClass.getSuperclass();
//            }
//            if(parentClass == Object.class) {
//                return null;
//            }
//            sourceClass = parentClass;
//            wrapper = classStructureWarppers.get(sourceClass);
//        }

        if (wrapper == null) {
            synchronized (sourceClass) {
                if (classStructureWarppers.containsKey(sourceClass)) {
                    return classStructureWarppers.get(sourceClass);
                }

                wrapper = new ClassStructureWrapper();
                wrapper.sourceClass = sourceClass;
                wrapper.assignableFromMap = Map.class.isAssignableFrom(sourceClass);
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
                    // 通过构造信息初始化wrapper
                    wrapperWithRecordConstructor(wrapper, superGenericClassMap);
                } else {
                    // 通过pojo或者javabean的规范（公约）即method或者field初始化wrapper
                    wrapperWithMethodAndField(wrapper, superGenericClassMap);
                }

                classStructureWarppers.put(sourceClass, wrapper);
            }
        }
        return wrapper;
    }

    private static void wrapperWithRecordConstructor(ClassStructureWrapper wrapper, Map<String, Class<?>> superGenericClassMap) {
        // sourceClass
        Class<?> sourceClass = wrapper.sourceClass;
        Constructor<?>[] constructors = sourceClass.getDeclaredConstructors();
        if (constructors.length == 0) return;
        Constructor<?> constructor = constructors[0];
        wrapper.defaultConstructor = constructor;

        List<GetterInfo> getterInfoOfFields = new ArrayList<GetterInfo>();
        wrapper.getterInfoOfFields = getterInfoOfFields;
        wrapper.getterInfos = getterInfoOfFields;
        try {
            // parameters数组
            Object parameters = getParametersMethod.invoke(constructor);
            int len = Array.getLength(parameters);
            wrapper.fieldCount = len;
            Method parameterNameMethod = null;
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();

            Object[] constructorArgs = new Object[len];
            wrapper.constructorArgs = constructorArgs;
            for (int i = 0; i < len; i++) {
                Object parameter = Array.get(parameters, i);
                if (parameterNameMethod == null) {
                    parameterNameMethod = parameter.getClass().getMethod("getName");
                    setAccessible(parameterNameMethod);
                }
                // invoke name
                String name = (String) parameterNameMethod.invoke(parameter);

                Field nameField = sourceClass.getDeclaredField(name);
                Method nameMethod = sourceClass.getDeclaredMethod(name);
                setAccessible(nameField);
                setAccessible(nameMethod);

                Class<?> fieldType = nameField.getType();
                constructorArgs[i] = defaulTypeValue(fieldType);

                // 构建getter
                GetterInfo getterInfo = new GetterInfo();
                getterInfo.setField(nameField);

                getterInfo.setName(name);

                Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                addAnnotations(annotationMap, nameMethod.getAnnotations());

                getterInfo.setAnnotations(annotationMap);
                getterInfoOfFields.add(getterInfo);

                // 构建setter
                SetterInfo setterInfo = new SetterInfo();
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
            }
        } catch (Throwable throwable) {
        }

    }

    private static void wrapperWithMethodAndField(ClassStructureWrapper wrapper, Map<String, Class<?>> superGenericClassMap) {
        // sourceClass
        Class<?> sourceClass = wrapper.sourceClass;

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

                // getter方法
                setAccessible(method);
                GetterMethodInfo getterInfo = new GetterMethodInfo(method);

                String fieldName = methodName.substring(startIndex, startIndex + 1).toLowerCase()
                        + methodName.substring(startIndex + 1);

                getterInfo.setName(fieldName);
                getterInfo.setUnderlineName(StringUtils.camelCaseToSymbol(fieldName));

                // load annotations
                Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                addAnnotations(annotationMap, method.getAnnotations());
                try {
                    // 属性
                    Field field = sourceClass.getDeclaredField(fieldName);
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        if (setAccessible(field)) {
                            getterInfo.setField(field);
                        }
                    }
                    addAnnotations(annotationMap, field.getAnnotations());
                } catch (Exception e) {
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

                String setFieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
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
                try {
                    Field field = sourceClass.getDeclaredField(setFieldName);
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                        if (setAccessible(field)) {
                            setterInfo.setField(field);
                        }
                    }
                    Annotation[] fieldAnnotations = field.getAnnotations();
                    addAnnotations(annotationMap, fieldAnnotations);
                } catch (Exception e) {
                }
                // 注解集合
                setterInfo.setAnnotations(annotationMap);
            }
        }

        // 解析所有字段
        parseWrapperFields(wrapper, sourceClass, superGenericClassMap);

        // 排序输出防止每次重启jvm后序列化顺序不一致
        Collections.sort(getterInfos, new Comparator<GetterInfo>() {
            public int compare(GetterInfo o1, GetterInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        wrapper.getterInfos = Collections.unmodifiableList(getterInfos);
        wrapper.setterInfos = Collections.unmodifiableMap(wrapper.setterInfos);
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
        if (sourceClass.getSuperclass().getName().equals("java.lang.Record")) {
            this.record = true;
            this.javaBuiltInModule = true;
        }

        if(javaBuiltInModule) {
            forceUseFields = true;
            for (Class superClass : USE_GETTER_METHOD_TYPE_LIST) {
                if(superClass.isAssignableFrom(sourceClass)) {
                    forceUseFields = false;
                    break;
                }
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
        while (target != Object.class) {
            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                if (field.isSynthetic()) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
                String fieldName = field.getName();
                if (fieldNames.add(fieldName)) {
                    setAccessible(field);
                    clearFinalModifiers(field);

                    Class<?> fieldType = field.getType();
                    // 构建getter
                    GetterInfo getterInfo = new GetterInfo();
                    getterInfo.setField(field);
                    getterInfo.setName(fieldName);

                    Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                    addAnnotations(annotationMap, field.getAnnotations());

                    getterInfo.setAnnotations(annotationMap);
                    getterInfoOfFields.add(getterInfo);

                    // create setter
                    SetterInfo setterInfo = new SetterInfo();
                    setterInfo.setName(fieldName);
                    setterInfo.setField(field);
                    setterInfo.setParameterType(fieldType);

                    Type genericType = field.getGenericType();
                    Class<?> declaringClass = field.getDeclaringClass();
                    // parse Generic Type
                    parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, fieldType);
                    setterInfo.setAnnotations(annotationMap);

                    wrapper.setterInfos.put(fieldName, setterInfo);
                }
            }
            target = target.getSuperclass();
        }
        wrapper.getterInfoOfFields = Collections.unmodifiableList(getterInfoOfFields);
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
}

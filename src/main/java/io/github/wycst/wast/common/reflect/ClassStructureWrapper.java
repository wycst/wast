package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * class序列化和反序列化结构包装
 * @author wangy
 */
public final class ClassStructureWrapper {

    private ClassStructureWrapper() {
    }

    // cache
    private static Map<Class<?>, ClassStructureWrapper> classStructureWarppers = new HashMap<Class<?>, ClassStructureWrapper>();

    /**
     * 最大类结构缓存数（计划使用）
     */
    private static final int MAX_STRUCTURE_COUNT = 10000;

    // jdk invoke
    private Class<?> sourceClass;

    private boolean assignableFromMap;

    // setter的属性和SetterMethodInfo映射
    private Map<String, SetterInfo> setterInfos = new HashMap<String, SetterInfo>();

    // 以数组+链表存储（setter）
    private SetterNode[] setterNodes;

    /**
     * getter方法有序集合
     */
    private List<GetterInfo> getterInfos;

    /**
     * fieldAgent方法
     */
    private List<GetterInfo> fieldAgentGetterFieldInfos;

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
        if (!fieldAgent) {
            return getterInfos;
        }
        return fieldAgentGetterFieldInfos;
    }

    //    public Map<String, SetterInfo> getSetterInfos() {
//        return Collections.unmodifiableMap(setterInfos);
//    }
    public SetterInfo getSetterInfo(String name) {
        return setterInfos.get(name);
    }

    public boolean containsSetterKey(String fieldName) {
        return setterInfos.containsKey(fieldName);
    }

    public Class<?> getSourceClass() {
        return sourceClass;
    }

    public Object newInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return defaultConstructor.newInstance(constructorArgs);
    }

    public boolean isAssignableFromMap() {
        return assignableFromMap;
    }

    public SetterInfo getSetterInfo(char[] buffers, int beginIndex, int endIndex) {
        int len = endIndex - beginIndex;
        int hashValue = hash(buffers, beginIndex, len);
        int capacity = setterNodes.length;
        int index = hashValue & capacity - 1;
        SetterNode setterNode = setterNodes[index];
        if (setterNode == null) {
            return null;
        }
        while (!matchKey(buffers, beginIndex, len, setterNode.key)) {
            setterNode = setterNode.next;
            if (setterNode == null) {
                return null;
            }
        }
        return setterNode.value;
    }

    /**
     * ~自定义hash算法
     *
     * @param buffers
     * @param offset
     * @param len
     * @return
     * @see String#hashCode()
     */
    public static int hash(char[] buffers, int offset, int len) {
        int h = 0;
        int off = offset;
        for (int i = 0; i < len; i++) {
            h = 31 * h + buffers[off++];
        }
        return h;
    }

    private static boolean matchKey(char[] buffers, int offset, int len, char[] key) {
        if (len != key.length) return false;
        for (int j = 0; j < len; j++) {
            if (buffers[offset + j] != key[j]) return false;
        }
        return true;
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

                /** 获取构造方法参数最少的作为默认构造方法 */
                Constructor<?>[] constructors = sourceClass.getDeclaredConstructors();

                Constructor<?> defaultConstructor = null;
                int minParameterCount = -1;
                Class<?>[] constructorParameterTypes = null;
                for (Constructor<?> constructor : constructors) {
                    constructorParameterTypes = constructor.getParameterTypes();
                    int parameterCount = constructorParameterTypes.length;
                    if (minParameterCount == -1 || minParameterCount > parameterCount) {
                        minParameterCount = parameterCount;
                        defaultConstructor = constructor;
                    }
                    if (minParameterCount == 0) {
                        break;
                    }
                }

                defaultConstructor.setAccessible(true);
                Object[] args = new Object[minParameterCount];
                for (int i = 0; i < minParameterCount; i++) {
                    Class<?> type = constructorParameterTypes[i];
                    // 除了基本类型或boolean类型，默认每个参数都是null
                    if (type == boolean.class) {
                        args[i] = false;
                    } else if (type.isPrimitive()) {
                        args[i] = 0;
                    } else if (type == String.class) {
                        // 兼容 kotlin
                        args[i] = "";
                    }
                }

                wrapper.defaultConstructor = defaultConstructor;
                wrapper.constructorArgs = args;

                List<GetterInfo> getterInfos = new ArrayList<GetterInfo>();

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
                    if (parameterTypes.length == 0 && (startsWithGet = methodName.startsWith("get") || methodName.startsWith("is"))
                            && !isVoid) {
                        int startIndex = startsWithGet ? 3 : 2;
                        if (methodName.length() == startIndex)
                            continue;

                        // getter方法
                        method.setAccessible(true);
                        GetterMethodInfo getterInfo = new GetterMethodInfo(method);

                        String fieldName = methodName.substring(startIndex, startIndex + 1).toLowerCase()
                                + methodName.substring(startIndex + 1);

                        getterInfo.setMappingName(fieldName);
                        getterInfo.setUnderlineName(StringUtils.camelCaseToSymbol(fieldName));
                        getterInfo.setReturnType(returnType);

                        // 加载注解
                        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                        addAnnotations(annotationMap, method.getAnnotations());
                        try {
                            // 属性
                            Field field = sourceClass.getDeclaredField(fieldName);
                            getterInfo.setField(field);
                            addAnnotations(annotationMap, field.getAnnotations());
                        } catch (Exception e) {
                        }
                        getterInfo.setAnnotations(annotationMap);
                        getterInfo.fixed();
                        getterInfos.add(getterInfo);

                    } else if (parameterTypes.length == 1 && methodName.startsWith("set")
                            && isVoid) {

                        if (methodName.length() == 3)
                            continue;

                        // setter方法
                        method.setAccessible(true);
                        SetterMethodInfo setterInfo = new SetterMethodInfo(method);

                        String setFieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                        wrapper.setterInfos.put(setFieldName, setterInfo);
                        // Support underline to camelCase
                        String underlineName = StringUtils.camelCaseToSymbol(setFieldName);
                        wrapper.setterInfos.put(underlineName, setterInfo);

                        setterInfo.setMappingName(setFieldName);

                        Class<?> parameterType = parameterTypes[0];
                        setterInfo.setParameterType(parameterType);

                        Type genericType = method.getGenericParameterTypes()[0];
                        parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, parameterType);

                        // 当确定parameterType和genericClazz后再调用
                        setterInfo.initParamClassType();

                        // 解析setter和field注解集合
                        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();

                        Annotation[] methodAnnotations = method.getAnnotations();
                        addAnnotations(annotationMap, methodAnnotations);
                        try {
                            Field field = sourceClass.getDeclaredField(setFieldName);
                            field.setAccessible(true);
                            Annotation[] fieldAnnotations = field.getAnnotations();
                            setterInfo.setField(field);
                            addAnnotations(annotationMap, fieldAnnotations);
                        } catch (Exception e) {
                        }
                        // 注解集合
                        setterInfo.setAnnotations(annotationMap);
                        String mappingName = setterInfo.getMappingName();
                        if (!setFieldName.equals(mappingName)) {
                            // 复制
                            wrapper.setterInfos.put(mappingName, setterInfo);
                        }
                    }
                }

                // 解析所有字段
                parseWrapperFields(wrapper, sourceClass, superGenericClassMap);

                // 计算setter的hash映射
                wrapper.calculateHashMapping();

                wrapper.getterInfos = Collections.unmodifiableList(getterInfos);
                wrapper.setterInfos = Collections.unmodifiableMap(wrapper.setterInfos);

                classStructureWarppers.put(sourceClass, wrapper);
            }
        }
        return wrapper;
    }

    private static void parseSetterGenericType(Map<String, Class<?>> superGenericClassMap, Class<?> sourceClass, Class<?> declaringClass, SetterInfo setterInfo, Type genericType, Class<?> parameterType) {

        GenericParameterizedType genericParameterizedType = null;
        if (Collection.class.isAssignableFrom(parameterType)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type type = pt.getActualTypeArguments()[0];
                if(type instanceof Class<?>) {
                    setterInfo.setActualTypeArgument((Class<?>)type);
                }
                genericParameterizedType = GenericParameterizedType.genericCollectionType(parameterType, type);
            } else {
                // 没有泛型将集合视作普通实体类创建泛型结构
                genericParameterizedType = GenericParameterizedType.actualType(parameterType);
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
                genericParameterizedType = GenericParameterizedType.actualType(parameterType);
            }
        } else {
            if (parameterType.isInterface() || Modifier.isAbstract(parameterType.getModifiers())) {
                // Map(LinkHashMap, HashMap)和Collection(ArayList)都有缺省实现类，其他接口或者抽象类,无法通过newInstance反射创建实例
                // 可以根据设置属性默认值来获取实际实例化的类型
                setterInfo.setNonInstanceType(true);
            }
            if (genericType instanceof TypeVariable) {
                // 伪泛型
                TypeVariable typeVariable = (TypeVariable) genericType;
                String name = typeVariable.getName();
                if (declaringClass != sourceClass) {
                    // maybe parent method
                    Class<?> superGenericClass = superGenericClassMap.get(name);
                    genericParameterizedType = GenericParameterizedType.actualType(superGenericClass);
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
                        genericParameterizedType = GenericParameterizedType.actualType(parameterType);
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
                genericParameterizedType = GenericParameterizedType.actualType(parameterType);
            }
        }

        if (genericParameterizedType != null) {
            setterInfo.setGenericParameterizedType(genericParameterizedType);
            genericParameterizedType.ownerInfo = setterInfo;
        }
    }

    /**
     * 解析类的所有字段（包含父类字段）
     */
    private static void parseWrapperFields(ClassStructureWrapper wrapper, Class<?> sourceClass, Map<String, Class<?>> superGenericClassMap) {
        Class<?> target = sourceClass;
        Set<String> fieldNames = new HashSet<String>();
        List<GetterInfo> fieldAgentGetterFieldInfos = new ArrayList<GetterInfo>();
        while (target != Object.class) {
            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                if (field.isSynthetic()) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
                String fieldName = field.getName();
                if (fieldNames.add(fieldName)) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    // 构建getter
                    GetterInfo getterInfo = new GetterInfo();
                    getterInfo.setField(field);

                    getterInfo.setMappingName(fieldName);
                    getterInfo.setReturnType(fieldType);

                    Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
                    addAnnotations(annotationMap, field.getAnnotations());

                    getterInfo.setAnnotations(annotationMap);
                    getterInfo.fixed();
                    fieldAgentGetterFieldInfos.add(getterInfo);

                    // 构建setter
                    SetterInfo setterInfo = new SetterInfo();
                    setterInfo.setMappingName(fieldName);
                    setterInfo.setField(field);
                    setterInfo.setParameterType(fieldType);

                    Type genericType = field.getGenericType();
                    Class<?> declaringClass = field.getDeclaringClass();
                    // 解析泛型结构
                    parseSetterGenericType(superGenericClassMap, sourceClass, declaringClass, setterInfo, genericType, fieldType);

                    setterInfo.initParamClassType();
                    setterInfo.setAnnotations(annotationMap);

                    String mappingName = setterInfo.getMappingName();
                    if (!wrapper.setterInfos.containsKey(mappingName)) {
                        wrapper.setterInfos.put(mappingName, setterInfo);
                    }
                }
            }
            target = target.getSuperclass();
        }
        wrapper.fieldAgentGetterFieldInfos = Collections.unmodifiableList(fieldAgentGetterFieldInfos);
    }

    // 内置链表结构
    static class SetterNode {
        public SetterNode(char[] key, SetterInfo value) {
            this.key = key;
            this.value = value;
        }

        char[] key;
        SetterInfo value;
        SetterNode next;
    }

    /**
     * @param cap
     * @return
     * @See java.util.HashMap#tableSizeFor(int)
     */
    static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1 << 30) ? 1 << 30 : n + 1;
    }

    private void calculateHashMapping() {

        // calculate capacity
        int capacity = Math.max(tableSizeFor(setterInfos.size()), 1 << 6);
        setterNodes = new SetterNode[capacity];

        Set<String> setterKeys = setterInfos.keySet();
        for (String key : setterKeys) {
            SetterInfo setterInfo = setterInfos.get(key);
            int keyHash = key.hashCode();
            int index = keyHash & capacity - 1;

            SetterNode setterNode = new SetterNode(key.toCharArray(), setterInfo);
            SetterNode oldNode = setterNodes[index];
            setterNodes[index] = setterNode;
            if (oldNode != null) {
                // hash冲突或者索引冲突
                setterNode.next = oldNode;
            }
        }
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

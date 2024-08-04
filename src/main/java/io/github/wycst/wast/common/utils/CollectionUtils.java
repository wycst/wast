package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 集合和数组类工具方法
 *
 * @Author: wangy
 * @Date: 2020/3/25 0:09
 * @Description:
 */
public final class CollectionUtils {

    public static List listOf(Object... elements) {
        List list = new ArrayList();
        for (Object element : elements) {
            list.add(element);
        }
        return list;
    }

    public static Set setOf(Object... elements) {
        Set set = new HashSet();
        for (Object element : elements) {
            set.add(element);
        }
        return set;
    }

    /**
     * 返回数组中元素等于obj的索引位置,如果没有找到返回-1
     *
     * @param arr
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> int indexOf(T[] arr, T obj) {
        return indexOf(arr, obj, 0);
    }

    /**
     * 从指定位置开始，向后查找数组中等于obj的元素,如果没有找到返回-1
     *
     * @param arr
     * @param obj
     * @param fromIndex
     * @param <T>
     * @return
     */
    public static <T> int indexOf(T[] arr, T obj, int fromIndex) {
        if (arr == null || obj == null) return -1;
        T e;
        for (int i = fromIndex; i < arr.length; i++) {
            e = arr[i];
            if (e == obj || obj.equals(e)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从指定区间，从左开始查找数组中等于obj的元素，如果没有找到返回-1
     *
     * @param arr
     * @param obj
     * @param fromIndex
     * @param toIndex
     * @param <T>
     * @return
     */
    public static <T> int indexOf(T[] arr, T obj, int fromIndex, int toIndex) {
        if (arr == null || obj == null) return -1;
        T e;
        for (int i = fromIndex; i < toIndex; i++) {
            e = arr[i];
            if (e == obj || obj.equals(e)) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(int[] arr, int j) {
        return indexOf(arr, j, 0);
    }

    public static int indexOf(int[] arr, int j, int fromIndex) {
        if (arr == null) return -1;
        for (int i = fromIndex; i < arr.length; i++) {
            if (arr[i] == j) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(int[] arr, int j, int fromIndex, int toIndex) {
        if (arr == null) return -1;
        for (int i = fromIndex; i < toIndex; i++) {
            if (arr[i] == j) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(char[] arr, char j) {
        return indexOf(arr, j, 0, arr.length);
    }

    public static int indexOf(char[] arr, char j, int fromIndex) {
        return indexOf(arr, j, fromIndex, arr.length);
    }

    public static int indexOf(char[] arr, char j, int fromIndex, int toIndex) {
        if (arr == null) return -1;
        for (int i = fromIndex; i < toIndex; i++) {
            if (arr[i] == j) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 判断对象是否为集合类型
     *
     * @param target
     * @return
     */
    public static boolean isCollection(Object target) {
        if (target == null) return false;
        if (target.getClass().isArray()) {
            return true;
        }
        return target instanceof Collection;
    }

    /**
     * 判断集合中是否包含obj
     *
     * @param arr
     * @param obj
     * @return
     */
    public static boolean contains(Object[] arr, Object obj) {
        return indexOf(arr, obj) > -1;
    }

    /**
     * 判断集合类是否为null或者空
     *
     * @param collection
     * @return
     */
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    /**
     * 判断集合类是否为null或者空
     *
     * @param arr
     * @return
     */
    public static boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * 获取集合类的大小
     *
     * @param target
     * @return
     */
    public static int getSize(Object target) {
        if (target == null) return 0;
        if (target instanceof Collection) {
            return ((Collection<?>) target).size();
        }
        if (target instanceof Object[]) {
            return ((Object[]) target).length;
        }
        return Array.getLength(target);
    }

    /**
     * 获取集合中指定位置的索引，如果不是集合抛出空指针
     *
     * @param target
     * @param index
     * @return
     */
    public static Object getElement(Object target, int index) {
        if (target == null) return null;
        Object[] arr = null;
        if (target instanceof Collection) {
            if (target instanceof List) {
                return ((List<?>) target).get(index);
            }
            arr = ((Collection<?>) target).toArray();
        } else if (arr instanceof Object[]) {
            arr = (Object[]) target;
        } else {
            try {
                return UnsafeHelper.arrayValueAt(target, index);
            } catch (RuntimeException throwable) {
                throw new UnsupportedOperationException("Non array object do not support get value by index, " + target.getClass());
            }
        }
        return arr[index];
    }

    public static void setElement(Object target, int index, Object value) {
        if (target == null) return;
        if (target instanceof Collection) {
            if (target instanceof List) {
                ((List) target).set(index, value);
            } else {
                throw new UnsupportedOperationException("不支持的集合赋值操作");
            }
        } else {
            Object[] arr;
            arr = (Object[]) target;
            arr[index] = value;
        }
    }

    /**
     * 集合转化为指定组件的数组
     *
     * @param collection
     * @param componentType
     * @return
     */
    public static Object toArray(Collection collection, Class<?> componentType) {
        componentType.getClass();
        Object array = Array.newInstance(componentType, collection.size());
        int k = 0;
        ReflectConsts.PrimitiveType primitiveType = ReflectConsts.PrimitiveType.typeOf(componentType);
        if (primitiveType != null) {
            for (Object obj : collection) {
                primitiveType.setElementAt(array, k++, obj);
            }
        } else {
            Object[] objects = (Object[]) array;
            for (Object obj : collection) {
                objects[k++] = obj;
            }
        }
        return array;
    }

}

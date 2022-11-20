package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

/**
 * 集合和数组类工具方法
 *
 * @Author: wangy
 * @Date: 2020/3/25 0:09
 * @Description:
 */
public class CollectionUtils {

//    public static int indexOf(Object[] arr, Object obj) {
//        if (arr == null || obj == null) return -1;
//        Object e = null;
//        for (int i = 0; i < arr.length; i++) {
//            e = arr[i];
//            if (e == obj || obj.equals(e)) {
//                return i;
//            }
//        }
//        return -1;
//    }

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
     * 注：jdk 1.8 已有类似的方法
     *
     * @param arr
     * @param delimiter
     * @return
     */
    public static String join(Object[] arr, String delimiter) {
        if (arr == null) return null;
        if (delimiter == null) delimiter = "";
        StringBuffer buffer = new StringBuffer();
        boolean appendDelimiterFlag = false;
        for (Object val : arr) {
            buffer.append(val).append(delimiter);
            appendDelimiterFlag = true;
        }
        if (appendDelimiterFlag) {
            buffer.delete(buffer.length() - delimiter.length(), buffer.length());
        }
        return buffer.toString();
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
     * @param groups
     * @return
     */
    public static boolean isEmpty(Collection groups) {
        return groups == null || groups.size() == 0;
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
        if(target instanceof Object[]) {
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
            if(target instanceof List) {
                return ((List<?>) target).get(index);
            }
            arr = ((Collection<?>) target).toArray();
        } else if(arr instanceof Object[]) {
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
            Object[] arr = null;
            arr = (Object[]) target;
            arr[index] = value;
        }
    }
}

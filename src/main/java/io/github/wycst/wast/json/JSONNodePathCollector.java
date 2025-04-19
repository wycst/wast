package io.github.wycst.wast.json;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.utils.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * xpath语法支持（仅仅支持以下简单语法，部分语法进行了语义替换，xpath复杂的语法不考虑实现,可以自定义实现JSONNodePathCollector）
 * </p>
 *
 * <h4>路径分隔语法(和xpath一致)</h4>
 * <li>"//" :  从当前节点开始查找所有满足条件的节点，并递归所有子孙节点；</li>
 * <li>"/"  :  仅从当前节点开始查找所有满足条件的节点（不递归）；</li>
 *
 * <h4>路径和属性匹配语法(xpath改进方便识别解析)</h4>
 * <li>  *         :  任意匹配；</li>
 * <li> abc*       :  仅仅支持对象节点查找，匹配前缀为abc；</li>
 * <li> *abc       :  仅仅支持对象节点查找，匹配后缀为abc；</li>
 * <li> *abc*      :  仅仅支持对象节点查找，匹配包含abc；</li>
 * <li> ^xxx       :  仅仅支持对象节点查找，匹配正则表达式；</li>
 * <li> n+         :  仅仅支持数组节点查找，匹配索引大于等于n；</li>
 * <li> n-         :  仅仅支持数组节点查找，匹配索引小于等于n；</li>
 * <li> n~m        :  仅仅支持数组节点查找，匹配索引大于等于n且小于等于m；</li>
 * <li> [n,n1,n2]  :  仅仅支持数组节点查找，匹配索引存在集合；</li>
 * <li> '*BC*[SDA' :  仅仅支持对象节点查找,且精确匹配*BC*[SDA；</li>
 * <h4>路径后面统一支持过滤表达式使用中括号环绕，表达式语法参考Expression</h4>
 *
 * @Date 2024/10/10 22:17
 * @Created by wangyc
 * @see Expression
 */
public abstract class JSONNodePathCollector {

    protected Serializable path;
    protected boolean recursive;
    JSONNodePathCollector next;
    JSONNodePathFilter filter;

    public JSONNodePathCollector() {
        this(null);
    }

    public JSONNodePathCollector(Serializable path) {
        this.path = path;
    }

    final JSONNodePathCollector self() {
        return this;
    }

    final boolean isSupportedExtract() {
        return !recursive && filter == null;
    }

    /**
     * Path syntax: '//' or '/'
     *
     * @param recursive '//' if true, '/' otherwise
     * @return
     */
    public final JSONNodePathCollector recursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public final JSONNodePathCollector filter(JSONNodePathFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * 表达式过滤
     *
     * @param condition
     * @return
     */
    public final static JSONNodePathCollector filter(String condition) {
        return JSONNodePathCollector.any().filter(JSONNodePathFilter.condition(condition));
    }

    /**
     * 表达式过滤
     *
     * @param condition
     * @return
     */
    public final JSONNodePathCollector condition(String condition) {
        return filter(JSONNodePathFilter.condition(condition));
    }

    /**
     * 表达式过滤
     *
     * @param expression
     * @return
     */
    public final JSONNodePathCollector condition(Expression expression) {
        return filter(JSONNodePathFilter.expression(expression));
    }

    public final boolean leafPath() {
        return next == null;
    }

    /**
     * 判断是否匹配成功
     *
     * @param index
     * @param size
     * @param target 父节点为数组，且下标值为index的目标节点
     * @return
     */
    protected abstract boolean matched(int index, int size, JSONNode target);

    /**
     * 判断是否匹配成功
     *
     * @param field
     * @param target 父节点为对象，名称为${field}的目标节点
     * @return
     */
    protected abstract boolean matched(String field, JSONNode target);

    /**
     * 执行过滤操作
     *
     * @param target
     * @return
     */
    protected final boolean doFilter(JSONNode target) {
        if (filter == null) return true;
        return filter.doFilter(target);
    }

    final <T> void addIfRecord(JSONNode node, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCtx) {
        if (node.collectCtx != collectCtx) {
            if (collector.filter(node)) {
                node.collectCtx = collectCtx;
                results.add(collector.map(node));
            }
        }
    }

    protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, final JSONNodePathCtx collectCx) {
        if (parentNode.leaf) return;
        parentNode.ensureCompleted(!recursive);
        final boolean leafPath = leafPath();
        if (parentNode.array) {
            for (int i = 0, size = parentNode.elementSize; i < size; ++i) {
                JSONNode value = parentNode.elementValues[i];
                if (matched(i, size, value) && doFilter(value)) {
                    if (leafPath) {
                        addIfRecord(value, results, collector, collectCx);
                    } else {
                        next.collect(value, results, collector, collectCx);
                    }
                }
                if (recursive) {
                    collect(value, results, collector, collectCx);
                }
            }
        } else {
            Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
            Set<Map.Entry<Serializable, JSONNode>> entrySet = fieldValues.entrySet();
            for (Map.Entry<Serializable, JSONNode> entry : entrySet) {
                String field = (String) entry.getKey();
                JSONNode value = entry.getValue();
                if (matched(field, value) && doFilter(value)) {
                    if (leafPath) {
                        addIfRecord(value, results, collector, collectCx);
                    } else {
                        next.collect(value, results, collector, collectCx);
                    }
                }
                if (recursive) {
                    collect(value, results, collector, collectCx);
                }
            }
        }
    }

    JSONNodePathCollector cloneCurrent() {
        return this;
    }

    public final JSONNodePathCollector clone() {
        JSONNodePathCollector pathCollector = cloneCurrent();
        pathCollector.recursive = recursive;
        pathCollector.filter = filter;
        return pathCollector;
    }

    final JSONNodePathCollector chainable(JSONNodePathCollector prev) {
        JSONNodePathCollector cloneFragment = clone();
        prev.next = cloneFragment;
        return prev;
    }

    public final String toString() {
        String pathStr = toPathString();
        String result = recursive ? "//" + pathStr : "/" + pathStr;
        if (filter != null) {
            result += filter.toString();
        }
        return result;
    }

    protected String toPathString() {
        return "";
    }

    protected int matchedObjectField(String key) {
        return -1;
    }

    protected boolean preparedSize() {
        return false;
    }

    /**
     * 返回匹配结果
     *
     * @param index
     * @param size
     * @return 如果没有匹配到返回-1，如果匹配到了并且能确定是最后一个匹配到的则返回1，否则返回0
     */
    protected int matchedArrayIndex(int index, int size) {
        return -1;
    }

    protected boolean isExtract() {
        return false;
    }

    static abstract class PathInternalImpl extends JSONNodePathCollector {

        public PathInternalImpl(Serializable path) {
            super(path);
        }

        public PathInternalImpl() {
        }

        @Override
        protected final boolean matched(int index, int size, JSONNode target) {
            return false;
        }

        @Override
        protected final boolean matched(String field, JSONNode target) {
            return false;
        }
    }

    final static class AnyPathImpl extends PathInternalImpl {

        @Override
        protected int matchedObjectField(String key) {
            return 0;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            return 0;
        }

        @Override
        protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCx) {
            if (parentNode.leaf) return;
            parentNode.ensureCompleted(!recursive);
            final boolean leafPath = leafPath();
            if (parentNode.array) {
                for (int i = 0, size = parentNode.elementSize; i < size; ++i) {
                    JSONNode value = parentNode.elementValues[i];
                    if (doFilter(value)) {
                        if (leafPath) {
                            addIfRecord(value, results, collector, collectCx);
                            if (recursive) {
                                collect(value, results, collector, collectCx);
                            }
                        } else {
                            next.collect(value, results, collector, collectCx);
                        }
                    } else {
                        if (recursive) {
                            collect(value, results, collector, collectCx);
                        }
                    }
                }
            } else {
                Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
                for (JSONNode value : fieldValues.values()) {
                    if (doFilter(value)) {
                        if (leafPath) {
                            addIfRecord(value, results, collector, collectCx);
                            if (recursive) {
                                collect(value, results, collector, collectCx);
                            }
                        } else {
                            next.collect(value, results, collector, collectCx);
                        }
                    } else {
                        if (recursive) {
                            collect(value, results, collector, collectCx);
                        }
                    }
                }
            }
        }

        @Override
        protected String toPathString() {
            return "*";
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new AnyPathImpl();
        }
    }

    final static class ExactImpl extends PathInternalImpl {

        final boolean preparedSize;
        final byte[] pathBytes;

        public ExactImpl(Serializable path) {
            super(path);
            this.preparedSize = path instanceof Integer && (Integer) path < 0;
            this.pathBytes = JSONUnsafe.getStringUTF8Bytes(path.toString());
        }

        @Override
        protected int matchedObjectField(String key) {
            return path.toString().equals(key) ? 1 : -1;
        }

        @Override
        protected boolean isExtract() {
            return true;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            if (path instanceof Integer) {
                int val = (Integer) path;
                if (val < 0) {
                    val += size;
                }
                return val == index ? 1 : -1;
            }
            return -1;
        }

        @Override
        protected boolean preparedSize() {
            return preparedSize;
        }

        protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCx) {
            if (parentNode.leaf) return;
            final boolean leafPath = leafPath();
            if (recursive) {
                parentNode.ensureCompleted(false, false);
                if (parentNode.array) {
                    int size = parentNode.elementSize, index = path instanceof Integer ? (Integer) path : size;
                    if (index < 0) {
                        index += size;
                    }
                    for (int i = 0; i < size; ++i) {
                        JSONNode value = parentNode.elementValues[i];
                        if (i == index && doFilter(value)) {
                            if (leafPath) {
                                addIfRecord(value, results, collector, collectCx);
                            } else {
                                next.collect(value, results, collector, collectCx);
                            }
                            if (!collectCx.greedy) {
                                continue;
                            }
                        }
                        collect(value, results, collector, collectCx);
                    }
                } else {
                    Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
                    Set<Map.Entry<Serializable, JSONNode>> entrySet = fieldValues.entrySet();
                    for (Map.Entry<Serializable, JSONNode> entry : entrySet) {
                        String field = (String) entry.getKey();
                        JSONNode value = entry.getValue();
                        if (field.equals(path.toString()) && doFilter(value)) {
                            if (leafPath) {
                                addIfRecord(value, results, collector, collectCx);
                            } else {
                                next.collect(value, results, collector, collectCx);
                            }
                            if (!collectCx.greedy) {
                                continue;
                            }
                        }
                        collect(value, results, collector, collectCx);
                    }
                }
            } else {
                JSONNode target;
                if (parentNode.array) {
                    if (!(path instanceof Integer)) {
                        return;
                    }
                    int index = (Integer) path;
                    if (index < 0) {
                        parentNode.ensureCompleted(true);
                        index += parentNode.elementSize;
                    }
                    target = parentNode.getElementAt(index);
                } else {
                    target = parentNode.getFieldNodeAt(path.toString());
                }
                if (target == null) return;
                if (doFilter(target)) {
                    if (leafPath) {
                        addIfRecord(target, results, collector, collectCx);
                    } else {
                        next.collect(target, results, collector, collectCx);
                    }
                }
            }
        }

        @Override
        protected String toPathString() {
            return String.valueOf(path);
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new ExactImpl(path);
        }
    }

    static abstract class ObjectNodeImpl extends PathInternalImpl {
        public ObjectNodeImpl(Serializable path) {
            super(path);
        }

        protected abstract boolean matched(String field, String path);

        protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCx) {
            if (parentNode.leaf) return;
            String str = path.toString().trim();
            final boolean leafPath = leafPath();
            if (parentNode.isObject()) {
                parentNode.ensureCompleted(!recursive);
                Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
                Set<Map.Entry<Serializable, JSONNode>> entrySet = fieldValues.entrySet();
                for (Map.Entry<Serializable, JSONNode> entry : entrySet) {
                    String field = entry.getKey().toString();
                    JSONNode value = entry.getValue();
                    if (matched(field, str) && doFilter(value)) {
                        if (leafPath) {
                            addIfRecord(value, results, collector, collectCx);
                        } else {
                            next.collect(value, results, collector, collectCx);
                        }
                        if (!collectCx.greedy) {
                            continue;
                        }
                    }
                    if (recursive) {
                        collect(value, results, collector, collectCx);
                    }
                }
            } else {
                if (recursive) {
                    parentNode.ensureCompleted(false);
                    for (int size = parentNode.elementSize, i = 0; i < size; ++i) {
                        JSONNode value = parentNode.elementValues[i];
                        collect(value, results, collector, collectCx);
                    }
                }
            }
        }
    }

    static abstract class ArrayNodeImpl extends PathInternalImpl {

        public abstract void parse(JSONNode parentNode);

        public int from(int size) {
            return 0;
        }

        public int to(int size) {
            return size;
        }

        protected abstract boolean matched(int index, int size);

        protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCx) {
            if (parentNode.leaf) return;
            final boolean leafPath = leafPath();
            if (parentNode.array) {
                if (recursive) {
                    parentNode.ensureCompleted(false);
                    for (int i = 0, size = parentNode.elementSize; i < size; ++i) {
                        JSONNode value = parentNode.elementValues[i];
                        if (matched(i, size) && doFilter(value)) {
                            if (leafPath) {
                                addIfRecord(value, results, collector, collectCx);
                            } else {
                                next.collect(value, results, collector, collectCx);
                            }
                            if (!collectCx.greedy) {
                                continue;
                            }
                        }
                        collect(value, results, collector, collectCx);
                    }
                } else {
                    parse(parentNode);
                    for (int size = parentNode.elementSize, i = from(size), to = to(size); i < to; ++i) {
                        JSONNode value = parentNode.elementValues[i];
                        if (matched(i, size) && doFilter(value)) {
                            if (leafPath) {
                                addIfRecord(value, results, collector, collectCx);
                            } else {
                                next.collect(value, results, collector, collectCx);
                            }
                        }
                    }
                }
            } else {
                if (recursive) {
                    parentNode.ensureCompleted(false);
                    Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
                    for (JSONNode value : fieldValues.values()) {
                        collect(value, results, collector, collectCx);
                    }
                }
            }
        }
    }


    final static class PrefixImpl extends ObjectNodeImpl {
        public PrefixImpl(Serializable path) {
            super(path);
        }

        @Override
        protected boolean matched(String field, String path) {
            return field.startsWith(path);
        }

        @Override
        protected int matchedObjectField(String key) {
            return key.startsWith((String) path) ? 0 : -1;
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new PrefixImpl(path);
        }

        @Override
        protected String toPathString() {
            return path + "*";
        }
    }

    final static class SuffixImpl extends ObjectNodeImpl {
        public SuffixImpl(Serializable path) {
            super(path);
        }

        @Override
        protected boolean matched(String field, String path) {
            return field.endsWith(path);
        }

        @Override
        protected int matchedObjectField(String key) {
            return key.endsWith((String) path) ? 0 : -1;
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new SuffixImpl(path);
        }

        @Override
        protected String toPathString() {
            return "*" + path;
        }
    }

    final static class ContainsImpl extends ObjectNodeImpl {
        public ContainsImpl(Serializable path) {
            super(path);
        }

        @Override
        protected boolean matched(String field, String path) {
            return field.contains(path);
        }

        @Override
        protected int matchedObjectField(String key) {
            return key.contains((String) path) ? 0 : -1;
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new ContainsImpl(path);
        }

        @Override
        protected String toPathString() {
            return "*" + path + "*";
        }
    }

    // 正则
    final static class RegularImpl extends ObjectNodeImpl {

        final Pattern pattern;

        public RegularImpl(String path) {
            this(Pattern.compile(path), path);
        }

        RegularImpl(Pattern pattern, Serializable path) {
            super(path);
            this.pattern = pattern;
        }

        @Override
        protected boolean matched(String field, String path) {
            try {
                return pattern.matcher(field).matches();
            } catch (Throwable throwable) {
                return false;
            }
        }

        @Override
        protected int matchedObjectField(String key) {
            try {
                return pattern.matcher(key).matches() ? 0 : -1;
            } catch (Throwable throwable) {
                return -1;
            }
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new RegularImpl(pattern, path);
        }

        @Override
        protected String toPathString() {
            return '(' + String.valueOf(path) + ')';
        }
    }

    final static class GEImpl extends ArrayNodeImpl {
        final int minIndexValue;
        final boolean preparedSize;

        public GEImpl(int minIndexValue) {
            this.minIndexValue = minIndexValue;
            this.preparedSize = minIndexValue < 0;
        }

        @Override
        public int from(int size) {
            return minIndexValue < 0 ? Math.max(minIndexValue + size, 0) : minIndexValue;
        }

        @Override
        protected boolean matched(int index, int size) {
            int targetIndex = minIndexValue < 0 ? minIndexValue + size : minIndexValue;
            return index >= targetIndex;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            return matched(index, size) ? 0 : -1;
        }

        @Override
        protected boolean preparedSize() {
            return preparedSize;
        }

        @Override
        public void parse(JSONNode parentNode) {
            parentNode.parseElementTo(-1);
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new GEImpl(minIndexValue);
        }

        @Override
        protected String toPathString() {
            return String.valueOf(minIndexValue) + '+';
        }
    }

    final static class LEImpl extends ArrayNodeImpl {
        final int maxIndexValue;
        final boolean preparedSize;

        public LEImpl(int maxIndexValue) {
            this.maxIndexValue = maxIndexValue;
            this.preparedSize = maxIndexValue < 0;
        }

        @Override
        public int to(int size) {
            return maxIndexValue < 0 ? maxIndexValue + size + 1 : Math.min(maxIndexValue + 1, size);
        }

        @Override
        protected boolean preparedSize() {
            return preparedSize;
        }

        @Override
        protected boolean matched(int index, int size) {
            int targetIndex = maxIndexValue;
            if (targetIndex < 0) {
                targetIndex += size;
            }
            return index <= targetIndex;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            int mv = maxIndexValue > 0 ? maxIndexValue : maxIndexValue + size;
            if (index == mv) return 1;
            return index < maxIndexValue ? 0 : -1;
        }

        @Override
        public void parse(JSONNode parentNode) {
            parentNode.parseElementTo(maxIndexValue);
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new LEImpl(maxIndexValue);
        }

        @Override
        protected String toPathString() {
            return String.valueOf(maxIndexValue) + '-';
        }
    }

    // 范围(连续)
    final static class RangeImpl extends ArrayNodeImpl {
        final int from;
        final int to;
        final boolean preparedSize;

        public RangeImpl(int from, int to) {
            this.from = from;
            this.to = to;
            this.preparedSize = from < 0 || to < 0;
        }

        @Override
        public int from(int size) {
            if (from < 0) {
                return Math.max(from + size, 0);
            } else {
                return from;
            }
        }

        @Override
        public int to(int size) {
            if (to < 0) {
                return to + size + 1;
            } else {
                return Math.min(to + 1, size);
            }
        }

        @Override
        protected boolean matched(int index, int size) {
            int startIndex = from < 0 ? from + size : from, endIndex = to < 0 ? to + size : to;
            return index >= startIndex && index <= endIndex;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            int startIndex = from < 0 ? from + size : from, endIndex = to < 0 ? to + size : to;
            boolean rec = index >= startIndex && index <= endIndex;
            if (rec) {
                return index == endIndex ? 1 : 0;
            } else {
                return -1;
            }
        }

        @Override
        protected boolean preparedSize() {
            return preparedSize;
        }

        @Override
        public void parse(JSONNode parentNode) {
            parentNode.parseElementTo(to < 0 ? -1 : to);
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new RangeImpl(from, to);
        }

        @Override
        protected String toPathString() {
            return from + "~" + to;
        }
    }

    // 离散数组下标例如[1,5,7]
    final static class DiscreteIndexImpl extends PathInternalImpl {
        final Integer[] indexs;
        final boolean preparedSize;
        final int maxPositiveIndex;

        public DiscreteIndexImpl(Integer[] indexs) {
            this.indexs = indexs;
            boolean preparedSize = false;
            int maxIndex = 0;
            for (Integer index : indexs) {
                if (index < 0) {
                    preparedSize = true;
                } else {
                    maxIndex = Math.max(maxIndex, index);
                }
            }
            this.preparedSize = preparedSize;
            this.maxPositiveIndex = maxIndex;
        }

        @Override
        protected <T> void collect(JSONNode parentNode, Collection<T> results, JSONNodeCollector<T> collector, JSONNodePathCtx collectCx) {
            if (parentNode.leaf) return;
            final boolean leafPath = leafPath();
            if (recursive) {
                parentNode.ensureCompleted(false);
                if (parentNode.array) {
                    for (int size = parentNode.elementSize, i = 0; i < size; ++i) {
                        JSONNode value = parentNode.elementValues[i];
                        if (matched(i, size) && doFilter(value)) {
                            if (leafPath) {
                                addIfRecord(value, results, collector, collectCx);
                            } else {
                                next.collect(value, results, collector, collectCx);
                            }
                            if (!collectCx.greedy) {
                                continue;
                            }
                        }
                        collect(value, results, collector, collectCx);
                    }
                } else {
                    Map<Serializable, JSONNode> fieldValues = parentNode.fieldValues;
                    for (JSONNode value : fieldValues.values()) {
                        collect(value, results, collector, collectCx);
                    }
                }
            } else {
                for (Integer index : indexs) {
                    JSONNode value = parentNode.getElementAt(index);
                    if (value != null) {
                        if (leafPath) {
                            addIfRecord(value, results, collector, collectCx);
                        } else {
                            next.collect(value, results, collector, collectCx);
                        }
                    }
                }
            }
        }

        protected boolean matched(int i, int size) {
            for (Integer index : indexs) {
                if (index == i || (index + size) == i) return true;
            }
            return false;
        }

        @Override
        protected int matchedArrayIndex(int index, int size) {
            if (!preparedSize && index == maxPositiveIndex) {
                return 1;
            }
            return matched(index, size) ? 0 : -1;
        }

        @Override
        protected boolean preparedSize() {
            return preparedSize;
        }

        @Override
        JSONNodePathCollector cloneCurrent() {
            return new DiscreteIndexImpl(indexs);
        }

        @Override
        protected String toPathString() {
            return '[' + StringUtils.join(",", indexs) + ']';
        }
    }

    public final static JSONNodePathCollector any() {
        return new AnyPathImpl();
    }

    public final static JSONNodePathCollector exact(Serializable path) {
        return new ExactImpl(path);
    }

    public final static JSONNodePathCollector prefix(Serializable path) {
        return new PrefixImpl(path);
    }

    public final static JSONNodePathCollector suffix(Serializable path) {
        return new SuffixImpl(path);
    }

    public final static JSONNodePathCollector contains(Serializable path) {
        return new ContainsImpl(path);
    }

    public final static JSONNodePathCollector regular(String path) {
        return new RegularImpl(path);
    }

    public final static JSONNodePathCollector ge(int index) {
        return new GEImpl(index);
    }

    public final static JSONNodePathCollector le(int index) {
        return new LEImpl(index);
    }

    public final static JSONNodePathCollector range(int from, int to) {
        return new RangeImpl(from, to);
    }

    public final static JSONNodePathCollector indexs(Integer... indexs) {
        return new DiscreteIndexImpl(indexs);
    }
}

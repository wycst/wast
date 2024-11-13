package io.github.wycst.wast.json;

/**
 * @Date 2024/10/13 7:53
 * @Created by wangyc
 */
public abstract class JSONNodeCollector<T> {

    public final JSONNodeCollector self() {
        return this;
    }

    public abstract T map(JSONNode node);

    public final static <T> JSONNodeCollector<T> of(final Class<T> targetClass) {
        targetClass.getClass();
        return new JSONNodeCollector<T>() {
            @Override
            public T map(JSONNode node) {
                return node.getValue(targetClass);
            }
        };
    }

    static class LeafWrapImpl<T> extends JSONNodeCollector<T> {
        final JSONNodeCollector<T> collector;

        LeafWrapImpl(JSONNodeCollector<T> collector) {
            this.collector = collector;
        }

        @Override
        public T map(JSONNode node) {
            return collector.map(node);
        }

        public boolean filter(JSONNode node) {
            if (!node.leaf) return false;
            return collector.filter(node);
        }
    }

    /**
     * 是否只收集叶子节点（对象和数组类型忽略）
     *
     * @return
     */
    public JSONNodeCollector onlyCollectLeaf() {
        return this instanceof LeafWrapImpl ? this : new LeafWrapImpl(this);
    }

    public boolean filter(JSONNode node) {
        return true;
    }

    public final static JSONNodeCollector<JSONNode> DEFAULT = new JSONNodeCollector<JSONNode>() {
        @Override
        public JSONNode map(JSONNode node) {
            return node;
        }
    };

    public final static JSONNodeCollector<Object> ANY = new JSONNodeCollector<Object>() {
        @Override
        public Object map(JSONNode node) {
            return node.any();
        }
    };
}

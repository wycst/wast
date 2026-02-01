package io.github.wycst.wast.jdbc.executer;

import io.github.wycst.wast.common.utils.ObjectUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class CacheableEntityHandler extends EntityHandler {

    CacheableEntityHandler(EntitySqlMapping entitySqlMapping) {
        super(entitySqlMapping);
    }

    Map<Serializable, CacheableEntity> cachedEntityMap = new ConcurrentHashMap<Serializable, CacheableEntity>();

    boolean cachedAllFlag = false;
    List<?> cachedFullEntityList = null;

    private final Object lock = new Object();

    @Override
    <E> E getById(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Serializable id) {
        CacheableEntity cacheableEntity = cachedEntityMap.get(id);
        if (cacheableEntity == null) {
            return putOrRemove(sqlExecuter, entityCls, id);
        }
        return (E) cacheableEntity.get();
    }

    @Override
    <E> int deleteById(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Serializable id) {
        try {
            return super.deleteById(sqlExecuter, entityCls, id);
        } finally {
            removeCacheEntity(id);
        }
    }

    void removeCacheEntity(Serializable id) {
        synchronized (lock) {
            cachedEntityMap.remove(id);
            cachedFullEntityList = null;
        }
    }

    @Override
    <E> List<E> executeQueryBy(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Object params) {
        List<E> result;
        boolean emptyParams = (params == null || ObjectUtils.getNonEmptyFields(params).isEmpty());
        if (cachedAllFlag && emptyParams) {
            synchronized (lock) {
                if (cachedFullEntityList == null) {
                    result = new ArrayList<E>();
                    Collection<CacheableEntity> cacheableEntities = cachedEntityMap.values();
                    for (CacheableEntity cacheableEntity : cacheableEntities) {
                        result.add((E) cacheableEntity.getEntity());
                    }
                    cachedFullEntityList = result;
                } else {
                    result = new ArrayList<E>((List<E>) cachedFullEntityList);
                }
                return result;
            }
        }
        result = super.executeQueryBy(sqlExecuter, entityCls, params);
        if (emptyParams) {
            this.cacheAll(result);
        }
        return result;
    }

    /**
     * 当存在时加入缓存，如果不存在从缓存中移除
     *
     * @param sqlExecuter
     * @param entityCls
     * @param id
     * @param <E>
     * @return
     */
    <E> E putOrRemove(DefaultSqlExecuter sqlExecuter, Class<E> entityCls, Serializable id) {
        E e = super.getById(sqlExecuter, entityCls, id);
        synchronized (lock) {
            if (e != null) {
                CacheableEntity cacheableEntity = new CacheableEntity(e);
                cachedEntityMap.put(id, cacheableEntity);
            } else {
                cachedEntityMap.remove(id);
            }
            cachedFullEntityList = null;
        }
        return e;
    }

    <E> void cacheAll(List<E> entityList) {
        if (entitySqlMapping.existPrimary()) {
            synchronized (lock) {
                cachedEntityMap.clear();
                for (E e : entityList) {
                    Serializable id = entitySqlMapping.getId(e);
                    CacheableEntity cacheableEntity = new CacheableEntity(e);
                    cachedEntityMap.put(id, cacheableEntity);
                }
                cachedAllFlag = true;
            }
        }
    }

    /**
     * 清除缓存
     */
    void resetCaches() {
        synchronized (lock) {
            cachedEntityMap.clear();
            cachedAllFlag = false;
            cachedFullEntityList = null;
        }
    }

    @Override
    <E> int updateEntity(DefaultSqlExecuter sqlExecuter, E entity) {
        int n = -1;
        try {
            n = super.updateEntity(sqlExecuter, entity);
            return n;
        } finally {
            if (n == 1) {
                afterUpdate(sqlExecuter, entity);
            }
        }
    }

    @Override
    <E> void afterUpdate(DefaultSqlExecuter sqlExecuter, E entity) {
        Serializable id = entitySqlMapping.getId(entity);
        putOrRemove(sqlExecuter, entity.getClass(), id);
    }

    @Override
    void afterBatchDelete() {
        resetCaches();
    }

    /**
     * 清除过期的缓存
     */
    void clearExpiredCaches() {
        final long expires = entitySqlMapping.expires();
        long now = System.currentTimeMillis();

        synchronized (lock) {
            Set<Serializable> cacheKeys = new HashSet<Serializable>(cachedEntityMap.keySet());
            int removeCnt = 0;
            for (Serializable id : cacheKeys) {
                CacheableEntity cacheableEntity = cachedEntityMap.get(id);
                if (cacheableEntity != null) {
                    long lastHitAt = cacheableEntity.getLastHitAt();
                    if (now - lastHitAt >= expires) {
                        cachedEntityMap.remove(id);
                        ++removeCnt;
                    }
                }
            }

            if (removeCnt > 0) {
                cachedAllFlag = false;
                cachedFullEntityList = null;
            }
        }
    }
}
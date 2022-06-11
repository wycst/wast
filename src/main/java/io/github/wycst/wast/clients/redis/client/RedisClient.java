package io.github.wycst.wast.clients.redis.client;

import io.github.wycst.wast.clients.redis.data.entry.KeyValueEntry;
import io.github.wycst.wast.clients.redis.data.entry.ScanEntry;
import io.github.wycst.wast.clients.redis.data.entry.UnionSortedSet;
import io.github.wycst.wast.clients.redis.data.entry.ZSetMember;
import io.github.wycst.wast.clients.redis.connection.RedisConnection;
import io.github.wycst.wast.clients.redis.exception.RedisException;
import io.github.wycst.wast.clients.redis.exception.RedisInvalidException;
import io.github.wycst.wast.clients.redis.listener.Subscriber;
import io.github.wycst.wast.clients.redis.options.*;
import io.github.wycst.wast.clients.redis.options.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * redis客户端抽象实现
 *
 * @Author: wangy
 * @Date: 2020/5/19 23:43
 * @Description:
 */
public abstract class RedisClient extends ClientCommander {

    public void auth(String password) {
        RedisConnection redisConnection = getConnection();
        redisConnection.auth(password);
    }

    public void select(int database) {
        RedisConnection redisConnection = getConnection();
        redisConnection.select(database);
    }

    public long hashSet(final String key, final String field, final Object value) {
        checkIfNull(key, field);
        String[] commands = commands("HSET", key, field, ifNullValue(value, ""));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public boolean hashSetnx(String key, String field, Object value) {
        checkIfNull(key, field);
        String[] commands = commands("HSETNX", key, field, ifNullValue(value, ""));
        return executeRedisCommand(commands, Long.class, 0L) == 1;
    }

    public void hashMultiSet(String key, Map<String, ? extends Object> values) {
        checkIfNull(key);
        if (values == null || values.size() == 0)
            return;
        List<String> commandList = new ArrayList<String>();
        commandList.add("HMSET");
        commandList.add(key);
        String[] commands = appendCommands(commandList, values);
        executeRedisCommand(commands, String.class, null);
    }

    public String hashGet(String key, String field) {
        checkIfNull(key, field);
        String[] commands = commands("HGET", key, field);
        return executeRedisCommand(commands, String.class, null);
    }

    public List<String> hashMultiGet(String key, String... fields) {
        // HMGET key field1,field2,field3
        checkIfNull(key);
        int fieldCount = fields.length;
        if (fieldCount == 0) {
            ifThrowException(new RedisInvalidException("[hashMultiGet] call error: ERR wrong number of arguments for 'hmget' command"));
        }
        String[] commands = new String[fieldCount + 2];
        commands[0] = "HMGET";
        commands[1] = key;
        System.arraycopy(fields, 0, commands, 2, fieldCount);
        return executeRedisCommand(commands, List.class, null);
    }

    public Map<String, String> hashGetAll(String key) {
        checkIfNull(key);
        String[] commands = commands("HGETALL", key);
        Map<String, String> values = new LinkedHashMap<String, String>();
        List<String> outLines = executeRedisCommand(commands, List.class, null);
        if (outLines == null) {
            return null;
        }
        int size = outLines.size();
        if (size % 2 == 1) {
            // 理论上不可能出现
            throw new RedisException("Data return exception");
        }
        for (int i = 0; i < size; i += 2) {
            values.put(outLines.get(i), outLines.get(i + 1));
        }
        return values;
    }

    public List<String> hashKeys(String key) {
        checkIfNull(key);
        String[] commands = commands("HKEYS", key);
        return executeRedisCommand(commands, List.class, null);
    }

    public List<String> hashVals(String key) {
        checkIfNull(key);
        String[] commands = commands("HVALS", key);
        return executeRedisCommand(commands, List.class, null);
    }

    public long hashLen(String key) {
        checkIfNull(key);
        String[] commands = commands("HLEN", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long hashDel(String key, String... fields) {
        checkIfNull(key);
        int fieldCount = fields.length;
        if (fieldCount == 0) {
            ifThrowException(new RedisInvalidException("[hashDel] call error: ERR wrong number of arguments for 'hdel' command"));
        }
        String[] commands = new String[fieldCount + 2];
        commands[0] = "HDEL";
        commands[1] = key;
        System.arraycopy(fields, 0, commands, 2, fieldCount);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public boolean hashExists(String key, String field) {
        checkIfNull(key);
        String[] commands = commands("HEXISTS", key, field);
        return executeRedisCommand(commands, Long.class, 0L) == 1;
    }

    public long hashIncrby(String key, String field, long increment) {
        checkIfNull(key, field);
        String[] commands = commands("HASHINCRBY", key, field, String.valueOf(increment));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public double hashIncrbyfloat(String key, String field, double increment) {
        checkIfNull(key, field);
        String[] commands = commands("HASHINCRBYFLOAT", key, field, String.valueOf(increment));
        return executeRedisCommand(commands, Double.class, 0D);
    }

    public ScanEntry hashScan(String key, long cursor) {
        return doScanOperation("HSCAN", key, cursor);
    }

    public ScanEntry hashScan(String key, long cursor, String pattern, long count) {
        return doScanOperation("HSCAN", key, cursor, pattern, count);
    }

    public String dump(String key) {
        checkIfNull(key);
        String[] commands = commands("DUMP", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public void restore(String key, long ttl, String value) {
        checkIfNull(key, value);
        String[] commands = commands("RESTORE", key, String.valueOf(ttl), value);
        String result = executeRedisCommand(commands, String.class, null);
        if (result != null && !"OK".equals(result)) {
            throw new RedisException(result);
        }
    }

    public List<String> sort(String key, SortOptions sortOptions) {
        checkIfNull(key);
        List<String> commandList = null;
        int optionLength = 0;
        if (sortOptions != null) {
            commandList = sortOptions.buildCommands();
            optionLength = commandList.size();
        }
        String[] commands = new String[2 + optionLength];
        commands[0] = "SORT";
        commands[1] = key;
        if (optionLength > 0) {
            System.arraycopy(commandList.toArray(), 0, commands, 2, optionLength);
        }
        return executeRedisCommand(commands, List.class, null);
    }

    public String type(String key) {
        checkIfNull(key);
        return executeRedisCommand(commands("TYPE", key), String.class, null);
    }

    public List<String> keys(String pattern) {
        checkIfNull(pattern);
        String[] commands = commands("KEYS", pattern);
        return executeRedisCommand(commands, List.class, null);
    }

    public ScanEntry scan(long cursor) {
        ScanEntry scanEntry = null;
        String[] commands = commands("SCAN", String.valueOf(cursor));
        List list = executeRedisCommand(commands, List.class, null);
        if (list != null && list.size() == 2) {
            scanEntry = new ScanEntry(Long.parseLong(list.get(0).toString()), (List) list.get(1));
        }
        return scanEntry;
    }

    public ScanEntry scan(long cursor, String pattern, long count) {
        ScanEntry scanEntry = null;
        List<String> commandList = new ArrayList<String>();
        commandList.add("SCAN");
        commandList.add(String.valueOf(cursor));
        if (validateCommand(pattern)) {
            commandList.add("MATCH");
            commandList.add(pattern);
        }
        if (count > 0) {
            commandList.add("COUNT");
            commandList.add(String.valueOf(count));
        }
        String[] commands = commandList.toArray(new String[commandList.size()]);
        List list = executeRedisCommand(commands, List.class, null);
        if (list != null && list.size() == 2) {
            scanEntry = new ScanEntry(Long.parseLong(list.get(0).toString()), (List) list.get(1));
        }
        return scanEntry;
    }

    public void migrate(String host, int port, String key, int dbIndex, int timeout, boolean copySource, boolean replaceDestination) {
        throw new RedisException("不支持MIGRATE命令");

//        checkIfNull(host, key);
//        String[] commands = null;
//        if (copySource || replaceDestination) {
//            if (copySource && replaceDestination) {
//                commands = commands("MIGRATE", host, String.valueOf(port), key, String.valueOf(dbIndex), String.valueOf(timeout), "COPY", "REPLACE");
//            } else {
//                commands = commands("MIGRATE", host, String.valueOf(port), key, String.valueOf(dbIndex), String.valueOf(timeout), copySource ? "COPY" : "REPLACE");
//            }
//        } else {
//            commands = commands("MIGRATE", host, String.valueOf(port), key, String.valueOf(dbIndex), String.valueOf(timeout));
//        }
//
//        String result = executeRedisCommand(commands, String.class, null);
//        if(result != null && !"OK".equals(result)) {
//            throw new RedisException(result);
//        }
    }

    public long move(String key, int dbIndex) {
        checkIfNull(key);
        String[] commands = commands("MOVE", key, String.valueOf(dbIndex));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public void migrate(String host, int port, String key, int dbIndex, int timeout) {
        migrate(host, port, key, dbIndex, timeout, false, false);
    }

    public <E> E object(CommandOptions<E> commandOptions, String key) {
        checkIfNull(key);
        if (commandOptions == null) {
            throw new RedisInvalidException("[object] call error: ERR Unknown subcommand or wrong number of arguments for 'object' command");
        }
        String[] commands = commands("OBJECT", commandOptions.getCode(), key);
        Class<E> typeCls = commandOptions.getTypeCls();
        boolean typeNumber = Number.class.isAssignableFrom(typeCls);
        Object defaultVal = Number.class.isAssignableFrom(typeCls) ? -1 : null;
        E def = (E) defaultVal;

        E result = executeRedisCommand(commands, commandOptions.getTypeCls(), def);
        if (result == null && typeNumber) {
            return def;
        }
        return result;
    }

    public long persist(String key) {
        checkIfNull(key);
        String[] commands = commands("PERSIST", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long del(String... keys) {
        checkIfNull(keys);
        int keyCount = keys.length;
        if (keyCount == 0) {
            ifThrowException(new RedisInvalidException("[del] call error: ERR wrong number of arguments for 'del' command"));
        }
        String[] commands = new String[keyCount + 1];
        commands[0] = "DEL";
        System.arraycopy(keys, 0, commands, 1, keyCount);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public boolean exists(String key) {
        checkIfNull(key);
        String[] commands = commands("EXISTS", key);
        return executeRedisCommand(commands, Long.class, 0L) == 1;
    }

    public boolean rename(String oldKey, String newKey) {
        checkIfNull(oldKey, newKey);
        String[] commands = commands("RENAME", oldKey, newKey);
        return "OK".equals(executeRedisCommand(commands, String.class, null));
    }

    public boolean renamenx(String key, String newKey) {
        checkIfNull(key, newKey);
        String[] commands = commands("RENAMENX", key, newKey);
        return executeRedisCommand(commands, Long.class, 0L) == 1;
    }

    public String randomKey() {
        String[] commands = commands("RANDOMKEY");
        return executeRedisCommand(commands, String.class, null);
    }

    public long expire(String key, long seconds) {
        checkIfNull(key);
        String[] commands = commands("EXPIRE", key, String.valueOf(seconds));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long expireAt(String key, long secondTimestamp) {
        checkIfNull(key);
        String[] commands = commands("EXPIREAT", key, String.valueOf(secondTimestamp));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long pexpire(String key, long milliseconds) {
        checkIfNull(key);
        String[] commands = commands("PEXPIRE", key, String.valueOf(milliseconds));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long pexpireAt(String key, long millisecondTimestamp) {
        checkIfNull(key);
        String[] commands = commands("PEXPIREAT", key, String.valueOf(millisecondTimestamp));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long ttl(String key) {
        checkIfNull(key);
        String[] commands = commands("TTL", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long pttl(String key) {
        checkIfNull(key);
        String[] commands = commands("PTTL", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public void set(String key, Object value) {
        checkIfNull(key);
        String[] commands = commands("SET", key, ifNullValue(value, ""));
        executeRedisCommand(commands, String.class, null);
    }

    public void set(String key, Object value, long milliseconds) {
        checkIfNull(key);
        String[] commands = commands("SET", key, ifNullValue(value, ""), "PX", String.valueOf(milliseconds));
        executeRedisCommand(commands, String.class, null);
    }

    public void set(String key, Object value, long count, TimeUnit timeUnit) {
        checkIfNull(key);
        String[] commands = commands("SET", key, ifNullValue(value, ""), timeUnit == TimeUnit.SECONDS ? "EX" : "PX", String.valueOf(count));
        executeRedisCommand(commands, String.class, null);
    }

    public boolean set(String key, Object value, SetOptions setOptions) {
        checkIfNull(key);
        List<String> commandList = null;
        int optionLength = 0;
        if (setOptions != null) {
            commandList = setOptions.buildCommands();
            optionLength = commandList.size();
        }
        String[] commands = new String[3 + optionLength];
        commands[0] = "SET";
        commands[1] = key;
        commands[2] = ifNullValue(value, "");
        if (optionLength > 0) {
            System.arraycopy(commandList.toArray(), 0, commands, 3, optionLength);
        }
        String resultValue = executeRedisCommand(commands, String.class, null);
        return "OK".equals(resultValue);
    }

    public void psetex(String key, Object value, long milliseconds) {
        // PSETEX key milliseconds value
        checkIfNull(key);
        String result = executeRedisCommand(commands("PSETEX", key, String.valueOf(milliseconds), ifNullValue(value, "")), String.class, null);
        if (result != null) {
            if ("OK".equals(result)) {
                ifThrowException(new RedisException(result));
            }
        }
    }

    public void setex(String key, Object value, long seconds) {
        checkIfNull(key);
        String result = executeRedisCommand(commands("SETEX", key, String.valueOf(seconds), ifNullValue(value, "")), String.class, null);
        if (result != null) {
            if ("OK".equals(result)) {
                ifThrowException(new RedisException(result));
            }
        }
    }

    public long append(String key, Object value) {
        checkIfNull(key);
        String[] commands = commands("APPEND", key, ifNullValue(value, ""));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public String getset(String key, Object value) {
        checkIfNull(key);
        String[] commands = commands("GET", key, ifNullValue(value, ""));
        return executeRedisCommand(commands, String.class, null);
    }

    public long strLen(String key) {
        checkIfNull(key);
        String[] commands = commands("STRLEN", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public String get(String key) {
        checkIfNull(key);
        String[] commands = commands("GET", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public String getRange(String key, int fromIndex, int toIndex) {
        checkIfNull(key);
        String[] commands = commands("GETRANGE", key, String.valueOf(fromIndex), String.valueOf(toIndex));
        return executeRedisCommand(commands, String.class, null);
    }

    public void mset(Map<String, ? extends Object> values) {
        if (values == null || values.size() == 0)
            return;

        List<String> commandList = new ArrayList<String>();
        commandList.add("MSET");
        String[] commands = appendCommands(commandList, values);
        executeRedisCommand(commands, String.class, null);
    }

    public boolean msetnx(Map<String, ? extends Object> values) {
        if (values == null || values.size() == 0)
            return false;

        List<String> commandList = new ArrayList<String>();
        commandList.add("MSETNX");
        String[] commands = appendCommands(commandList, values);
        return 1l == executeRedisCommand(commands, Long.class, 0L);
    }

    public List<String> mget(String... keys) {
        checkIfNull(keys);
        int keyCount = keys.length;
        if (keyCount == 0) {
            ifThrowException(new RedisInvalidException("[mget] call error: ERR wrong number of arguments for 'mget' command"));
        }
        String[] commands = new String[keyCount + 1];
        commands[0] = "MGET";
        System.arraycopy(keys, 0, commands, 1, keyCount);
        return executeRedisCommand(commands, List.class, null);
    }

    public long bitcount(String key) {
        return bitcount(key, 0, -1);
    }

    public long bitcount(String key, int start, int end) {
        checkIfNull(key);
        String[] commands = commands("BITCOUNT", String.valueOf(start), String.valueOf(end));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long bitopAnd(String destkey, String... keys) {
        return doBitop("AND", destkey, keys);
    }

    public long bitopOr(String destkey, String... keys) {
        return doBitop("OR", destkey, keys);
    }

    public long bitopXor(String destkey, String... keys) {
        return doBitop("XOR", destkey, keys);
    }

    public long bitopNot(String destkey, String key) {
        return doBitop("NOT", destkey, key);
    }

    private long doBitop(String op, String destkey, String... keys) {
        checkIfNull(destkey);
        checkIfNull(keys);
        int keysLength = keys.length;
        if (keysLength == 0) {
            ifThrowException(new RedisInvalidException("[BITOP " + op + "] call error: ERR wrong number of arguments for 'BITOP " + op + "' command"));
        }
        String[] commands = new String[keysLength + 3];
        commands[0] = "BITOP";
        commands[1] = op;
        commands[2] = destkey;
        System.arraycopy(keys, 0, commands, 3, keysLength);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long setbit(String key, int offset, int value) {
        checkIfNull(key);
        if (offset < 0) {
            ifThrowException(new RedisInvalidException("ERR setbit offset[" + offset + "] is out of range for Less than 0 "));
        }
        if (value < 0 || value > 1) {
            ifThrowException(new RedisInvalidException("ERR setbit value[" + value + "] is out of range,The Value Can only be 0 or 1 "));
        }
        String[] commands = commands("SETBIT", key, String.valueOf(offset), String.valueOf(value));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long getbit(String key, int offset) {
        checkIfNull(key);
        if (offset < 0) {
            ifThrowException(new RedisInvalidException("ERR setbit offset[" + offset + "] is out of range for Less than 0 "));
        }
        String[] commands = commands("GETBIT", key, String.valueOf(offset));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long decr(String key) {
        checkIfNull(key);
        return executeRedisCommand(commands("DECR", key), Long.class, 0L);
    }

    public long incr(String key) {
        checkIfNull(key);
        return executeRedisCommand(commands("INCR", key), Long.class, 0L);
    }

    public long decrby(String key, long decrement) {
        checkIfNull(key);
        return executeRedisCommand(commands("DECRBY", key, String.valueOf(decrement)), Long.class, 0L);
    }

    public long incrby(String key, long increment) {
        checkIfNull(key);
        return executeRedisCommand(commands("INCRBY", key, String.valueOf(increment)), Long.class, 0L);
    }

    public double incrbyfloat(String key, double increment) {
        checkIfNull(key);
        String result = executeRedisCommand(commands("INCRBYFLOAT", key, String.valueOf(increment)), String.class, null);
        if (result != null) {
            try {
                return Double.parseDouble(result);
            } catch (NumberFormatException exception) {
                ifThrowException(new RedisException(result));
            }
        }
        return 0D;
    }

    public String lpop(String key) {
        checkIfNull(key);
        String[] commands = commands("LPOP", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public String rpop(String key) {
        checkIfNull(key);
        String[] commands = commands("RPOP", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public long lpush(String key, String... values) {
        return push("LPUSH", key, values);
    }

    public long rpush(String key, String... values) {
        return push("RPUSH", key, values);
    }

    public long lpushx(String key, String value) {
        return pushx("LPUSHX", key, value);
    }

    public long rpushx(String key, String value) {
        return pushx("RPUSHX", key, value);
    }

    private long push(String pushCommand, String key, String[] values) {
        checkIfNull(key);
        checkIfNull(values);

        int valueCount = values.length;
        if (valueCount == 0) {
            ifThrowException(new RedisInvalidException("[push] call error: ERR wrong number of arguments for '" + pushCommand + "' command"));
        }
        String[] commands = new String[valueCount + 2];
        commands[0] = pushCommand;
        commands[1] = key;
        System.arraycopy(values, 0, commands, 2, valueCount);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    private long pushx(String pushxCommand, String key, String value) {
        checkIfNull(key, value);
        String[] commands = commands(pushxCommand, key, value);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public String lindex(String key, int index) {
        checkIfNull(key);
        String[] commands = commands("LINDEX", key, String.valueOf(index));
        return executeRedisCommand(commands, String.class, null);
    }

    public long linsert(String key, int direct, String pivot, String value) {
        checkIfNull(key);
        String[] commands = commands("LINSERT", key, direct == 0 ? "BEFORE" : "AFTER", pivot, ifNullValue(value, ""));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long llen(String key) {
        checkIfNull(key);
        String[] commands = commands("LLEN", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public List<String> lrange(String key, int start, int stop) {
        checkIfNull(key);
        String[] commands = commands("LRANGE", key, String.valueOf(start), String.valueOf(stop));
        return executeRedisCommand(commands, List.class, null);
    }

    public long lrem(String key, int count, String value) {
        checkIfNull(key);
        String[] commands = commands("LREM", key, String.valueOf(count), ifNullValue(value, ""));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public void lset(String key, int index, String value) {
        checkIfNull(key);
        String[] commands = commands("LSET", key, String.valueOf(index), ifNullValue(value, ""));
        String result = executeRedisCommand(commands, String.class, null);
        if (result != null) {
            if (!"OK".equals(result)) {
                throw new RedisException(result);
            }
        }
    }

    public void ltrim(String key, int start, int stop) {
        checkIfNull(key);
        String[] commands = commands("LTRIM", key, String.valueOf(start), String.valueOf(stop));
        executeRedisCommand(commands, String.class, null);
    }

    public String rpoplpush(String source, String destination) {
        checkIfNull(source, destination);
        String[] commands = commands("RPOPLPUSH", source, destination);
        return executeRedisCommand(commands, String.class, null);
    }

    public KeyValueEntry blpop(String... keys) {
        return blpop(keys, 0);
    }

    public KeyValueEntry blpop(Collection<String> keys) {
        return blpop(keys, 0);
    }

    public KeyValueEntry blpop(String[] keys, int timeout) {
        checkIfNull(keys);
        // BLPOP key [key ...] timeout
        int keyCount = keys.length;
        if (keyCount == 0) {
            throw new RedisInvalidException("[blpop] call error: ERR wrong number of arguments for 'blpop' command");
        }
        String[] commands = new String[2 + keyCount];
        commands[0] = "BLPOP";
        System.arraycopy(keys, 0, commands, 1, keyCount);
        commands[keyCount + 1] = String.valueOf(timeout);

        List<String> returnVal = executeRedisCommand(commands, List.class, null);
        if (returnVal == null) {
            return null;
        }
        if (returnVal.size() != 2) {
            // 理论上不可能出现
            throw new RedisException("Data return exception");
        }
        KeyValueEntry keyValueEntry = new KeyValueEntry(returnVal.get(0), returnVal.get(1));
        return keyValueEntry;
    }

    public KeyValueEntry blpop(Collection<String> keys, int timeout) {
        if (keys != null) {
            return blpop(keys.toArray(new String[keys.size()]), timeout);
        }
        return null;
    }

    //###################set begin################################

    public long sAdd(String key, String... values) {
        checkIfNull(key);
        int valueCount = values.length;
        if (valueCount == 0) {
            ifThrowException(new RedisInvalidException("[SAdd] call error: ERR wrong number of arguments for 'SAdd' command"));
        }
        String[] commands = new String[valueCount + 2];
        commands[0] = "SADD";
        commands[1] = key;
        System.arraycopy(values, 0, commands, 2, valueCount);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long sCard(String key) {
        checkIfNull(key);
        String[] commands = commands("SCARD", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public List<String> sDiff(String key, String... keys) {
        return doSetOperation("SDIFF", key, keys);
    }

    public long sDiffStore(String destination, String key, String... keys) {
        return doSetOperationStore("SDIFFSTORE", destination, key, keys);
    }

    public List<String> sInter(String key, String... keys) {
        return doSetOperation("SINTER", key, keys);
    }

    public long sInterStore(String destination, String key, String... keys) {
        return doSetOperationStore("SINTERSTORE", destination, key, keys);
    }

    public boolean sIsMember(String key, String member) {
        checkIfNull(key);
        String[] commands = commands("SISMEMBER", key, member);
        long result = executeRedisCommand(commands, Long.class, 0L).intValue();
        return result == 1;
    }

    public List<String> sMembers(String key) {
        checkIfNull(key);
        String[] commands = commands("SMEMBERS", key);
        return executeRedisCommand(commands, List.class, null);
    }

    public boolean sMove(String source, String destination, String member) {
        checkIfNull(source, destination, member);
        String[] commands = commands("SMOVE", source, destination, member);
        long result = executeRedisCommand(commands, Long.class, 0L);
        return result == 1;
    }

    public String sPop(String key) {
        checkIfNull(key);
        String[] commands = commands("SPOP", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public String sRandMember(String key) {
        checkIfNull(key);
        String[] commands = commands("SRANDMEMBER", key);
        return executeRedisCommand(commands, String.class, null);
    }

    public List<String> sRandMember(String key, int count) {
        checkIfNull(key);
        String[] commands = commands("SRANDMEMBER", key, String.valueOf(count));
        return executeRedisCommand(commands, List.class, null);
    }

    public long sRem(String key, String member, String... members) {
        checkIfNull(key);
        int length = members.length;
        String[] commands = new String[length + 2];
        commands[0] = "SREM";
        commands[1] = key;
        commands[2] = member;
        if (length > 0) {
            System.arraycopy(members, 0, commands, 3, length);
        }
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public List<String> sUnion(String key, String... keys) {
        return doSetOperation("SUNION", key, keys);
    }

    public long sUnionStore(String destination, String key, String... keys) {
        return doSetOperationStore("SUNIONSTORE", destination, key, keys);
    }

    public ScanEntry sScan(String key, long cursor) {
        return doScanOperation("SSCAN", key, cursor);
    }

    public ScanEntry sScan(String key, long cursor, String pattern, long count) {
        return doScanOperation("SSCAN", key, cursor, pattern, count);
    }

    private ScanEntry doScanOperation(String operation, String key, long cursor) {
        checkIfNull(key);
        ScanEntry scanEntry = null;
        String[] commands = commands(operation, key, String.valueOf(cursor));
        List list = executeRedisCommand(commands, List.class, null);
        if (list != null && list.size() == 2) {
            scanEntry = new ScanEntry(Long.parseLong(list.get(0).toString()), (List) list.get(1));
        }
        return scanEntry;
    }

    private ScanEntry doScanOperation(String operation, String key, long cursor, String pattern, long count) {
        checkIfNull(key);
        ScanEntry scanEntry = null;
        List<String> commandList = new ArrayList<String>();
        commandList.add(operation);
        commandList.add(key);
        commandList.add(String.valueOf(cursor));
        if (validateCommand(pattern)) {
            commandList.add("MATCH");
            commandList.add(pattern);
        }
        if (count > 0) {
            commandList.add("COUNT");
            commandList.add(String.valueOf(count));
        }
        String[] commands = commandList.toArray(new String[commandList.size()]);
        List list = executeRedisCommand(commands, List.class, null);
        if (list != null && list.size() == 2) {
            scanEntry = new ScanEntry(Long.parseLong(list.get(0).toString()), (List) list.get(1));
        }
        return scanEntry;
    }

    private List<String> doSetOperation(String operation, String key, String[] keys) {
        checkIfNull(key);
        int keysCount = keys.length;
        String[] commands = new String[keysCount + 2];
        commands[0] = operation;
        commands[1] = key;
        if (keysCount > 0) {
            System.arraycopy(keys, 0, commands, 2, keysCount);
        }
        return executeRedisCommand(commands, List.class, null);
    }

    private long doSetOperationStore(String operation, String destination, String key, String... keys) {
        checkIfNull(destination, key);
        int keysCount = keys.length;
        String[] commands = new String[keysCount + 3];
        commands[0] = operation;
        commands[1] = destination;
        commands[2] = key;
        if (keysCount > 0) {
            System.arraycopy(keys, 0, commands, 3, keysCount);
        }
        return executeRedisCommand(commands, Long.class, 0L);
    }

    //###################set end################################

    //###################SortedSet（有序集合 begin################################

    public long zAdd(String key, double score, String member) {
        checkIfNull(key);
        String[] commands = commands("ZADD", key, String.valueOf(score), ifNullValue(member, ""));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long zAdd(String key, ZSetMember member, ZSetMember... members) {
        checkIfNull(key);
        if (member == null) {
            ifThrowException(new RedisInvalidException("[zadd] call error: ERR wrong number of arguments for 'zadd' command"));
        }
        List<String> commandList = new ArrayList<String>();
        commandList.add("ZADD");
        commandList.add(key);
        commandList.add(String.valueOf(member.getScore()));
        commandList.add(ifNullValue(member.getMember(), ""));
        for (ZSetMember zmember : members) {
            commandList.add(String.valueOf(zmember.getScore()));
            commandList.add(ifNullValue(zmember.getMember(), ""));
        }
        String[] commands = commandList.toArray(new String[commandList.size()]);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long zCard(String key) {
        checkIfNull(key);
        String[] commands = commands("ZCARD", key);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long zCount(String key, double min, double max) {
        checkIfNull(key);
        String[] commands = commands("ZCOUNT", key, String.valueOf(min), String.valueOf(max));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public String zIncrby(String key, double increment, String member) {
        checkIfNull(key);
        String[] commands = commands("ZINCRBY", key, String.valueOf(increment), ifNullValue(member, ""));
        return executeRedisCommand(commands, String.class, null);
    }

    public List<String> zRange(String key, long start, long stop) {
        checkIfNull(key);
        String[] commands = commands("ZRANGE", key, String.valueOf(start), String.valueOf(stop));
        return executeRedisCommand(commands, List.class, null);
    }

    public List<? extends Object> zRange(String key, long start, long stop, boolean withscores) {
        if (!withscores) {
            return zRange(key, start, stop);
        }
        return doRangeWithScore("ZRANGE", key, start, stop);
    }

    public List<? extends Object> zRangeByScore(String key, double min, double max, ScoreOptions scoreOptions) {
        return zRangeByScore(key, String.valueOf(min), String.valueOf(max), scoreOptions);
    }

    public List<? extends Object> zRangeByScore(String key, String minExpr, String maxExpr, ScoreOptions scoreOptions) {
        checkIfNull(key);
        List<String> commandList = null;
        int optionLength = 0;
        boolean withscores = false;
        if (scoreOptions != null) {
            withscores = scoreOptions.isWithscores();
            commandList = scoreOptions.buildCommands();
            optionLength = commandList.size();
        }
        String[] commands = new String[4 + optionLength];//commands("ZRANGEBYSCORE", key, String.valueOf(min), String.valueOf(max), "WITHSCORES");
        commands[0] = "ZRANGEBYSCORE";
        commands[1] = key;
        commands[2] = minExpr;
        commands[3] = maxExpr;
        if (optionLength > 0) {
            System.arraycopy(commandList.toArray(), 0, commands, 4, optionLength);
        }

        List<String> results = executeRedisCommand(commands, List.class, null);
        if (results == null || !withscores) {
            return results;
        }

        return toZSetMembers(results);
    }

    public long zRank(String key, String member) {
        checkIfNull(key);
        String[] commands = commands("ZRANK", key, member);
        Long value = executeRedisCommand(commands, Long.class, null);
        return value == null ? -1 : value;
    }

    public long zRem(String key, String member, String... members) {
        checkIfNull(key);
        String[] baseCommands = commands("ZREM", key, ifNullValue(member, ""));
        String[] commands = new String[3 + members.length];
        System.arraycopy(baseCommands, 0, commands, 0, 3);
        System.arraycopy(members, 0, commands, 3, members.length);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long zRemRangeByRank(String key, long start, long stop) {
        checkIfNull(key);
        String[] commands = commands("ZREMRANGEBYRANK", key, String.valueOf(start), String.valueOf(stop));
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public long zRemRangeByScore(String key, double min, double max) {
        return zRemRangeByScore(key, String.valueOf(min), String.valueOf(max));
    }

    public long zRemRangeByScore(String key, String minExpr, String maxExpr) {
        checkIfNull(key);
        String[] commands = commands("ZREMRANGEBYSCORE", key, minExpr, maxExpr);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public List<String> zRevRange(String key, long start, long stop) {
        checkIfNull(key);
        String[] commands = commands("ZREVRANGE", key, String.valueOf(start), String.valueOf(stop));
        return executeRedisCommand(commands, List.class, null);
    }

    public List<? extends Object> zRevRange(String key, long start, long stop, boolean withscores) {
        if (!withscores) {
            return zRevRange(key, start, stop);
        }
        return doRangeWithScore("ZREVRANGE", key, start, stop);
    }

    public List<? extends Object> zRevRangeByScore(String key, double min, double max, ScoreOptions scoreOptions) {
        return zRevRangeByScore(key, String.valueOf(min), String.valueOf(max), scoreOptions);
    }

    public List<? extends Object> zRevRangeByScore(String key, String minExpr, String maxExpr, ScoreOptions scoreOptions) {

        checkIfNull(key);
        List<String> commandList = null;
        int optionLength = 0;
        boolean withscores = false;
        if (scoreOptions != null) {
            withscores = scoreOptions.isWithscores();
            commandList = scoreOptions.buildCommands();
            optionLength = commandList.size();
        }
        String[] commands = new String[4 + optionLength];
        commands[0] = "ZREVRANGEBYSCORE";
        commands[1] = key;
        commands[2] = minExpr;
        commands[3] = maxExpr;
        if (optionLength > 0) {
            System.arraycopy(commandList.toArray(), 0, commands, 4, optionLength);
        }

        List<String> results = executeRedisCommand(commands, List.class, null);
        if (results == null || !withscores) {
            return results;
        }

        return toZSetMembers(results);

    }

    public long zRevRank(String key, String member) {
        checkIfNull(key);
        String[] commands = commands("ZREVRANK", key, ifNullValue(member, ""));
        Long value = executeRedisCommand(commands, Long.class, null);
        return value == null ? -1 : value;
    }

    public String zScore(String key, String member) {
        checkIfNull(key);
        String[] commands = commands("ZSCORE", key, ifNullValue(member, ""));
        String value = executeRedisCommand(commands, String.class, null);
        return value;
    }

    public long zUnionStore(String destination, List<UnionSortedSet> unionSortedSets, Aggregate aggregate) {
        return doZStore("ZUNIONSTORE", destination, unionSortedSets, aggregate);
    }

    public long zInterStore(String destination, List<UnionSortedSet> unionSortedSets, Aggregate aggregate) {
        return doZStore("ZINTERSTORE", destination, unionSortedSets, aggregate);
    }

    public ScanEntry zScan(String key, long cursor) {
        return doScanOperation("ZSCAN", key, cursor);
    }

    public ScanEntry zScan(String key, long cursor, String pattern, long count) {
        return doScanOperation("ZSCAN", key, cursor, pattern, count);
    }

    private long doZStore(String storeCmd, String destination, List<UnionSortedSet> unionSortedSets, Aggregate aggregate) {
        checkIfNull(destination);
        if (unionSortedSets == null || unionSortedSets.size() == 0) {
            return 0;
        }
        List<String> commandList = new ArrayList<String>();
        commandList.add(storeCmd);
        commandList.add(destination);
        commandList.add(String.valueOf(unionSortedSets.size()));

        List<String> weights = new ArrayList<String>();
        for (UnionSortedSet unionSortedSet : unionSortedSets) {
            String key = unionSortedSet.getKey();
            checkIfNull(key);
            commandList.add(key);
            weights.add(String.valueOf(unionSortedSet.getWeight()));
        }
        commandList.add("WEIGHTS");
        commandList.addAll(weights);

        if (aggregate != null) {
            commandList.add("AGGREGATE");
            commandList.add(aggregate.toString());
        }

        String[] commands = commandList.toArray(new String[commandList.size()]);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    private List<ZSetMember> doRangeWithScore(String command, String key, long start, long stop) {
        checkIfNull(key);
        String[] commands = commands(command, key, String.valueOf(start), String.valueOf(stop), "WITHSCORES");
        List<String> results = executeRedisCommand(commands, List.class, null);
        if (results != null) {
            return toZSetMembers(results);
        }
        return null;
    }

    private List<ZSetMember> toZSetMembers(List<String> results) {
        List<ZSetMember> zSetMembers = new ArrayList<ZSetMember>();
        ZSetMember zSetMember = null;
        for (int i = 0; i < results.size(); i += 2) {
            String member = results.get(i);
            String score = results.get(i + 1);
            zSetMember = new ZSetMember();
            zSetMember.setMember(member);
            zSetMember.setScore(Double.parseDouble(score));
            zSetMembers.add(zSetMember);
        }
        return zSetMembers;
    }

    //###################SortedSet（有序集合 end################################


    //#######pubsub###############################################################
    public long publish(String topic, String message) {
        checkIfNull(topic);
        String[] commands = commands("PUBLISH", topic, message);
        return executeRedisCommand(commands, Long.class, 0L);
    }

    public void subscribe(Subscriber subscriber) {
        doSubscribe("SUBSCRIBE", subscriber);
    }

    public void psubscribe(Subscriber subscriber) {
        doSubscribe("PSUBSCRIBE", subscriber);
    }


    public void doSubscribe(String subCommand, Subscriber subscriber) {
        String[] channels = subscriber.getChannels();
        checkIfNull(channels);
        RedisConnection connection = getSharedConnection();
        if (connection.isPipelined()) {
            throw new RedisException("Unsupported operation[" + subCommand + "] because pipeline is running... ");
        }
        String[] commands = new String[channels.length + 1];
        commands[0] = subCommand;
        System.arraycopy(channels, 0, commands, 1, channels.length);
        connection.subscribe(commands, subscriber);
    }

    public void unsubscribe(String... channels) {
        doUnsubscribe("UNSUBSCRIBE", channels);
    }

    public void punsubscribe(String... channelPatterns) {
        doUnsubscribe("PUNSUBSCRIBE", channelPatterns);
    }

    private void doUnsubscribe(String command, String... channels) {
        checkIfNull(channels);
        // unsubscribe
        RedisConnection connection = getSharedConnection();
        if (connection.isPipelined()) {
            throw new RedisException("Unsupported operation[" + command + "] because pipeline is running... ");
        }
        connection.unsubscribe(command, channels);
    }

    public List<String> pubsubChannels(String pattern) {
        String[] commands = commands("PUBSUB", "CHANNELS", ifNullValue(pattern, "*"));
        return executeRedisCommand(commands, List.class, null);
    }

    public Map<String, Long> pubsubNumsub(String... channels) {
        checkIfNull(channels);
        String[] commands = new String[channels.length + 2];
        commands[0] = "PUBSUB";
        commands[1] = "NUMSUB";
        System.arraycopy(channels, 0, commands, 2, channels.length);
        Map<String, Long> channelNums = new LinkedHashMap<String, Long>();
        List<Object> results = executeRedisCommand(commands, List.class, null);
        if (results != null) {
            int size = results.size();
            if (size % 2 == 1) {
                // 理论上不可能出现
                throw new RedisException("Data return exception");
            }
            for (int i = 0; i < size; i += 2) {
                channelNums.put((String) results.get(i), (Long) results.get(i + 1));
            }
        }
        return channelNums;
    }

    public long pubsubNumpat() {
        return executeRedisCommand(commands("PUBSUB", "NUMPAT"), Long.class, 0L);
    }

    // ===Transaction========================================================================

    public void multi() {
        // 设置开启事务状态
        beginMulti();
        // 发送命令
        executeRedisCommand(commands("MULTI"), String.class, null);
    }

    public Object exec() {
        // 结束事务状态
        endMulti();
        // 执行命令
        return executeRedisCommand(commands("EXEC"), List.class, null);
    }

    public void discard() {
        endMulti();
        executeRedisCommand(commands("DISCARD"), String.class, null);
    }

    public void watch(String... keys) {
        checkIfNull(keys);
        int keyCount = keys.length;
        if (keyCount == 0) {
            ifThrowException(new RedisInvalidException("[watch] call error: ERR wrong number of arguments for 'watch' command"));
        }
        String[] commands = new String[keyCount + 1];
        commands[0] = "WATCH";
        System.arraycopy(keys, 0, commands, 1, keyCount);
        executeRedisCommand(commands, String.class, null);
    }

    public void unwatch() {
        executeRedisCommand(commands("UNWATCH"), String.class, null);
    }

    // =================华丽的分割线==========================================================

    private String ifNullValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value).trim();
    }

    private void checkIfNull(String... commands) {
        for (String command : commands) {
            if (!validateCommand(command)) {
                ifThrowException(new RedisInvalidException("Invalid Redis Key ['" + command + "']"));
            }
        }
    }

    private void ifThrowException(RuntimeException exception) {
        if (!isMulti()) {
            throw exception;
        }
    }

    private String[] appendCommands(List<String> commandList, Map<String, ? extends Object> values) {
        if (values != null) {
            for (String entryKey : values.keySet()) {
                Object entryValue = values.get(entryKey);
                if (!validateCommand(entryKey))
                    continue;
                commandList.add(entryKey);
                if (entryValue instanceof byte[]) {
                    commandList.add(new String((byte[]) entryValue));
                } else {
                    commandList.add(ifNullValue(entryValue, ""));
                }
            }
        }
        return commandList.toArray(new String[commandList.size()]);
    }

}

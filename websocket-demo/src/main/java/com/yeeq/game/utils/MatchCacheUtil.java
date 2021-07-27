package com.yeeq.game.utils;

import com.yeeq.game.utils.pageUtil.RedisFurryAndPageQuery;
import com.yeeq.game.websocket.ChatWebsocket;
import com.yeeq.game.redis.EnumRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yeeq
 * @date 2021/4/16
 */
@Slf4j
@Component
public class MatchCacheUtil {

    /**
     * 用户 userId 为 key，ChatWebsocket 为 value
     */

    private static final Map<String, ChatWebsocket> CLIENTS = new ConcurrentHashMap<>();
    private static Integer tableId = 0;

    public static Map<String, ChatWebsocket> getCLIENTS() {
        return CLIENTS;
    }

    /**
     * 设置当前服务器最大可容纳的缓存个数
     */
    private int maxSize = 100;


    /**
     * key 是标识存储用户在线状态的 EnumRedisKey，value 为 map 类型，其中用户 userId 为 key，用户在线状态 为 value
     */
    @Resource
    private RedisTemplate<String, Map<String, String>> redisTemplate;

    /**
     * 添加客户端
     * 借助缓存来实现高效率的添加删除操作
     */
    public void addClient(String userId, ChatWebsocket websocket) {
        checkNotNull(userId);
        checkNotNull(websocket);
        // 当缓存存在时，更新缓存
        if(CLIENTS.containsKey(userId)){
            ChatWebsocket newCache = CLIENTS.get(userId);
            newCache.setAccessTime(System.currentTimeMillis());
            newCache.setWriteTime(System.currentTimeMillis());
            // 设置每个缓存的设置时间为10分钟
            newCache.setExpireTime(10 * 60);
            newCache.setHitCount(newCache.getHitCount() + 1);
            return;
        }

        // 已经达到最大缓存
        if (isFull()) {
            String kickedKey = getKickedKey();
            if (kickedKey !=null){
                // 移除最少使用的缓存
                removeClient(kickedKey);
            }else {
                return;
            }
        }
        // 记录加入游戏的用户ID
        websocket.setUserId(userId);
        websocket.setHitCount(1);
        websocket.setAccessTime(System.currentTimeMillis());
        websocket.setWriteTime(System.currentTimeMillis());
        websocket.setExpireTime(10 * 60);
        // 我可以将userID + nonce这样的组合存入redis嘛？
        CLIENTS.put(userId, websocket);
    }

    /**
     * 移除客户端
     */
    public static void removeClient(String userId) {
        CLIENTS.remove(userId);
    }

    /**
     * 获取客户端
     */
    public ChatWebsocket getClient(String userId) {
        if (CLIENTS.isEmpty()) return null;
        if (!CLIENTS.containsKey(userId)) return null;
        ChatWebsocket cache = CLIENTS.get(userId);
        if (cache == null) return null;
        // 读取当前客户端的时候，将使用次数+1，并将访问时间设置为当前
        cache.setHitCount(cache.getHitCount() + 1);
        cache.setAccessTime(System.currentTimeMillis());
        return cache;
    }

    /**
     * 获取最少使用的缓存
     * @return
     */
    private String getKickedKey() {
        ChatWebsocket min = Collections.min(CLIENTS.values());
        return min.getUserId();
    }

    /**
     * 判断是否达到最大缓存
     *
     * @return
     */
    public boolean isFull(){
        return CLIENTS.size() == maxSize;
    }

    /**
     * 检测字段是否合法
     *
     * @param reference
     * @param <T>
     * @return
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }


    /**
     * 移除用户在线状态
     * getKey()返回的是一个name
     */
    public void removeUserOnlineStatus(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.USER_STATUS.getKey(), userId);
    }

    /**
     * 获取用户在线状态
     */
    public StatusEnum getUserOnlineStatus(String userId) {
        Object status = redisTemplate.opsForHash().get(EnumRedisKey.USER_STATUS.getKey(), userId);
        if (status == null) {
            return null;
        }
        return StatusEnum.getStatusEnum(status.toString());
    }

    /**
     * 设置用户为 IDLE 状态, 带匹配状态
     * 设置redis中某个变量的hashMap
     */
    public void setUserIDLE(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IDLE.getValue());
    }

    /**
     * 设置用户为 IN_MATCH 状态
     */
    public void setUserInMatch(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IN_MATCH.getValue());
    }

    /**
     * 获取玩家用户 IN_MATCH状态
     */
    public boolean getUserInMatch(String userId) {
        return redisTemplate.opsForHash().get(EnumRedisKey.USER_STATUS.getKey(), userId) == StatusEnum.IN_MATCH.getValue() ? true : false;
    }

    /**
     * 获取玩家用户 IN_GAME状态
     */
    public boolean getUserInGame(String userId){
        return redisTemplate.opsForHash().get(EnumRedisKey.USER_STATUS.getKey(), userId) == StatusEnum.IN_GAME.getValue() ? true : false;
    }

    /**
     *  获取当前在redis缓存中处于匹配状态的用户
     */
    public List<String> getAllUserInMatchRoom(String userId) {
        Cursor<Map.Entry<Object,Object>> cursor = redisTemplate.opsForHash().scan(EnumRedisKey.USER_STATUS.getKey(), ScanOptions.NONE);
        List<String> keyList = new ArrayList<>();
        while (cursor.hasNext()){
            Map.Entry<Object, Object> entry = cursor.next();
            if(!entry.getKey().equals(userId) && entry.getValue().equals(StatusEnum.IN_MATCH.getValue())){
                String key = (String) entry.getKey();
                keyList.add(key);
            }
        }
        return keyList;
    }

    /**
     *  分页获取当前redis缓存中处于匹配状态的用户（除了指定用户外，请求匹配的用户）
     */
    public List<String> findKeysForPage(List<String> keyList, int pageNum, int pageSize) throws IOException {
//        Cursor<Map.Entry<Object,Object>> cursor = redisTemplate.opsForHash().scan(EnumRedisKey.USER_STATUS.getKey(), ScanOptions.NONE);
//        RedisFurryAndPageQuery.setPage(cursor, "key", String.valueOf(tableId));
//        RedisFurryAndPageQuery.getPageResult("key")
        // 记录指定分页的user个数
//        int count = keyList.size();

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = pageNum * pageSize;
        int tempIndex = 0;
        List<String> result = new ArrayList<>(pageSize);
        Iterator it = keyList.iterator();
        while (it.hasNext()){
            if (tempIndex >= startIndex && tempIndex < endIndex){
                result.add(String.valueOf(it.next()));
                tempIndex++;
                continue;
            }
            //获取到满足条件的数据后，就退出
            if (tempIndex >= endIndex){
                break;
            }
            //记录每次遍历的次数
            tempIndex++;
            it.next();
        }
        return result;
    }


    /**
     * 随机获取处于匹配状态的用户（除了指定用户外）
     * 返回的是随机找到的用户ID
     */
    public String getUserInMatchRandom(String userId) {
        Optional<Map.Entry<Object, Object>> any = redisTemplate.opsForHash().entries(EnumRedisKey.USER_STATUS.getKey())
                .entrySet().stream().filter(entry -> entry.getValue().equals(StatusEnum.IN_MATCH.getValue()) && !entry.getKey().equals(userId))
                .findAny();
        return any.map(entry -> entry.getKey().toString()).orElse(null);
    }

    /**
     * 设置用户为 IN_GAME 状态
     */
    public void setUserInGame(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.IN_GAME.getValue());
    }

    /**
     * 设置处于游戏中的用户在同一房间
     */
    public void setUserInRoom(String userId1, String userId2) {
        redisTemplate.opsForHash().put(EnumRedisKey.ROOM.getKey(), userId1, userId2);
        redisTemplate.opsForHash().put(EnumRedisKey.ROOM.getKey(), userId2, userId1);
    }

    /**
     * 从房间中移除用户
     */
    public void removeUserFromRoom(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.ROOM.getKey(), userId);
    }

    /**
     * 从房间中获取用户
     */
    public String getUserFromRoom(String userId) {
        return redisTemplate.opsForHash().get(EnumRedisKey.ROOM.getKey(), userId).toString();
    }

    /**
     * 设置处于游戏中的用户的对战信息
     */
    public void setUserMatchInfo(String userId, String userMatchInfo) {
        redisTemplate.opsForHash().put(EnumRedisKey.USER_MATCH_INFO.getKey(), userId, userMatchInfo);
    }

    /**
     * 移除处于游戏中的用户的对战信息
     */
    public void removeUserMatchInfo(String userId) {
        redisTemplate.opsForHash().delete(EnumRedisKey.USER_MATCH_INFO.getKey(), userId);
    }

    /**
     * 获取处于游戏中的用户的对战信息
     */
    public String getUserMatchInfo(String userId) {
        return redisTemplate.opsForHash().get(EnumRedisKey.USER_MATCH_INFO.getKey(), userId).toString();
    }

    /**
     * 设置用户为游戏结束状态
     */
    public synchronized void setUserGameover(String userId) {
        removeUserOnlineStatus(userId);
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), userId, StatusEnum.GAME_OVER.getValue());
    }
}

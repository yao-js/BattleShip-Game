package com.yjs.battleshipGame.utils.pageUtil;

import com.yjs.battleshipGame.redis.EnumRedisKey;
import com.yjs.battleshipGame.utils.StatusEnum;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Map;

public class test {
    @Resource
    private static RedisTemplate<String, Map<String, String>> redisTemplate;

    public static void main(String[] args) {
//        MatchCacheUtil matchCacheUtil = new MatchCacheUtil();
        redisTemplate.opsForHash().put(EnumRedisKey.USER_STATUS.getKey(), "ddd", StatusEnum.IN_MATCH.getValue());
//        RedisFurryAndPageQuery.setPage();
    }
}

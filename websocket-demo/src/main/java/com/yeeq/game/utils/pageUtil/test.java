package com.yeeq.game.utils.pageUtil;

import com.yeeq.game.redis.EnumRedisKey;
import com.yeeq.game.utils.MatchCacheUtil;
import com.yeeq.game.utils.StatusEnum;
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

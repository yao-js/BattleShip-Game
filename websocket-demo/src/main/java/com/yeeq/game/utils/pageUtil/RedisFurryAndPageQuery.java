package com.yeeq.game.utils.pageUtil;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.*;

@Component
public class RedisFurryAndPageQuery<T> {

    //插入辅助分页
    @Autowired
    private static PageUtil pageUtil;
    //注入redis模板工具
    @Autowired
    private RedisTemplate redisTemplate;



    /**
     * 根据条件进行模糊查询查询并返回分页结果
     * @param name
     * @param currentPage
     * @param count
     * @return
     */
//    public Map<String,Object> find(String name, int currentPage, int count,String tableName,T t) {
//
//        //将传入的参数变为小写
//        name = StringUtils.lowerCase(name);
//        //map用来存储查询到的结果以及分页的总数据
//        Map<String,Object> map = new HashMap<>();
//        //用来存储查询到的ZhDicGoods
//        ArrayList<ZhDicGoods> result = new ArrayList<>();
//        //定义pageName用来查看redis中是否存在对应的辅助分页key-value
//        String pageName = tableName+name + ":page";
//        //是否存在对应的辅助分页辅助分页key-value
//        Boolean ifExist = redisTemplate.hasKey(pageName);
//        //如果不存在，生成辅助分页，同时查询
//        if(!ifExist){
//
//            try {
//
//                //如果传入参数为空则查询所有
//                if(name==null||"".equals(name)){
//
//                    //模糊查询返回结果
//                    //Cusor中存储的是查询key对应的Map
//                    Cursor<Map.Entry<String,String>> cursor = redisTemplate
//                            .opsForHash()
//                            .scan(tableName, ScanOptions.scanOptions()       //绑定模糊查询的hash的key
//                                    .match("*")                                      //模糊查询规则
//                                    .count(1000).build());
//                }
//                //模糊查询返回结果
//                //Cusor中存储的是查询key对应的Map
//                Cursor<Map.Entry<String,String>> cursor = redisTemplate
//                        .opsForHash()
//                        .scan(tableName, ScanOptions.scanOptions()       //绑定模糊查询的hash的key
//                                .match(name+"*")                                 //模糊查询规则
//                                .count(10000).build());
//                //生成分页key-value
//                setPage(cursor,name,tableName);
//                //生成查询结果并且放入map中
//                getPageResult(name,currentPage,count,map,result,tableName,t);
//                cursor.close();
//            } catch (Exception e) {
//
//                e.printStackTrace();
//            }
//        }else{
//
//            //有辅助分页key-value,直接查询结果放入map中
//            getPageResult(name,currentPage,count,map,result,tableName,t);
//        }
//        //返回result
//        return map;
//
//    }


    /**
     * 生成辅助分页
     * @param cursor
     * @param name：会在pageutil中自动加上:page
     */
    public static void setPage(Cursor<Map.Entry<Object, Object>> cursor, String name, String tableName){

        //setPage初始化值,zset的sort值)
        //指定每个存入set中变量的分值
        int i = 1;
        //遍历模糊查询结果,将模糊查询的结果生成分页数据，用zset存储模糊查询的数据排序
        //存储名称是查询name+：page，值是hash类型的key
        while (cursor.hasNext()){

            Map.Entry<Object,Object> result= cursor.next();
            //获取key
            String key = (String) result.getKey();
            //获取value
//            Object value = result.getValue();
            //存储对应的辅助分页数据
            pageUtil.setPage(tableName+name,key,i,null);
            i++;

        }
    }


    /**
     * 获取分页的总数据，并且加入到map中
     * @param name
     * @param currentPage
     * @param count
     */
    public static Map getPageResult(String name, int currentPage, int count, String tableName){

//        ArrayList<Object> result = new ArrayList<>();
        Map<Object, Object> resultMap = new HashMap<>();
        //得到分页数据总条数
        Integer totalNumber = pageUtil.getSize(tableName+name);
        //得到对应分页的key数组
        Set<String> keyPages = pageUtil.getPage(tableName+name, currentPage, count);
        //遍历循环查询对应count对应的数据，同时转为对象输入到List
        count =keyPages.size();
//        for (String keyPage : keyPages) {
//
//            //根据zset的key查询hash对应的数据
//            String JSONObject = (String) redisTemplate.boundHashOps(tableName).get(keyPage);
//            //转为对应的实体类
//            T Object = (T) JSON.parseObject(JSONObject, (Type) t);
//            result.add(Object);
//        }
        //返回总页数
        Integer totalPage;
        if(totalNumber<=count){

            totalPage=1;
        }else {

            totalPage =totalNumber/10+1;
        }

        //加入返回实体类结果，总条数，起始页数，总页数
//        resultMap.put("result",result);
        resultMap.put("totalNumber",totalNumber);
        resultMap.put("currentPage",currentPage);
        resultMap.put("count",count);
        resultMap.put("totalPage",totalPage);
        return resultMap;
    }

}

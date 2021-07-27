package com.yeeq.game.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yeeq.game.constant.CommonField;
import com.yeeq.game.entity.*;
import com.yeeq.game.error.GameServerError;
import com.yeeq.game.exception.GameServerException;
import com.yeeq.game.service.QuestionSev;
import com.yeeq.game.utils.MatchCacheUtil;
import com.yeeq.game.utils.MessageCode;
import com.yeeq.game.utils.MessageTypeEnum;
import com.yeeq.game.utils.StatusEnum;
import com.yeeq.game.utils.localCache.Cache;
import com.yeeq.game.utils.localCache.LFUCache;
import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yeeq
 * @date 2021/4/9
 */
@Component
@Slf4j
@Getter
@Setter
@ServerEndpoint(value = "/game/match/{userId}") //用来表示在server端处理client的请求，客户端 -> 服务器端， 服务器 -> 客户端
public class ChatWebsocket implements Comparable<ChatWebsocket>{

    private Session session;

    // 最后一次访问时间
    private long accessTime;
    // 创建时间
    private long writeTime;
    // 存活时间
    private long expireTime;
    // 命中次数
    private Integer hitCount;

    private String userId;

    private String bit;


    static QuestionSev questionSev;
    static MatchCacheUtil matchCacheUtil;

    static Lock lock = new ReentrantLock();

    static Condition matchCond = lock.newCondition();


    @Autowired
    public void setMatchCacheUtil(MatchCacheUtil matchCacheUtil) {
        ChatWebsocket.matchCacheUtil = matchCacheUtil;
    }

    @Autowired
    public void setQuestionSev(QuestionSev questionSev) {
        ChatWebsocket.questionSev = questionSev;
    }

    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session) {

        log.info("ChatWebsocket open 有新连接加入 userId: {}", userId);
        if (matchCacheUtil.getClient(userId) != null){
            Set<String> receivers = new HashSet<>();
            receivers.add(userId);
            ChatMessage<String> newResult = new ChatMessage<>();
            MessageReply<String> newMessageReply = new MessageReply<>();
            newMessageReply.setDesc(MessageCode.USER_IS_NULL.getDesc());
            newMessageReply.setCode(MessageCode.USER_IS_NULL.getCode());
            newResult.setType(MessageTypeEnum.IN_SESSION);
            newResult.setData("该玩家已建立了连接，无法继续进行操作");
            newResult.setReceivers(receivers);
            newMessageReply.setChatMessage(newResult);
            session.getAsyncRemote().sendText(JSON.toJSONString(newMessageReply));
            log.info("ChatWebsocket open 连接建立结束 userId: {}", userId);
            return;
        }

        this.userId = userId;
        this.session = session;

        matchCacheUtil.addClient(userId, this);

        log.info("ChatWebsocket open 连接建立完成 userId: {}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {

        log.error("ChatWebsocket onError 发生了错误 userId: {}, errorMessage: {}", userId, error.getMessage());

        if (userId != null){
            matchCacheUtil.removeClient(userId);
            matchCacheUtil.removeUserOnlineStatus(userId);
            matchCacheUtil.removeUserFromRoom(userId);
            matchCacheUtil.removeUserMatchInfo(userId);
        }

        log.info("ChatWebsocket onError 连接断开完成 userId: {}", userId);
    }

    @OnClose
    public void onClose()
    {
        log.info("ChatWebsocket onClose 连接断开 userId: {}", userId);
        if (userId != null){
            matchCacheUtil.removeClient(userId);
            matchCacheUtil.removeUserOnlineStatus(userId);
            matchCacheUtil.removeUserFromRoom(userId);
            matchCacheUtil.removeUserMatchInfo(userId);
        }

        log.info("ChatWebsocket onClose 连接断开完成 userId: {}", userId);
    }


    @OnMessage
    public void onMessage(String message, Session session) {

        log.info("ChatWebsocket onMessage userId: {}, 来自客户端的消息 message: {}", userId, message);

        JSONObject jsonObject = JSON.parseObject(message);
        MessageTypeEnum type = jsonObject.getObject("type", MessageTypeEnum.class);

        log.info("ChatWebsocket onMessage userId: {}, 来自客户端的消息类型 type: {}", userId, type);

        if (type == MessageTypeEnum.ADD_USER) {
            addUser(jsonObject);
        } else if (type == MessageTypeEnum.MATCH_USER) {
            matchUser(jsonObject);
        } else if (type == MessageTypeEnum.CANCEL_MATCH) {
            cancelMatch(jsonObject);
        } else if (type == MessageTypeEnum.PLAY_GAME) {
            toPlay(jsonObject);
        } else if (type == MessageTypeEnum.GAME_OVER) {
            gameover(jsonObject);
        } else if (type == MessageTypeEnum.ENTER_MATCH) {
            this.bit = jsonObject.getObject("bit", String.class);
//            log.info(getBit());
            showAllMatchUserCount(jsonObject);
        } else if (type == MessageTypeEnum.ACCEPT_MATCH){
            acceptMatch(jsonObject);
        } else if (type == MessageTypeEnum.REJECT_MATCH){
            rejectMatch(jsonObject);
        }
//        else if (type == MessageTypeEnum.FIND_PAGE){
//            int pageNum = jsonObject.getObject("pageNum", Integer.class);
//            int pageSize = jsonObject.getObject("pageSize", Integer.class);
//            findUserListForPage(jsonObject, pageNum, pageSize);
//        }
        else {
            throw new GameServerException(GameServerError.WEBSOCKET_ADD_USER_FAILED);
        }

        log.info("ChatWebsocket onMessage userId: {} 消息接收结束", userId);
    }


    /**
     * 群发消息
     */
    private void sendMessageAll(MessageReply<?> messageReply) {

        log.info("ChatWebsocket sendMessageAll 消息群发开始 userId: {}, messageReply: {}", userId, JSON.toJSONString(messageReply));

        Set<String> receivers = messageReply.getChatMessage().getReceivers();
        for (String receiver : receivers) {
            ChatWebsocket client = matchCacheUtil.getClient(receiver);
            client.session.getAsyncRemote().sendText(JSON.toJSONString(messageReply));
        }

        log.info("ChatWebsocket sendMessageAll 消息群发结束 userId: {}", userId);
    }

    /**
     * 用户加入游戏
     */
    private void addUser(JSONObject jsonObject) {

        log.info("ChatWebsocket addUser 用户加入游戏开始 message: {}, userId: {}", jsonObject.toJSONString(), userId);

        MessageReply<Object> messageReply = new MessageReply<>();
        ChatMessage<Object> result = new ChatMessage<>();
        result.setType(MessageTypeEnum.ADD_USER);
        result.setSender(userId);

//        if (matchCacheUtil.getClient(userId) != null){
//            Set<String> receivers = new HashSet<>();
//            receivers.add(userId);
//            ChatMessage<String> newResult = new ChatMessage<>();
//            MessageReply<String> newMessageReply = new MessageReply<>();
//            newMessageReply.setDesc(MessageCode.USER_IS_IN_SESSION.getDesc());
//            newMessageReply.setCode(MessageCode.USER_IS_IN_SESSION.getCode());
//            newResult.setType(MessageTypeEnum.IN_SESSION);
//            newResult.setData("该玩家已建立了连接，无法继续进行操作");
//            newResult.setReceivers(receivers);
//            newMessageReply.setChatMessage(newResult);
//            sendMessageAll(newMessageReply);
//            log.info("ChatWebsocket open 连接建立结束 userId: {}", userId);
//            return;
//        }

        if (matchCacheUtil.getUserInMatch(userId)) {
            // 设置receiver地址ID
            Set<String> receivers = new HashSet<>();
            receivers.add(userId);
            ChatMessage<String> newResult = new ChatMessage<>();
            MessageReply<String> newMessageReply = new MessageReply<>();
            newMessageReply.setDesc(MessageCode.USER_IS_ONLINE.getDesc());
            newMessageReply.setCode(MessageCode.USER_IS_ONLINE.getCode());
            newResult.setType(MessageTypeEnum.IN_MATCH);
            newResult.setData("您已经处于匹配当中，无法继续进行操作");
            newResult.setReceivers(receivers);
            newMessageReply.setChatMessage(newResult);
            sendMessageAll(newMessageReply);
            log.info("ChatWebsocket addUser 用户加入游戏结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);
            return;
        }

        /*
         * 获取用户的在线状态
         * 如果缓存中没有保存用户状态，表示用户新加入，则设置为在线状态
         * 否则直接返回
         */
        StatusEnum status = matchCacheUtil.getUserOnlineStatus(userId);
        if (status != null) {
            /*
             * 游戏结束状态，重新设置为在线状态
             * 否则返回错误提示信息
             */
            // 如果当前这个user的status是处于game over的状态，则将它转为加入成功状态
            // 否则将它设置为用户已存在状态
            if (status.compareTo(StatusEnum.GAME_OVER) == 0) {
                messageReply.setCode(MessageCode.SUCCESS.getCode());
                messageReply.setDesc(MessageCode.SUCCESS.getDesc());
                matchCacheUtil.setUserIDLE(userId);
            } else {
                messageReply.setCode(MessageCode.USER_IS_ONLINE.getCode());
                messageReply.setDesc(MessageCode.USER_IS_ONLINE.getDesc());
            }
        } else {
            messageReply.setCode(MessageCode.SUCCESS.getCode());
            messageReply.setDesc(MessageCode.SUCCESS.getDesc());
            matchCacheUtil.setUserIDLE(userId);
        }

        Set<String> receivers = new HashSet<>();
        receivers.add(userId);
        result.setReceivers(receivers);
        messageReply.setChatMessage(result);

        sendMessageAll(messageReply);

        log.info("ChatWebsocket addUser 用户加入游戏结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);

    }

    /**
     * 用户进入匹配大厅
     */
    @SneakyThrows
    private void showAllMatchUserCount(JSONObject jsonObject){
        log.info("ChatWebsocket enterMatchHall 用户查询当前匹配大厅的总人数开始 message: {}, userId: {}", jsonObject.toJSONString(), userId);
        ChatMessage<PageInfo> result = new ChatMessage<>();
        MessageReply<PageInfo> messageReply = new MessageReply<>();
        // 配置sender的地址ID
        result.setSender(userId);
        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(userId);
        result.setReceivers(receivers);

//        // 如果该用户未处于USER_IS_ONLINE状态，则无法进行进入匹配大厅的操作
//        if (matchCacheUtil.getUserOnlineStatus(userId) == null){
//            ChatMessage<String> newResult = new ChatMessage<>();
//            MessageReply<String> newMessageReply = new MessageReply<>();
//            newMessageReply.setDesc(MessageCode.USER_IS_NULL.getDesc());
//            newMessageReply.setCode(MessageCode.USER_IS_NULL.getCode());
//            newResult.setType(MessageTypeEnum.IN_SESSION);
//            newResult.setData("该玩家未建立连接，无法继续进行操作");
//            newResult.setReceivers(receivers);
//            newMessageReply.setChatMessage(newResult);
//            sendMessageAll(newMessageReply);
//            log.info("ChatWebsocket enterMatchHall 用户查询当前匹配大厅的总人数结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);
//            return;
//        }

        // 若玩家处于已游戏状态，无法进行进入匹配大厅操作
        if (matchCacheUtil.getUserInGame(userId)){
            ChatMessage<String> newResult = new ChatMessage<>();
            MessageReply<String> newMessageReply = new MessageReply<>();
            newMessageReply.setDesc(MessageCode.CURRENT_USER_IS_INGAME.getDesc());
            newMessageReply.setCode(MessageCode.CURRENT_USER_IS_INGAME.getCode());
            newResult.setType(MessageTypeEnum.IN_GAME);
            newResult.setData("您已经处于游戏当中，无法继续进行匹配操作");
            newMessageReply.setChatMessage(newResult);
            sendMessageAll(newMessageReply);
            log.info("ChatWebsocket enterMatchHall 用户查询当前匹配大厅的总人数结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);
            return;
        }

        // 若当前玩家用户的状态不在匹配中的话
        // 将当前的玩家用户的状态设置为在匹配中
        if (!matchCacheUtil.getUserInMatch(userId)){
            lock.lock();
            try {
                // 设置用户状态为匹配中
                matchCacheUtil.setUserInMatch(userId);
                matchCond.signal();
            } finally {
                lock.unlock();
            }
        }

        List<String> keyList = matchCacheUtil.getAllUserInMatchRoom(userId);
        // 当前处于匹配状态的总人数，用于被Javascript来分析得出分页数据
        int totalNumber = keyList.size();

        //若totalNumber = 0，则当前匹配大厅的人数为空
        if (totalNumber == 0) {
            result.setType(MessageTypeEnum.MATCH_NULL);
            messageReply.setDesc(MessageCode.RESULT_NULL.getDesc());
            messageReply.setCode(MessageCode.RESULT_NULL.getCode());
            messageReply.setChatMessage(result);
            sendMessageAll(messageReply);
            return;
        }

        // 设置返回给客户端的信息
        List<PlayerInfo> playerInfos = new ArrayList<>();
        PageInfo pageInfo = new PageInfo();
        Iterator it = keyList.iterator();
        while (it.hasNext()){
            PlayerInfo playerInfo = new PlayerInfo();
            String userId = String.valueOf(it.next());
            playerInfo.setPlayerEthAddress(userId);
            playerInfo.setBit(matchCacheUtil.getClient(userId).getBit());
            playerInfos.add(playerInfo);
        }
        pageInfo.setPlayerInfoList(playerInfos);
        pageInfo.setTotalPageNums(totalNumber);

        messageReply.setDesc(MessageCode.QUERY_ALL_USER.getDesc());
        messageReply.setCode(MessageCode.QUERY_ALL_USER.getCode());

        result.setType(MessageTypeEnum.FIND_PAGE);
        result.setSender(userId);
        result.setData(pageInfo);
        // 返回总的匹配人数
        messageReply.setChatMessage(result);

        sendMessageAll(messageReply);

        log.info("ChatWebsocket enterMatchHall 用户查询当前匹配大厅的总人数结束 message: {}, userId: {}", jsonObject.toJSONString(), userId);

    }

//    @SneakyThrows
//    private void findUserListForPage(JSONObject jsonObject, int pageNum, int pageSize){
//        log.info("ChatWebsocket enterMatchHall 用户查询当前页面人数 message: {}, userId: {}", jsonObject.toJSONString(), userId);
//        log.info(String.valueOf(pageNum), pageSize);
//        ChatMessage<List<PlayerInfo>> result = new ChatMessage<>();
//        MessageReply<List<PlayerInfo>> messageReply = new MessageReply<>();
//        result.setSender(userId);
//        result.setType(MessageTypeEnum.ENTER_MATCH);
//
//        List<String> keyList = matchCacheUtil.getAllUserInMatchRoom(userId);
//        // 根据总的玩家列表查询出某分页的玩家信息
//        List<String> resultList = matchCacheUtil.findKeysForPage(keyList, pageNum, pageSize);
//
//        //若totalNumber = 0，则当前匹配大厅的人数为空
//        if (keyList.size() == 0) {
//            messageReply.setDesc(MessageCode.MESSAGE_ERROR.getDesc());
//            messageReply.setCode(MessageCode.MESSAGE_ERROR.getCode());
//            sendMessageAll(messageReply);
//            return;
//        }
//
//        List<PlayerInfo> playerInfos = new ArrayList<>();
//
//        Iterator it = resultList.iterator();
//        if (it.hasNext()){
//            PlayerInfo playerInfo = new PlayerInfo();
//            String userId = String.valueOf(it.next());
//            playerInfo.setPlayerEthAddress(userId);
//            playerInfo.setPlayerEthAddress(matchCacheUtil.getClient(userId).getBit());
//            playerInfos.add(playerInfo);
//        }
//        result.setData(playerInfos);
//        messageReply.setCode(MessageCode.QUERY_PLAYER_PAGE.getCode());
//        messageReply.setDesc(MessageCode.QUERY_PLAYER_PAGE.getDesc());
//        messageReply.setChatMessage(result);
//
//        sendMessageAll(messageReply);
//        log.info("ChatWebsocket enterMatchHall 用户查询查询当前页面人数 message: {}, userId: {}", jsonObject.toJSONString(), userId);
//    }

    private void acceptMatch(JSONObject jsonObject) {

        log.info("ChatWebsocket matchUser 用户拒绝匹配邀请 message: {}, userId: {}", jsonObject.toJSONString(), userId);
        MessageReply<MatchInfo> messageReply = new MessageReply<>();
        ChatMessage<MatchInfo> result = new ChatMessage<>();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);


        // 创建一个异步线程任务，负责匹配其他同样处于匹配状态的选定用户
        Thread matchThread = new Thread(() -> {
//            boolean flag = true;
            String receiver = null;
//            while (flag) {
                // 获取除自己以外的其他待匹配用户 （手动添加锁，保证线程安全）
                lock.lock();
                try {
                    // 当前用户不处于待匹配状态 status is In_Match
                    // 做了一个double check，确保在加独占锁后的当前玩家用户的状态是处于In_Match
                    if (matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.IN_GAME) == 0
                            || matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.GAME_OVER) == 0) {
                        log.info("ChatWebsocket matchUser 当前用户 {} 已退出匹配", userId);
                        return;
                    }
                    // 当前用户取消匹配状态
                    if (matchCacheUtil.getUserOnlineStatus(userId).compareTo(StatusEnum.IDLE) == 0) {
                        MessageReply<String> messageReply1 = new MessageReply<>();
                        ChatMessage<String> result1 = new ChatMessage<>();
                        // 当前用户取消匹配
                        messageReply1.setCode(MessageCode.CANCEL_MATCH_ERROR.getCode());
                        messageReply1.setDesc(MessageCode.CANCEL_MATCH_ERROR.getDesc());
                        Set<String> set = new HashSet<>();
                        set.add(userId);
                        result1.setReceivers(set);
                        result1.setType(MessageTypeEnum.CANCEL_MATCH);
                        result1.setData("cancel match");
                        messageReply1.setChatMessage(result1);
                        log.info("ChatWebsocket matchUser 当前用户 {} 已退出匹配", userId);
                        sendMessageAll(messageReply1);
                        return;
                    }
                    receiver = opponentPlayerAddress;
                    if (receiver != null) {
                        // 对手不处于待匹配状态
                        if (matchCacheUtil.getUserOnlineStatus(receiver).compareTo(StatusEnum.IN_MATCH) != 0) {
                            log.info("ChatWebsocket matchUser 当前用户 {}, 匹配对手 {} 已退出匹配状态", userId, receiver);
                        } else {
                            matchCacheUtil.setUserInGame(userId);
                            matchCacheUtil.setUserInGame(receiver);
                            matchCacheUtil.setUserInRoom(userId, receiver);
//                            flag = false;
                        }
                    }
//                    else {
//                        // 如果当前没有待匹配用户，进入等待队列
//                        try {
//                            log.info("ChatWebsocket matchUser 当前用户 {} 无对手可匹配", userId);
//                            matchCond.await();
//                        } catch (InterruptedException e) {
//                            log.error("ChatWebsocket matchUser 匹配线程 {} 发生异常: {}",
//                                    Thread.currentThread().getName(), e.getMessage());
//                        }
//                    }
                } finally {
                    lock.unlock();
                }
//            }

            UserMatchInfo senderInfo = new UserMatchInfo();
            UserMatchInfo receiverInfo = new UserMatchInfo();
            senderInfo.setUserId(userId);
            senderInfo.setScore(0);
            receiverInfo.setUserId(receiver);
            receiverInfo.setScore(0);

            matchCacheUtil.setUserMatchInfo(userId, JSON.toJSONString(senderInfo));
            matchCacheUtil.setUserMatchInfo(receiver, JSON.toJSONString(receiverInfo));

            messageReply.setCode(MessageCode.CONFIRM_MATCH.getCode());
            messageReply.setDesc(MessageCode.CONFIRM_MATCH.getDesc());

            //通知接受游戏匹配的玩家成功匹配了
            Set<String> set = new HashSet<>();
            set.add(userId);
//            set.add(opponentPlayerAddress);
            result.setReceivers(set);
            result.setType(MessageTypeEnum.MATCH_USER);

            MatchInfo matchInfo = new MatchInfo();
            //定义对手玩家的信息
            matchInfo.setOpponentAddress(receiver);
            matchInfo.setOpponentBit(matchCacheUtil.getClient(receiver).bit);
            result.setData(matchInfo);
            messageReply.setChatMessage(result);
            sendMessageAll(messageReply);
            //通知发送游戏匹配的玩家成功匹配了
            set.clear();
            set.add(receiver);
            result.setReceivers(set);
            //定义对手玩家的信息
            matchInfo.setOpponentBit(bit);
            matchInfo.setOpponentAddress(userId);
            result.setData(matchInfo);
            messageReply.setChatMessage(result);
            sendMessageAll(messageReply);

            log.info("ChatWebsocket matchUser 用户随机匹配对手结束 messageReply: {}", JSON.toJSONString(messageReply));
        }, CommonField.MATCH_TASK_NAME_PREFIX + userId);
        matchThread.start();
    }

    /**
     * 对手玩家拒绝匹配邀请
     * @param jsonObject
     */
    private void rejectMatch(JSONObject jsonObject) {
        log.info("ChatWebsocket matchUser 用户拒绝匹配邀请 message: {}, userId: {}", jsonObject.toJSONString(), userId);
        MessageReply<String> messageReply = new MessageReply<>();
        ChatMessage<String> result = new ChatMessage<>();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.REJECT_MATCH);

        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        result.setReceivers(receivers);

        result.setData(userId);
        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.REJECT_MATCH.getCode());
        messageReply.setDesc(MessageCode.REJECT_MATCH.getDesc());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket matchUser 对手玩家拒绝匹配指定对手结束 messageReply: {}", JSON.toJSONString(messageReply));

    }

    /**
     * 用户匹配指定对手
     */
    @SneakyThrows
    private void matchUser(JSONObject jsonObject) {

        log.info("ChatWebsocket matchUser 用户匹配指定对手开始 message: {}, userId: {}", jsonObject.toJSONString(), userId);

        MessageReply<MatchInfo> messageReply = new MessageReply<>();
        ChatMessage<MatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        result.setType(MessageTypeEnum.MATCH_USER);

        String opponentID = jsonObject.getObject("opponentID", String.class);
        lock.lock();
        try {
            // 设置用户状态为匹配中
            matchCacheUtil.setUserInMatch(userId);
            matchCond.signal();
        } finally {
            lock.unlock();
        }

        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentID);
        result.setReceivers(receivers);

        //设置发送邀请的发起者的ID以及赌注给想要匹配的玩家
        MatchInfo matchInfo = new MatchInfo();
        matchInfo.setOpponentAddress(userId);
        matchInfo.setOpponentBit(matchCacheUtil.getClient(userId).bit);
        result.setData(matchInfo);
        messageReply.setChatMessage(result);
        messageReply.setDesc(MessageCode.MATCH_INVITATION.getDesc());
        messageReply.setCode(MessageCode.MATCH_INVITATION.getCode());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket matchUser 用户匹配指定对手结束 messageReply: {}", JSON.toJSONString(messageReply));

    }

    /**
     * 取消匹配
     */
    private void cancelMatch(JSONObject jsonObject) {

        log.info("ChatWebsocket cancelMatch 用户取消匹配开始 userId: {}, message: {}", userId, jsonObject.toJSONString());

        lock.lock();
        try {
            matchCacheUtil.setUserIDLE(userId);
        } finally {
            lock.unlock();
        }

        log.info("ChatWebsocket cancelMatch 用户取消匹配结束 userId: {}", userId);
    }

    /**
     * 游戏中
     */
    @SneakyThrows
    public void toPlay(JSONObject jsonObject) {

        log.info("ChatWebsocket toPlay 用户更新对局信息开始 userId: {}, message: {}", userId, jsonObject.toJSONString());

        MessageReply<UserMatchInfo> messageReply = new MessageReply<>();

        ChatMessage<UserMatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        String receiver = matchCacheUtil.getUserFromRoom(userId);
        Set<String> set = new HashSet<>();
        set.add(receiver);
        result.setReceivers(set);
        result.setType(MessageTypeEnum.PLAY_GAME);

        Integer newScore = jsonObject.getInteger("data");
        UserMatchInfo userMatchInfo = new UserMatchInfo();
        userMatchInfo.setUserId(userId);
        userMatchInfo.setScore(newScore);

        matchCacheUtil.setUserMatchInfo(userId, JSON.toJSONString(userMatchInfo));

        result.setData(userMatchInfo);
        messageReply.setCode(MessageCode.SUCCESS.getCode());
        messageReply.setDesc(MessageCode.SUCCESS.getDesc());
        messageReply.setChatMessage(result);

        sendMessageAll(messageReply);

        log.info("ChatWebsocket toPlay 用户更新对局信息结束 userId: {}, userMatchInfo: {}", userId, JSON.toJSONString(userMatchInfo));
    }

    /**
     * 游戏结束
     */
    public void gameover(JSONObject jsonObject) {

        log.info("ChatWebsocket gameover 用户对局结束 userId: {}, message: {}", userId, jsonObject.toJSONString());

        MessageReply<UserMatchInfo> messageReply = new MessageReply<>();

        ChatMessage<UserMatchInfo> result = new ChatMessage<>();
        result.setSender(userId);
        String receiver = matchCacheUtil.getUserFromRoom(userId);
        result.setType(MessageTypeEnum.GAME_OVER);

        lock.lock();
        try {
            matchCacheUtil.setUserGameover(userId);
            if (matchCacheUtil.getUserOnlineStatus(receiver).compareTo(StatusEnum.GAME_OVER) == 0) {
                messageReply.setCode(MessageCode.SUCCESS.getCode());
                messageReply.setDesc(MessageCode.SUCCESS.getDesc());

                String userMatchInfo = matchCacheUtil.getUserMatchInfo(userId);
                result.setData(JSON.parseObject(userMatchInfo, UserMatchInfo.class));
                messageReply.setChatMessage(result);
                Set<String> set = new HashSet<>();
                set.add(receiver);
                result.setReceivers(set);
                sendMessageAll(messageReply);

                String receiverMatchInfo = matchCacheUtil.getUserMatchInfo(receiver);
                result.setData(JSON.parseObject(receiverMatchInfo, UserMatchInfo.class));
                messageReply.setChatMessage(result);
                set.clear();
                set.add(userId);
                result.setReceivers(set);
                sendMessageAll(messageReply);

                matchCacheUtil.removeUserMatchInfo(userId);
                matchCacheUtil.removeUserFromRoom(userId);
            }
        }  finally {
            lock.unlock();
        }

        log.info("ChatWebsocket gameover 对局 [{} - {}] 结束", userId, receiver);
    }


        @Override
    public int compareTo(ChatWebsocket o) {
        return hitCount.compareTo(o.getHitCount());
    }

    /**
     * 处理过期缓存
     */
    public static class TimeoutTimerThread implements Runnable {
        public void run() {
            while (true) {
                try {
                    if (matchCacheUtil.isFull()){
                        expireCache();
                    }
                    TimeUnit.SECONDS.sleep(60);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        /**
         * 创建多久后，缓存失效
         *
         * @throws Exception
         */
        private void expireCache() throws Exception {
            log.info("检测缓存是否过期缓存");
            for (String key : MatchCacheUtil.getCLIENTS().keySet()) {
                ChatWebsocket cache = MatchCacheUtil.getCLIENTS().get(key);
                long timoutTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()
                        - cache.getWriteTime());

                log.info(String.valueOf(timoutTime));
                log.info(String.valueOf(cache.getExpireTime()));

                if (cache.getExpireTime() > timoutTime) {
                    continue;
                }
                log.info(" 清除过期缓存 ： " + key);
                //清除过期缓存
                // 发送断开session消息
                MessageReply<Object> messageReply = new MessageReply<>();
                messageReply.setCode(MessageCode.Session_Cancel.getCode());
                messageReply.setDesc(MessageCode.Session_Cancel.getDesc());
                ChatMessage<Object> result = new ChatMessage<>();
                result.setSender("服务器端");
                result.setType(MessageTypeEnum.Session_Cancel);
                Set<String> receivers = new HashSet<>();
                receivers.add(key);
                result.setReceivers(receivers);
                messageReply.setChatMessage(result);
                // 在对应的session中调用sendMessageAll
                matchCacheUtil.getClient(key).sendMessageAll(messageReply);
                // 服务器断开与客户端的连接
                matchCacheUtil.getClient(key).session.close();
                matchCacheUtil.removeClient(key);
                matchCacheUtil.removeUserOnlineStatus(key);
                matchCacheUtil.removeUserFromRoom(key);
                matchCacheUtil.removeUserMatchInfo(key);
            }
        }
    }
}

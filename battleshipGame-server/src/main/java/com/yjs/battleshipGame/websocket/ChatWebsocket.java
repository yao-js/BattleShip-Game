package com.yjs.battleshipGame.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yjs.battleshipGame.constant.CommonField;
import com.yjs.battleshipGame.entity.*;
import com.yjs.battleshipGame.entity.Board.BoardGuess;
import com.yjs.battleshipGame.entity.Board.BoardGuessRespond;
import com.yjs.battleshipGame.entity.Board.BoardInfo;
import com.yjs.battleshipGame.entity.Board.BoardResult;
import com.yjs.battleshipGame.error.GameServerError;
import com.yjs.battleshipGame.exception.GameServerException;
import com.yjs.battleshipGame.utils.MatchCacheUtil;
import com.yjs.battleshipGame.utils.MessageCode;
import com.yjs.battleshipGame.utils.MessageTypeEnum;
import com.yjs.battleshipGame.utils.StatusEnum;
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

    // 用来处理提交board后的state，当webSocket端收集到了来自两方玩家的状态后，就通知两方玩家游戏正式开始
    // ready && unready
    private boolean boardState;


//    static QuestionSev questionSev;
    static MatchCacheUtil matchCacheUtil;

    static Lock lock = new ReentrantLock();

    static Condition matchCond = lock.newCondition();


    @Autowired
    public void setMatchCacheUtil(MatchCacheUtil matchCacheUtil) {
        ChatWebsocket.matchCacheUtil = matchCacheUtil;
    }

//    @Autowired
//    public void setQuestionSev(QuestionSev questionSev) {
//        ChatWebsocket.questionSev = questionSev;
//    }

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
            matchCacheUtil.removeUserBoardResult(userId);
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
            matchCacheUtil.removeUserBoardResult(userId);
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
        } else if (type == MessageTypeEnum.GAME_OVER) {
            battleshipGameOver();
        } else if (type == MessageTypeEnum.ENTER_MATCH) {
            this.bit = jsonObject.getObject("bit", String.class);
//            log.info(getBit());
            showAllMatchUserCount(jsonObject);
        } else if (type == MessageTypeEnum.ACCEPT_MATCH){
            acceptMatch(jsonObject);
        } else if (type == MessageTypeEnum.REJECT_MATCH){
            rejectMatch(jsonObject);
        } else if (type == MessageTypeEnum.HANDLE_TIMEOUT_ACCUSE){
            handleTimeoutAccuse(jsonObject);
        } else if (type == MessageTypeEnum.SETUP_BOARD_INFO){
            setupBoardState(jsonObject);
        } else if (type == MessageTypeEnum.BOARD_GUESS){
            playerBoardGuess(jsonObject);
        } else if (type == MessageTypeEnum.RESPOND_TO_BOARD_GUESS){
            respondToBoardGuess(jsonObject);
        } else if (type == MessageTypeEnum.BROADCAST_PLAYER_TURN){
            broadcastPlayerTurn(jsonObject);
        } else if (type == MessageTypeEnum.BROADCAST_GAME_WINNER) {
            broadcastGameWinner(jsonObject);
        } else if (type == MessageTypeEnum.NOTIFY_BOARD_GUESS_SIGNATURE_INVALID){
            notifyBoardGuessSignatureInvalid(jsonObject);
        } else if (type == MessageTypeEnum.NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS){
            notifyAccusedPlayerTimeoutSuccess(jsonObject);
        } else {
            throw new GameServerException(GameServerError.WEBSOCKET_ADD_USER_FAILED);
        }

        log.info("ChatWebsocket onMessage userId: {} 消息接收结束", userId);
    }


    /**
     * 群发消息
     */
    @SneakyThrows
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
    @SneakyThrows
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

    @SneakyThrows
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
            //定义玩家的信息
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
    @SneakyThrows
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
    @SneakyThrows
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
     * 设置当前玩家的boardState为true，意味着准备就绪
     * 并将自己的board commitment以及signature提交给对方玩家
     */
    @SneakyThrows
    private void setupBoardState(JSONObject jsonObject){
        log.info("ChatWebsocket setupBoardState 玩家设置游戏棋盘状态为准备就绪 userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<BoardInfo> messageReply = new MessageReply<>();
        ChatMessage<BoardInfo> result = new ChatMessage<>();
        String opponentBoardCommitment = jsonObject.getObject("boardCommitment", String.class);
        String opponentBoardSignature =jsonObject.getObject("boardSignature", String.class);
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        String stringLock = jsonObject.getObject("stringLock", String.class);
        String contractID = jsonObject.getObject("contractID", String.class);

//        System.out.println("contractID : " +contractID);

        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.SETUP_BOARD_INFO);

        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        result.setReceivers(receivers);

        BoardInfo boardInfo = new BoardInfo();
        boardInfo.setBoardCommitment(opponentBoardCommitment);
        boardInfo.setOpponentBoardSignature(opponentBoardSignature);
        boardInfo.setOpponentStringLock(stringLock);
        boardInfo.setOpponentContractID(contractID);
        result.setData(boardInfo);

        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.SETUP_BOARD_INFO.getCode());
        messageReply.setDesc(MessageCode.SETUP_BOARD_INFO.getDesc());
        sendMessageAll(messageReply);

        //配置一个悲观锁，确保线程的安全
        lock.lock();
        try{
            // 在webSocket端设置这个客户端的状态为就绪状态
            this.boardState = true;
            // 若此时对手玩家的棋盘状态同样为就绪状态，则开始分别向这两个玩家发送游戏开始信号
            // 需要加lock吗？？？？
            // 我觉得是需要的，因为如果两个玩家同时在一个时间提交了board info，webSocket端无法调用以下代码段，因为第一个玩家的这段代码还没跑完，它的state还没变成true，则无法满足下述要求
            if (matchCacheUtil.getClient(opponentPlayerAddress).boardState){
                //两个玩家谁的赌注越大，可以有一个特权，让赌注越大的玩家先进行guess_board
                String firstPlayerTurn = Integer.valueOf(this.bit) >= Integer.valueOf(matchCacheUtil.getClient(opponentPlayerAddress).bit) ? this.userId : opponentPlayerAddress;

                MessageReply<String> messageReply_start = new MessageReply<>();
                ChatMessage<String> result_start = new ChatMessage<>();
                result_start.setSender(userId);
                result_start.setType(MessageTypeEnum.PLAY_GAME);
                // 设置receiver地址ID
                Set<String> receivers_start = new HashSet<>();
                receivers_start.add(opponentPlayerAddress);
                receivers_start.add(userId);
                result_start.setReceivers(receivers_start);
                // 将最先进行guess board的玩家地址传给双方玩家
                result_start.setData(firstPlayerTurn);
                messageReply_start.setChatMessage(result_start);
                messageReply_start.setCode(MessageCode.GAME_IS_STARTED.getCode());
                messageReply_start.setDesc(MessageCode.GAME_IS_STARTED.getDesc());
                sendMessageAll(messageReply_start);
                log.info("ChatWebsocket Game Start 通知双方玩家游戏正式开始 messageReply: {}", JSON.toJSONString(messageReply));
            }
        }finally {
            lock.unlock();
        }

        log.info("ChatWebsocket Game Start 通知双方玩家游戏正式开始结束 messageReply: {}", JSON.toJSONString(messageReply));
    }

    /**
     * 通知对方玩家的board guess signature是无效的
     */
    @SneakyThrows
    private void notifyBoardGuessSignatureInvalid(JSONObject jsonObject){
        log.info("ChatWebsocket notifyBoardGuessSignatureInvalid 通知对手玩家的signature无效 userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<String> messageReply = new MessageReply<>();
        ChatMessage<String> result = new ChatMessage<>();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        String invalidSignature = jsonObject.getObject("invalidSignature", String.class);

        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.NOTIFY_BOARD_GUESS_SIGNATURE_INVALID);
        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        result.setReceivers(receivers);
        result.setData(invalidSignature);

        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.NOTIFY_BOARD_GUESS_SIGNATURE_INVALID.getCode());
        messageReply.setDesc(MessageCode.NOTIFY_BOARD_GUESS_SIGNATURE_INVALID.getDesc());
        sendMessageAll(messageReply);

        log.info("ChatWebsocket notifyBoardGuessSignatureInvalid 通知对手玩家的signature无效 messageReply: {}", JSON.toJSONString(messageReply));

    }

    /**
     * 通知对方玩家指控timeout成功有效
     */
    @SneakyThrows
    private void notifyAccusedPlayerTimeoutSuccess(JSONObject jsonObject){
        log.info("ChatWebsocket notifyBoardGuessSignatureInvalid 通知对手玩家指控的accuse timeout成功有效 userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<String> messageReply = new MessageReply<>();
        ChatMessage<String> result = new ChatMessage<>();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS);
        // 设置receiver地址ID（通知双方玩家）
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        receivers.add(userId);
        result.setReceivers(receivers);
        result.setData(opponentPlayerAddress);

        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS.getCode());
        messageReply.setDesc(MessageCode.NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS.getDesc());
        sendMessageAll(messageReply);

        log.info("ChatWebsocket notifyBoardGuessSignatureInvalid 通知对手玩家指控的accuse timeout成功有效 messageReply: {}", JSON.toJSONString(messageReply));
    }


    /**
     * 向对手玩家传输board guess信息
     */
    @SneakyThrows
    private void playerBoardGuess(JSONObject jsonObject){
        log.info("ChatWebsocket playerBoardGuess 向对手玩家传输board guess userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<BoardGuess> messageReply = new MessageReply<>();
        ChatMessage<BoardGuess> result = new ChatMessage<>();
        // 实例化对手玩家猜测棋盘信息
        BoardGuess boardGuess = new BoardGuess();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        boardGuess.setBoardGuess(jsonObject.getObject("boardGuess", String.class));
        boardGuess.setBoard_i(jsonObject.getObject("i", String.class));
        boardGuess.setBoard_j(jsonObject.getObject("j", String.class));

        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.BOARD_GUESS);

        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        result.setReceivers(receivers);
        result.setData(boardGuess);
        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.PLAYER_BOARD_GUESS.getCode());
        messageReply.setDesc(MessageCode.PLAYER_BOARD_GUESS.getDesc());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket playerBoardGuess 向对手玩家传输board guess结束 messageReply: {}", JSON.toJSONString(messageReply));
    }

    /**
     * 向对手玩家传输the respond of board guess信息
     */
    @SneakyThrows
    private void respondToBoardGuess(JSONObject jsonObject){
        log.info("ChatWebsocket respondToBoardGuess 向对手玩家传输respond of board guess userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<BoardGuessRespond> messageReply = new MessageReply<>();
        ChatMessage<BoardGuessRespond> result = new ChatMessage<>();
        // 实例化棋盘信息的respond
        BoardGuessRespond boardGuessRespond = new BoardGuessRespond();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        boardGuessRespond.setOpponentBoardNonce(jsonObject.getObject("nonce", String.class));
        boardGuessRespond.setOpponentBoardOpening(jsonObject.getObject("opening", String.class));
        boardGuessRespond.setOpponentBoardProof(jsonObject.getObject("proof", String.class));
        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.RESPOND_TO_BOARD_GUESS);

        // 设置receiver地址ID
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        result.setReceivers(receivers);
        result.setData(boardGuessRespond);
        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.RESPOND_TO_BOARD_GUESS.getCode());
        messageReply.setDesc(MessageCode.RESPOND_TO_BOARD_GUESS.getDesc());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket respondToBoardGuess 向对手玩家传输respond of board guess结束 messageReply: {}", JSON.toJSONString(messageReply));
    }

    /**
     * 广播player turn
     */
    @SneakyThrows
    private void broadcastPlayerTurn(JSONObject jsonObject){
        log.info("ChatWebsocket broadcastPlayerTurn broadcast player's turn userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<String> messageReply = new MessageReply<>();
        ChatMessage<String> result = new ChatMessage<>();
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);
        String broadcastTurn = jsonObject.getObject("turn", String.class);
        String boardGuessResult = jsonObject.getObject("boardGuessResult", String.class);

        // 如果当前玩家点击了ship图标并且通过了merkle proof，则将结果信息存入redis cache
        if (boardGuessResult == "true"){
            if (matchCacheUtil.getUserBoardResult(userId) == null){
                BoardResult boardResult = new BoardResult();
                boardResult.setPlayerAddress(userId);
                boardResult.setBoardHitShipCount(1);
                matchCacheUtil.setUserBoardResult(userId, JSON.toJSONString(boardResult));
            } else{
                String boardResultString = matchCacheUtil.getUserBoardResult(userId);
                // 更新是先删除在添加
                matchCacheUtil.removeUserBoardResult(userId);
                BoardResult boardResult = JSON.parseObject(boardResultString, BoardResult.class);
                // 点击了ship图标的次数 + 1
                boardResult.setBoardHitShipCount(boardResult.getBoardHitShipCount() + 1);
                boardResult.setPlayerAddress(userId);
                matchCacheUtil.setUserBoardResult(userId, JSON.toJSONString(boardResult));
            }
        }

        // 如果当前的点击ship图标次数 >=2 的话，则通知该玩家是胜利者
        if (matchCacheUtil.getUserBoardResult(userId) != null){
            if (JSON.parseObject(matchCacheUtil.getUserBoardResult(userId), BoardResult.class).getBoardHitShipCount() >= 2){
                // define winner
                String winner = userId;
                result.setSender(userId);
                result.setType(MessageTypeEnum.PLAYER_CLAIM_WIN);
                result.setData(winner);
                // define receivers
                Set<String> receivers = new HashSet<>();
                receivers.add(opponentPlayerAddress);
                receivers.add(userId);
                result.setReceivers(receivers);

                messageReply.setChatMessage(result);
                messageReply.setCode(MessageCode.PLAYER_CLAIN_WIN.getCode());
                messageReply.setDesc(MessageCode.PLAYER_CLAIN_WIN.getDesc());
                sendMessageAll(messageReply);
                battleshipGameOver();
                log.info("ChatWebsocket playerBoardGuess 玩家宣布胜利 messageReply: {}", JSON.toJSONString(messageReply));
                return;
            }
        }



        //set up sender info
        result.setSender(userId);
        result.setType(MessageTypeEnum.BROADCAST_PLAYER_TURN);
        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        receivers.add(userId);
        result.setReceivers(receivers);
        // 将当前玩家turn广播给双方玩家
        result.setData(broadcastTurn);
        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.BROADCAST_PLAYER_TURN.getCode());
        messageReply.setDesc(MessageCode.BROADCAST_PLAYER_TURN.getDesc());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket playerBoardGuess 向对手玩家传输board guess结束 messageReply: {}", JSON.toJSONString(messageReply));
    }

    /**
     * 通知两个玩家游戏的提前赢家是谁
     */
    @SneakyThrows
    private void broadcastGameWinner(JSONObject jsonObject){
        log.info("ChatWebsocket broadcastGameWinner 用户广播当前游戏的赢家 userId: {}, message: {}", userId, jsonObject.toJSONString());
        MessageReply<String> messageReply = new MessageReply<>();
        ChatMessage<String> result = new ChatMessage<>();

        String winnerAddress = jsonObject.getObject("winnerAddress", String.class);
        String opponentPlayerAddress = jsonObject.getObject("opponentPlayerAddress", String.class);

        result.setData(winnerAddress);
        result.setSender(userId);
        result.setType(MessageTypeEnum.BROADCAST_GAME_WINNER);

        Set<String> receivers = new HashSet<>();
        receivers.add(opponentPlayerAddress);
        receivers.add(userId);
        result.setReceivers(receivers);

        messageReply.setChatMessage(result);
        messageReply.setCode(MessageCode.BROADCAST_GAME_WINNER.getCode());
        messageReply.setDesc(MessageCode.BROADCAST_GAME_WINNER.getDesc());
        sendMessageAll(messageReply);
        battleshipGameOver();
        log.info("ChatWebsocket playerBoardGuess 玩家宣布胜利 messageReply: {}", JSON.toJSONString(messageReply));

    }

    /**
     * 通知对手玩家timeout accuse是无效的
     */
    @SneakyThrows
    private void handleTimeoutAccuse(JSONObject jsonObject) {
        log.info("ChatWebsocket handleTimeoutAccuse 对手用户的timeout accuse无效 userId: {}, message: {}", userId, jsonObject.toJSONString());
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
        messageReply.setCode(MessageCode.HANDLE_TIMEOUT_ACCUSE.getCode());
        messageReply.setDesc(MessageCode.HANDLE_TIMEOUT_ACCUSE.getDesc());
        sendMessageAll(messageReply);
        log.info("ChatWebsocket handleTimeoutAccuse 对手用户的timeout accuse无效结束 messageReply: {}", JSON.toJSONString(messageReply));

    }


    /**
     * 游戏结束
     */
    @SneakyThrows
    public void battleshipGameOver() {

        log.info("ChatWebsocket gameover 用户对局结束 userId: {}", userId);

//        MessageReply<String> messageReply = new MessageReply<>();
//        ChatMessage<String> result = new ChatMessage<>();
//
//        result.setSender(userId);
//        // 这个receiver是与当前userId一起在游戏房间游戏的对手玩家
//        result.setType(MessageTypeEnum.GAME_OVER);
        String receiver = matchCacheUtil.getUserFromRoom(userId);
        lock.lock();
        try {
            // delete all the board info in the redis cache between player and opponent player
            matchCacheUtil.setUserGameover(userId);
            matchCacheUtil.setUserGameover(receiver);
            matchCacheUtil.removeUserBoardResult(userId);
            matchCacheUtil.removeUserFromRoom(userId);
            matchCacheUtil.removeUserBoardResult(receiver);
            matchCacheUtil.removeUserFromRoom(receiver);

//            if (matchCacheUtil.getUserOnlineStatus(receiver).compareTo(StatusEnum.GAME_OVER) == 0) {
//                messageReply.setCode(MessageCode.SUCCESS.getCode());
//                messageReply.setDesc(MessageCode.SUCCESS.getDesc());

//                String userMatchInfo = matchCacheUtil.getUserBoardResult(userId);
//                result.setData(JSON.parseObject(userMatchInfo, UserMatchInfo.class));
//                messageReply.setChatMessage(result);
//                Set<String> set = new HashSet<>();
//                set.add(receiver);
//                result.setReceivers(set);
//                sendMessageAll(messageReply);
//
//                String receiverMatchInfo = matchCacheUtil.getUserBoardResult(receiver);
//                result.setData(JSON.parseObject(receiverMatchInfo, UserMatchInfo.class));
//                messageReply.setChatMessage(result);
//                set.clear();
//                set.add(userId);
//                result.setReceivers(set);
//                sendMessageAll(messageReply);
//            }
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
                matchCacheUtil.removeUserBoardResult(key);
            }
        }
    }
}

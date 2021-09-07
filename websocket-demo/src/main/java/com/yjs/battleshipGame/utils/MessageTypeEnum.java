package com.yjs.battleshipGame.utils;

/**
 * 消息类型
 *
 * @author yeeq
 */
public enum MessageTypeEnum {

    /**
     * 用户加入
     */
    ADD_USER,
    /**
     * 匹配对手
     */
    MATCH_USER,
    /**
     * 取消匹配
     */
    CANCEL_MATCH,
    /**
     * 游戏开始
     */
    PLAY_GAME,
    /**
     * 游戏结束
     */
    GAME_OVER,
    /**
     * 取消连接
     */
    Session_Cancel,
    /**
     *  进入匹配大厅
     */
    ENTER_MATCH,
    /**
     *  匹配大厅为空
     */
    MATCH_NULL,
    /**
     * 位置指定页面找到相应的玩家
     */
    FIND_PAGE,
    /**
     * 对手玩家确认匹配邀请
     */
    ACCEPT_MATCH,
    /**
     * 对手玩家取消匹配邀请
     */
    REJECT_MATCH,
    /**
     * 玩家当前处于游戏中
     */
    IN_GAME,
    /**
     * 玩家当前处于匹配状态
     */
    IN_MATCH,
    /**
     * 玩家已经建立了Session连接
     */
    IN_SESSION,
    /**
     * 玩家处理了timeout accuse的请求
     */
    HANDLE_TIMEOUT_ACCUSE,
    /**
     * 接收对方玩家传过来的棋盘信息
     */
    SETUP_BOARD_INFO,
    /**
     * 接收对方玩家传过来的board guess信息
     */
    BOARD_GUESS,
    /**
     * 接收对方玩家传过来的the respond of board guess
     */
    RESPOND_TO_BOARD_GUESS,
    /**
     * 广播当前玩家的turn
     */
    BROADCAST_PLAYER_TURN,
    /**
     * 宣布当前玩家是赢家
     */
    PLAYER_CLAIM_WIN,
    /**
     * 广播游戏的赢家
     */
    BROADCAST_GAME_WINNER,
    /**
     * 通知对方玩家的board guess signature无效
     */
    NOTIFY_BOARD_GUESS_SIGNATURE_INVALID,
    /**
     * 通知对方玩家指控的accuse timeout成功
     */
    NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS;
}
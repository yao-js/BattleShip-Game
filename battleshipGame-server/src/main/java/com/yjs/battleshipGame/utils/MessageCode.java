package com.yjs.battleshipGame.utils;

import lombok.Getter;

/**
 * 响应码
 *
 * @author yeeq
 * @date 2021/3/27
 */
@Getter
public enum MessageCode {

    /**
     * 响应码
     */
    SUCCESS(2000, "连接成功"),
    USER_IS_ONLINE(2001, "用户已存在"),
    CURRENT_USER_IS_INGAME(2002, "当前用户已在游戏中"),
    MESSAGE_ERROR(2003, "消息错误"),
    CANCEL_MATCH_ERROR(2004, "用户取消了匹配"),
    RESULT_NULL(2005, "结果为空"),
    Session_Cancel(2006, "因用户长时间未操作而断开连接"),
    QUERY_ALL_USER(2007, "用户查询当前所有处在匹配大厅的用户"),
    QUERY_PLAYER_PAGE(2008, "查询某一页的玩家信息"),
    MATCH_INVITATION(2009, "发送匹配邀请给指定玩家用户"),
    ACCEPT_MATCH(2010, "对手玩家同意匹配邀请"),
    REJECT_MATCH(2011, "对手玩家拒绝匹配邀请"),
    CONFIRM_MATCH(2012, "两位玩家匹配游戏成功"),
    USER_IS_NULL(2013,"玩家已建立session连接"),
    HANDLE_TIMEOUT_ACCUSE(2014, "玩家的timeout accuse无效"),
    SETUP_BOARD_INFO(2015,"接收对方玩家棋盘的信息"),
    GAME_IS_STARTED(2016, "游戏正式开始"),
    PLAYER_BOARD_GUESS(2017, "对方玩家猜测棋盘"),
    RESPOND_TO_BOARD_GUESS(2018, "玩家反馈棋盘猜测结果"),
    BROADCAST_PLAYER_TURN(2019,"广播当前玩家轮数"),
    PLAYER_FORFEIT_GAME(2020, "玩家认输退出游戏"),
    PLAYER_CLAIN_WIN(2021, "玩家宣布胜利"),
    BROADCAST_GAME_WINNER(2022, "提前宣布游戏赢家"),
    NOTIFY_BOARD_GUESS_SIGNATURE_INVALID(2023, "通知对方玩家签名无效"),
    NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS(2024, "通知对方玩家accuse timeout有效成功");

    private final Integer code;
    private final String desc;

    MessageCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package com.yeeq.game.utils;

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
    USER_IS_NULL(2013,"玩家已建立session连接");

    private final Integer code;
    private final String desc;

    MessageCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

package com.yjs.battleshipGame.entity;

import lombok.Data;

import java.util.List;

/**
 * @author yeeq
 * @date 2021/4/4
 */
@Data
public class GameMatchInfo {

    private UserMatchInfo selfInfo;
    private UserMatchInfo opponentInfo;
    private List<Question> questions;
}

package com.yjs.battleshipGame.entity.Board;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BoardGuess {
    // 对手玩家猜测的棋盘位置
    private String board_i;
    private String board_j;
    // 对手玩家猜测的签名棋盘信息
    private String boardGuess;

}

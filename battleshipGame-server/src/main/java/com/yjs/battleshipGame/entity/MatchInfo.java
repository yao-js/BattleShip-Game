package com.yjs.battleshipGame.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchInfo {
    // 想要匹配的对手地址
    private String opponentAddress;
    // 想要匹配的对手设置赌注
    private String opponentBit;
}

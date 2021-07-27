package com.yeeq.game.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageInfo {
    private List<PlayerInfo> playerInfoList;

    private int totalPageNums;
}

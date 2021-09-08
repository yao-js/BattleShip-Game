package com.yjs.battleshipGame.entity.Board;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BoardGuessRespond {

    // 对方玩家对于board guess的respond
    private String opponentBoardOpening;
    private String opponentBoardProof;
    private String opponentBoardNonce;

}

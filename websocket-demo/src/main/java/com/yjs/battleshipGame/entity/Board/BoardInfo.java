package com.yjs.battleshipGame.entity.Board;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BoardInfo {
    // board's merkle root commitment
    private String boardCommitment;
    //opponent player signature
    private String opponentBoardSignature;
    // opponent player's stringLock
    private String opponentStringLock;
    // opponent player's contractID
    private String opponentContractID;

}


// 记录玩家实体类信息
let player = null;
// 记录玩家是否准备就绪
let player_ready = false;
// 判断当前是由哪个玩家进行guess board
let playerTurn = -1

let board_i = null;
let board_j = null;

// 记录player被accuse timeout的次数
let timeOutAccusedCount = 0;

// ===============================================================
//                          SETUP GAME
// ===============================================================

function setup_game(playerAddress, opponentPlayerAddress){
    // $('#subtitle').html('Initializing a Game');
    enter_battleship_placing_ui();
    player = new BattleshipPlayer(playerAddress, opponentPlayerAddress);
    // console.log(player);
}

// ===============================================================
//                          PLAY GAME
// ===============================================================
// Play Game function area
// function called when a user guesses a square
async function guess_square(i, j, player){
    // build singature on the guess
    let signed_guess = await player.build_guess(i, j);
    board_i = i;
    board_j = j;
    // send guess and signature to opponent and receive response
    // let [opening, nonce, proof] = opponentPlayer.respond_to_guess(i, j, signed_guess);
    // update my-board with the outcome of the guess (the update relies on the value of response)
    // $('#' + opponentPlayer.myAddress + ' > .my-board #' + i + '-' + j)
    //     .css('background-image', 'url(' + (opening? EXPLOSION_IMG: SPLASH_IMG) + ')');
    // interpret response
    // await player.receive_response_to_guess(i, j, [opening, nonce, proof]);
    // return if the guess hit a ship
    // callback(opening);
    return signed_guess;
}

function respond_guess(player, i, j, signed_guess){
    // send guess and signature to opponent and receive response
    let [opening, nonce, proof] = player.respond_to_guess(i, j, signed_guess);
    $('#player > .my-board #' + i + '-' + j)
        .css('background-image', 'url(' + (opening? EXPLOSION_IMG: SPLASH_IMG) + ')');
    // callback(opening);
    return [opening, nonce, proof];
}

async function receive_response_guess(i, j, player, opening, nonce, proof, callback){
    await player.receive_response_to_guess(i, j, [opening, nonce, proof]);
    // 传递结果
    callback(opening);
}

// 确保两个玩家都已经准备开始
// function prepare_game() {
//     leave_battleship_placing_ui();
//     enter_game()
//     player_ready = true;
//     // if (player_ready && opponent_ready) {
//     //     turn = 0;
//     //     enter_playing_game_ui(guess_square);
//     // }
//     return player_ready;
// }


// 若收到webSocket的游戏就绪的通知，将调用这个function并进入游戏界面
async function enter_game() {
    if (checkWebSocketStatus()) {
        return;
    }
    // 确保玩家提交了board后无法继续操作board initialize操作
    leave_battleship_placing_ui();

    // 设置有效时间为3分钟后
    let timeLock =  new Date().valueOf() + 180000;
    // call the store_bid function which defined in evm to store battleship game player bit and eth address
    await player.store_bid(playerAddress, opponentPlayer, bit, player.stringLock, timeLock).then(r => console.log("Store Bid Successful!"));

    // 调用evm中的store_board_commitment函数来存储board merkle commitment到evm中
    let [commitment, signature] = await player.initialize_board(parse_my_board());

    signature.then((sig) => {
        // console.log("player.contractID: " + contractID);
        storeBoardInfo(commitment, sig, opponentPlayer, contractID, player.stringLock);
    });
    // enter_playing_game_ui(player, guess_square);
}

// ===============================================================
//                          END GAME
// ===============================================================

function battleship_game_over(){
    alert("The game is over, please check your account balance! If you have any question about this game, please look for ethereum record");
    // inform the webSocket server that the game is over, and remove the status of two battleship players which set up in redis cache
    gameOver();
}

async function accuse_timeout(){
    suspend_game_ui();
    await player.accuse_timeout().then(r => console.log("accuse opponent player's operation is timeout"));
}

async function handle_timeout_accusation(){
    await player.handle_timeout_accusation();
}

async function accuse_timeout_win(){
    await player.claim_timeout_winnings();
}

async function forfeit_game(){

    if (confirm("请确认是否继续进行退出游戏申请？")){
        player.forfeitGame().then(r => alert("操作成功"));
    }
    // player.forfeitGame().then(r => broadcastGameWinner(opponentPlayer, opponentPlayer));
    // 通知双方玩家游戏提前结束, 若玩家A点击forfeit游戏按钮，则玩家B是winner，并且opponentPlayer也是玩家B
    // broadcastGameWinner(opponentPlayer, opponentPlayer);
}

async function forfeit_cheating_timeout_claimWin(){
    await player.forfeit_cheating_timeout_claimWin();
}

async function claim_win(){
   await player.claim_win();
}

async function accuse_cheating(){
    await player.accuse_cheating();
}
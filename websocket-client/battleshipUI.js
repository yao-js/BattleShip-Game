// setup the timeout seconds
const secs = 10;

let placeShipCount = 0;

let lastTimeoutID = 0;

// 建立并设置玩家用户的棋盘
function enter_battleship_placing_ui(){
    // build board for player
    function build_battleship_table(user, type) {
        table = '<caption style="caption-side: top">' + '<h2>' + type+ '</h2>'  + '</caption>';
        for (let i = 0; i < BOARD_LEN; i++) {
            table += '<tr>';
            for (let j = 0; j < BOARD_LEN; j++) {
                table += '<td ';
                table += 'id=' + i + '-' + j + ' ';
                table += 'class=battleship-board-square>';
                table += '</td>';
            }
            table += '</tr>';
        }
        return table;
    }
    // update the page
    $('#player > .my-board').html(build_battleship_table('player1', 'my board'));
    $('#player > .their-board').html(build_battleship_table('player1', 'opponent board'));
    $('.my-board .battleship-board-square').hover(
        function() { $(this).css('background-color', 'blue'); },
        function() { $(this).css('background-color', 'transparent'); }
    );
    $('#subtitle').html('Players are Placing Boats');

    // 当点击表格时，添加ship图标
    function addShip() {
        placeShipCount = placeShipCount + 1;
        let imageUrl = './media/boat.png'
        $(this).css('background-image', 'url(' + imageUrl + ')');
        $(this).attr('name', 'boat');
        // 需要先解绑之前所绑定的click事件，再绑定另一个click事件
        $(this).unbind("click");
        $(this).click(removeShip);
    }
    // 当点击表格时，移除ship图标
    function removeShip() {
        placeShipCount = placeShipCount-1;
        $(this).css('background-image', 'none');
        $(this).attr('name', '');
        // 需要先解绑之前所绑定的click事件，再绑定另一个click事件
        $(this).unbind("click");
        $(this).click(addShip);
    }
    $('.my-board .battleship-board-square').click(addShip);
    $('.my-board .battleship-board-square').hover(
        function() { $(this).css('background-color', 'blue'); },
        function() { $(this).css('background-color', 'transparent'); }
    );

    //set up the battleship submission button
    $('#game-option').html(

        '<span>游戏选项：</span>\
               <button id="next-step">Submit Battleships</button>'
    );
    $('#game-option > #next-step').click(() => {
        console.log(placeShipCount);
        if (placeShipCount == NUM_SHIPS){enter_game().then(r => console.log("success"))}
        else {alert("您放置的船只个数不满足要求（2个），请再确认放置的船只个数！"); return;}
    });

}

// 离开放置船只的阶段
function leave_battleship_placing_ui(){
    $('#subtitle').html('Wait for the game to start');
    $('#player' + ' .my-board .battleship-board-square').unbind();
    $('#player' + ' .my-board .battleship-board-square').off('hover');
}

// 进入游戏阶段
async function enter_playing_game_ui(player, guess_square, turn){

    $('#connect').hide();
    $('#sendUserId').hide();
    $('#match').hide();
    $('#cancelMatch').hide();
    playerTurn = turn;
    // start hover style on their-board
    $('#player .their-board .battleship-board-square').hover(
        function() { $(this).css('background-color', 'red'); },
        function() { $(this).css('background-color', 'transparent'); }
    );

    // accuse timeout 这个按钮先由后端服务器进行一个判断，后续再实现为由玩家用户自己进行一个accuse，通过可视界面来操作这个accuse timeout
    $('#game-option').html(
        '<span>游戏选项：</span>\
        <button id="forfeitGame" onclick="forfeit_game()">Forfeit Game</button>\
        <button id="accuseTimeout" onclick="accuse_timeout()">Accuse Timeout (10s)</button>'
    )

    if (playerTurn == playerAddress){
        $('#subtitle').html('It is your turn');
        $('#accuseTimeout').attr('disabled', 'true');
    } else {
        $('#subtitle').html("It is opponent player's turn");
        setupAccuseTimeoutFunction();
    }
    // setup the buttons of end game type
    // $('#player > #game-option > #forfeitGame').click(() => { forfeit_game(player); });
    // $('#player > #game-option > #claimWin').click(() => { claim_win(player); });
    // $('#player > #game-option > #accuseCheating').click(() => { accuse_cheating(player); });
    // setupTimeoutFunction();
    // $('#player > #game-option > #accuseTimeout').click(() => { accuse_timeout(); });

    $('#player .their-board .battleship-board-square').click(function () {
        if (playerAddress !== playerTurn) return;// only parse guess if it is the player's turn
        // get the i, j of the guess
        let [i, j] = $(this).attr('id').split('-');
        // 若之前已经点击了对手棋盘的话，则无法继续点击这个位置
        if (player.opponent_board[i][j] != 0){
            console.log("This position in the opponent's board has been hit before");
            return;
        }
        let signed_guess = guess_square(parseInt(i), parseInt(j), player);

        signed_guess.then((sign) => {
            playerGuessBoard(sign, opponentPlayer, i, j);
        });
    });
}

// 当用户收到board guess后，用于更新玩家用户界面
function update_playing_game_ui(){

    // 先清除之前所设置的所有定时器
    if (playerTurn == playerAddress){
        for (let i = 1; i <= lastTimeoutID; i++){
            clearTimeout(i);
        }
    }

    $('#game-option').html(
        '<span>游戏选项：</span>\
        <button id="forfeitGame" onclick="forfeit_game()">Forfeit Game</button>\
        <button id="accuseTimeout" onclick="accuse_timeout()">Accuse Timeout (10s)</button>'
    )

    if (playerTurn == playerAddress){
        $('#subtitle').html('It is your turn');
        $('#accuseTimeout').attr('disabled', 'true');
    } else {
        $('#subtitle').html("It is opponent player's turn");
        setupAccuseTimeoutFunction();
    }
}

// suspend the game(暂停游戏界面)
function suspend_game_ui(){
    // unbind click and hover on both player's boards
    $('#player .their-board .battleship-board-square').css('background-color', 'transparent');
    $('#player .their-board .battleship-board-square').unbind();
    $('#player .their-board .battleship-board-square').off('hover');
    $('.battleship-board-square').css('opacity', '0.4');
    // $('#forfeitGame').attr('disabled', 'true');
    $('#accuseTimeout').attr('disabled', 'true');

    $('#subtitle').html("Accuse Timeout Phase");

}

// restart the game(离开暂停游戏阶段，重新进入游戏阶段)
function restart_game_ui(){

    $('#game-option').html(
        '<span>游戏选项：</span>\
        <button id="forfeitGame" onclick="forfeit_game()">Forfeit Game</button>\
        <button id="accuseTimeout" onclick="accuse_timeout()">Accuse Timeout (10s)</button>'
    )

    $('.battleship-board-square').css('opacity', '');
    // 将游戏选项回归初始状态
    // $('#forfeitGame').removeAttr('disabled');
    if (playerTurn == playerAddress){
        $('#subtitle').html('It is your turn');
        $('#accuseTimeout').attr('disabled', 'true');
    } else {
        $('#subtitle').html("It is opponent player's turn");
        setupAccuseTimeoutFunction();
    }

    // start hover style on their-board
    $('#player .their-board .battleship-board-square').hover(
        function() { $(this).css('background-color', 'red'); },
        function() { $(this).css('background-color', 'transparent'); }
    );
    $('#player .their-board .battleship-board-square').bind('click', function (){
        if (playerAddress !== playerTurn) return;// only parse guess if it is the player's turn
        // get the i, j of the guess
        let [i, j] = $(this).attr('id').split('-');
        // 若之前已经点击了对手棋盘的话，则无法继续点击这个位置
        if (player.opponent_board[i][j] != 0){
            console.log("This position in the opponent's board has been hit before");
            return;
        }
        let signed_guess = guess_square(parseInt(i), parseInt(j), player);

        signed_guess.then((sign) => {
            playerGuessBoard(sign, opponentPlayer, i, j);
        });
    });
}

// 设置respond timeout function的按钮
function setResponseTimeOut_ui(){
    // 重新渲染游戏选项div
    $('#game-option').html(
        '<span>游戏选项：</span>\
        <button id="forfeitGame" onclick="forfeit_game()">Forfeit Game</button>\
        <button id="handleAccusedTimeout">Response Accuse Timeout</button>'
    )

    // 再实现一个setTime机制
    // 要求玩家在10s内完成response timeout accuse，不然就通知对方玩家执行accuse timeout win函数
    setupAccuseTimeoutRespondFunction();

    // unbind click and hover on both player's boards
    $('#player .their-board .battleship-board-square').css('background-color', 'transparent');
    $('#player .their-board .battleship-board-square').unbind();
    $('#player .their-board .battleship-board-square').off('hover');
    $('.battleship-board-square').css('opacity', '0.4');
    // $('#forfeitGame').attr('disabled', 'true');
    // $('#accuseTimeout').attr('disabled', 'true');

    $('#subtitle').html("You are being accused timeout");


}

// 结束游戏界面设置
function end_game_ui(winnerAddress){
    // unbind click and hover on both player's boards
    $('#player .their-board .battleship-board-square').css('background-color', 'transparent');
    $('#player .their-board .battleship-board-square').unbind();
    $('#player .their-board .battleship-board-square').off('hover');
    // make all boards transparent
    $('.battleship-board-square').css('opacity', '0.4');
    if (winnerAddress == playerAddress){
        $('#subtitle').html("You are winner!");
    } else{
        $('#subtitle').html("Your opponent player is winner!");
    }
    $('#gameState').html("游戏已经结束！");
    $('#game-option').html(
        '<span>游戏选项：</span>\
        <button id="nextGame" value="Next Game">Next Game</button>'
    )
    // 设置进行下一局游戏按钮功能
    $('#game-option #nextGame').click(function (){
        alert("请重新连接游戏服务器，以及重新设置您下的赌注并进入匹配大厅!");
        window.location.reload();
    });
}

function accuseTimeoutUpdate(num){
    if(num == secs) {
        $('#accuseTimeout').html("Accuse Timeout");
        // document.getElementById("accuseTimeout").attributes('disabled') = false;
        $('#accuseTimeout').removeAttr('disabled');
        // $('#player > #game-option > #accuseTimeout').click(() => { accuse_timeout().then(r => console.log("good")); });
        // $('#accuseTimeout').attr('disabled', 'false');
    }else {
        let printnr = secs - num;
        $('#accuseTimeout').html("Accuse Timeout (" + printnr + "s)");
    }
}

// 为accuse timeout按钮添加一个定时器功能
function setupAccuseTimeoutFunction(){
    $('#accuseTimeout').attr('disabled', 'true');
    for (let i=1; i<=secs; i++){
        lastTimeoutID = window.setTimeout("accuseTimeoutUpdate(" + i + ")", i * 1000);
    }
}

// 为Response timeout accuse按钮添加一个定时器功能
function setupAccuseTimeoutRespondFunction(){
    // 判断handle accused timeout按钮是否点击了
    let isClick = false;
    let count = 0;
    $(document).on('click', function (e){
        if (e.target.id == "handleAccusedTimeout"){
            isClick =true;
        }
    })

    let timeoutId = window.setInterval(function () {
        count = count + 1;
        console.log("isClick " + isClick);
        console.log("count " + count);
        // 如果当前用户点击了响应按钮，则调用evm handle_timeout function，并通过event通知对方玩家处理成功
        if (isClick == true){
            alert("当前玩家响应了操作超时指控，游戏继续");
            handle_timeout_accusation().then(r => restart_game_ui());
            clearInterval(timeoutId);
            return;
        }else if (count == 6 && isClick == false){ // 设置响应时间为五秒左右
            alert("你未在有效时间内未点击响应按钮，游戏即将结束！");
            // 通知对手玩家被指控超时的玩家未能在有效时间内做出响应
            notifyAccuseTimeoutSuccess(opponentPlayer);
            clearInterval(timeoutId);
            return;
        }
    }, 1000);
}
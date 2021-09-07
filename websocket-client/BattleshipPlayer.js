// let web3 = new Web3(Web3.givenProvider || "https://rinkeby.infura.io/v3/6c355a1bd0d7487e868ae499d320b4d9");

let abi = [
    {
        "inputs": [],
        "stateMutability": "payable",
        "type": "constructor"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            },
            {
                "indexed": false,
                "internalType": "address",
                "name": "sender",
                "type": "address"
            }
        ],
        "name": "AccuseTimeout",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            },
            {
                "indexed": false,
                "internalType": "bool",
                "name": "result",
                "type": "bool"
            }
        ],
        "name": "CheckPlayerShipResult",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            },
            {
                "indexed": false,
                "internalType": "address",
                "name": "sender",
                "type": "address"
            },
            {
                "indexed": false,
                "internalType": "bool",
                "name": "result",
                "type": "bool"
            }
        ],
        "name": "HandleTimeoutAccuse",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            },
            {
                "indexed": false,
                "internalType": "bytes32",
                "name": "_contractId",
                "type": "bytes32"
            },
            {
                "indexed": false,
                "internalType": "bytes32",
                "name": "_hashlock",
                "type": "bytes32"
            }
        ],
        "name": "addContract",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            }
        ],
        "name": "informPlayerCheating",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {
                "indexed": true,
                "internalType": "address",
                "name": "defendant",
                "type": "address"
            }
        ],
        "name": "requestForfeitGame",
        "type": "event"
    },
    {
        "inputs": [
            {
                "internalType": "bytes",
                "name": "opening_nonce",
                "type": "bytes"
            },
            {
                "internalType": "bytes32[]",
                "name": "proof",
                "type": "bytes32[]"
            },
            {
                "internalType": "uint256",
                "name": "guess_leaf_index",
                "type": "uint256"
            },
            {
                "internalType": "address",
                "name": "owner",
                "type": "address"
            },
            {
                "internalType": "bytes32",
                "name": "winner_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "bytes32",
                "name": "opponent_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "string",
                "name": "opponent_preimage",
                "type": "string"
            },
            {
                "internalType": "string",
                "name": "winner_preimage",
                "type": "string"
            }
        ],
        "name": "accuse_cheating",
        "outputs": [
            {
                "internalType": "bool",
                "name": "result",
                "type": "bool"
            }
        ],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes",
                "name": "opening_nonce",
                "type": "bytes"
            },
            {
                "internalType": "bytes32[]",
                "name": "proof",
                "type": "bytes32[]"
            },
            {
                "internalType": "uint256",
                "name": "guess_leaf_index",
                "type": "uint256"
            },
            {
                "internalType": "address",
                "name": "owner",
                "type": "address"
            }
        ],
        "name": "check_one_ship",
        "outputs": [
            {
                "internalType": "bool",
                "name": "result",
                "type": "bool"
            }
        ],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes32",
                "name": "winner_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "bytes32",
                "name": "opponent_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "string",
                "name": "opponent_preimage",
                "type": "string"
            },
            {
                "internalType": "string",
                "name": "winner_preimage",
                "type": "string"
            }
        ],
        "name": "claim_game_win",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address",
                "name": "opponentPlayer",
                "type": "address"
            },
            {
                "internalType": "bytes32",
                "name": "contractID",
                "type": "bytes32"
            }
        ],
        "name": "claim_opponent_left",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address",
                "name": "opponentPlayer",
                "type": "address"
            },
            {
                "internalType": "bytes32",
                "name": "winner_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "bytes32",
                "name": "opponent_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "string",
                "name": "opponent_preimage",
                "type": "string"
            },
            {
                "internalType": "string",
                "name": "winner_preimage",
                "type": "string"
            }
        ],
        "name": "claim_timeout_winnings",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address",
                "name": "opponentPlayer",
                "type": "address"
            }
        ],
        "name": "forfeit",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes32",
                "name": "winner_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "bytes32",
                "name": "opponent_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "string",
                "name": "opponent_preimage",
                "type": "string"
            },
            {
                "internalType": "string",
                "name": "winner_preimage",
                "type": "string"
            }
        ],
        "name": "forfeit_cheating_timeout_claimWin",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "getBalance",
        "outputs": [
            {
                "internalType": "uint256",
                "name": "",
                "type": "uint256"
            }
        ],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address",
                "name": "opponentPlayer",
                "type": "address"
            }
        ],
        "name": "handle_timeout",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address",
                "name": "opponentPlayer",
                "type": "address"
            }
        ],
        "name": "is_game_over",
        "outputs": [
            {
                "internalType": "bool",
                "name": "",
                "type": "bool"
            }
        ],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes32",
                "name": "a",
                "type": "bytes32"
            },
            {
                "internalType": "bytes32",
                "name": "b",
                "type": "bytes32"
            }
        ],
        "name": "merge_bytes32",
        "outputs": [
            {
                "internalType": "bytes",
                "name": "",
                "type": "bytes"
            }
        ],
        "stateMutability": "pure",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes32",
                "name": "_contractId",
                "type": "bytes32"
            }
        ],
        "name": "refund",
        "outputs": [
            {
                "internalType": "bool",
                "name": "",
                "type": "bool"
            }
        ],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "address payable",
                "name": "player",
                "type": "address"
            },
            {
                "internalType": "address payable",
                "name": "opponent",
                "type": "address"
            },
            {
                "internalType": "uint256",
                "name": "bit",
                "type": "uint256"
            },
            {
                "internalType": "string",
                "name": "hash",
                "type": "string"
            },
            {
                "internalType": "uint256",
                "name": "_timelock",
                "type": "uint256"
            }
        ],
        "name": "storeBit",
        "outputs": [
            {
                "internalType": "bytes32",
                "name": "contractId",
                "type": "bytes32"
            }
        ],
        "stateMutability": "payable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes32",
                "name": "merkle_root",
                "type": "bytes32"
            }
        ],
        "name": "store_board_commitment",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {
                "internalType": "bytes",
                "name": "opening_nonce",
                "type": "bytes"
            },
            {
                "internalType": "bytes32[]",
                "name": "proof",
                "type": "bytes32[]"
            },
            {
                "internalType": "uint256",
                "name": "guess_leaf_index",
                "type": "uint256"
            },
            {
                "internalType": "bytes32",
                "name": "commit",
                "type": "bytes32"
            }
        ],
        "name": "verify_opening",
        "outputs": [
            {
                "internalType": "bool",
                "name": "result",
                "type": "bool"
            }
        ],
        "stateMutability": "pure",
        "type": "function"
    }
]

abiDecoder.addABI(abi);

let contractAddress = "0x8F3C260E91B537aC7cD003614b8fB4315102bF3B";

let Battleship = new web3.eth.Contract(abi, contractAddress);

let contractID = null;

class BattleshipPlayer{
    constructor(myAddress, opponentAddress) {
        this.myAddress = myAddress;
        this.opponentAddress = opponentAddress;
        this.guesses = Array(BOARD_LEN).fill(Array(BOARD_LEN).fill(false));
        this.my_board = null;
        this.opponentContractID = null;
        this.stringLock = randomString(6);
        this.opponentStringLock = null;
        //0: initial state; 1: hit the opponent ship; 2: miss
        this.opponent_board = new Array();
        for(let x=0; x < BOARD_LEN; x++){
            this.opponent_board[x]=new Array();
            for(let y=0; y < BOARD_LEN; y++){
                this.opponent_board[x][y] = 0; //initialize with 0
            }
        }

        // 当用户调用了evm上的storeBit方法时候，监听器将返回的contractID赋值给这个用户的bitContractID属性
        // 每个用户都对应于独一无二的bitContractID
        Battleship.events.addContract({filter: {defendant: this.myAddress}}, function (err, event){
            if (err){
                return console.log(err);
            }
            contractID = event.returnValues._contractId;
            console.log("contract ID: " + contractID);
        });
        //register a event listener that listens to events from the smart contract.
        //only when the defendant is me, then call "alert" function.
        Battleship.events.AccuseTimeout({filter: {defendant: this.myAddress}}, function (err, event) {
            if (err) {
                return console.log(err);
            }
            timeOutAccusedCount = timeOutAccusedCount + 1;
            // suspend the game
            // suspend_game_ui();
            // set up the accused player UI
            setResponseTimeOut_ui();

            // let startTime = new Date();
            // // 如何对这个确认选项做一个倒计时功能？(借助两个时间戳来实现, 判断是否超过一分钟)
            // if (confirm(event.returnValues.defendant + " is accused. You get 1 min to response it. Please confirm whether response the timeout accuse ?")){
            //     // handle_timeout_accusation().then((result) => alert("response timeout accuse successful!"));
            //     let endTime = new Date();
            //     let totalTime = endTime - startTime;
            //     let minutes = Math.floor(totalTime / (60 * 1000));
            //     if (minutes <= 1){
            //         // handle_timeout_accusation().then((result) => alert("response timeout accuse successful!"));
            //         // alert("response timeout accuse successful!");
            //         restart_game_ui();
            //     } else{
            //         alert("The operation is timeout for responding the accuse!");
            //     }
            // } else {
            //     // 通知双方玩家游戏结束
            //     alert("give up the game");
            // }
        });

        // 当对方玩家点击respond timeout accuse后，evm event会收到对应的日志记录，并通过过滤器反馈给对应的玩家结果
        Battleship.events.HandleTimeoutAccuse({filter: {defendant: this.myAddress}}, function (err, event) {
            if (err) {
                return console.log(err);
            }
            let handleTimeoutAccuseResult = event.returnValues.result;
            // 若收到accuse timeout的回应后，则恢复游戏状态
            restart_game_ui();
            console.log("handleTimeoutAccuseResult: " + handleTimeoutAccuseResult);

        });

        // 当玩家点击forfeit game后，evm event会收到对应的日志记录，并通过过滤器反馈给对方玩家该forfeit game请求
        Battleship.events.requestForfeitGame({filter: {defendant: this.myAddress}}, function (err, event) {
            if (err) {
                return console.log(err);
            }
            alert("您收到对方玩家的退出游戏的请求，游戏即将结束");
            forfeit_cheating_timeout_claimWin().then(r => broadcastGameWinner(playerAddress, opponentPlayer));
        });

        // 当某玩家点击中了ship，则截取evm上的check one ship function的结果
        Battleship.events.CheckPlayerShipResult({filter: {defendant: this.myAddress}}, function (err, event){
            if (err) {
                return console.log(err);
            }
            let checkResult = event.returnValues.result;
            console.log("checkResult: " + checkResult);
            if (checkResult == "false"){
                // 调用claim win function (直接结束游戏，通知双方玩家有人出老千)
                alert("对方玩家存在不规范游戏行为，游戏提前结束，您是赢家！");
                accuse_cheating().then(r => broadcastGameWinner(playerAddress, opponentPlayer));
            }
        });

        // 当某玩家发起accuse cheating请求并且返回true后，通知对方玩家的游戏存在不规范行为，提前终止游戏
        Battleship.events.informPlayerCheating({filter: {defendant: this.myAddress}}, function (err, event){
            if (err) {
                return console.log(err);
            }
            alert("您在游戏过程中存在不规范行为，游戏提前结束！");
        });
    }

    // 存储玩家的赌注、hash time lock并生成对应的赌注contract
    async store_bid(playerAddress, opponentAddress, ante, stringLock, timeLock){
        let ante_wei = ante * 10 ** 18;
        Battleship.methods.storeBit(playerAddress, opponentAddress, ante_wei.toString(), stringLock, timeLock).send({
            from: playerAddress,
            value: ante_wei,
            gas: 3141592
        });
    }

    // create the commitment for the giving board, which is returned and sent to the opponent's player
    async initialize_board(initial_board) {
        this.my_board = initial_board;

        // Store the positions of your two ships locally, so you can prove it if you win
        this.my_ships = [];
        for (let i = 0; i < BOARD_LEN; i++){
            for (let j=0; j < BOARD_LEN; j++){
                if (this.my_board[i][j]) {
                    this.my_ships.push([i,j]);
                }
            }
        }

        // set nonces to build our commitment
        this.nonces = get_nonces();
        // build commitment to our board
        const commit = build_board_commitment(this.my_board, this.nonces); // build_board_commitment defined in util.js
        // sign this commitment
        const sig = sign_msg(commit, this.myAddress);

        // store the board commitment in the contract
        await Battleship.methods.store_board_commitment(commit).send({from : this.myAddress, gas: 3141592})
        return [commit, sig];
    }

    /* receive_initial_board_commit
  \brief
    called with the returned commitment from initialize_board() as argument
  \params:
    commitment - a commitment to an initial board state received from opponent
    signature - opponeng signature on commitment
    */
    receive_initial_board_commit(commitment, signature) {
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //    DONE this function has been completed for you.
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if (!check_signature(commitment, signature, this.opponentAddress)) {
            throw "received an invalid signature from opponent as initial board commit";
        }
        this.opponent_commit = commitment;
        this.opponent_commit_sig = signature;
    }

    /* build_guess
    \brief:
      build a guess to be sent to the opponent
    \params
      i - int - the row of the guessed board square
      j - int - the column of the guessed board square
    \return:
      signature - Promise - a signature on [i, j]
  */
    build_guess(i, j) {
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //    DONE this function has been completed for you.
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // Sends signed guess to opponent off-chain
        return sign_msg(JSON.stringify([i, j]), this.myAddress); // sign_msg defined in util.js
    }

    /* respond_to_guess
  \brief:
    called when the opponent guesses a board squaure (i, j)
  \params:
    i - int - the row of the guessed board square
    j - int - the column of the guessed board square
    signature - signature that proves the opponent is guessing (i, j)
  \return:
    hit (opening)   - bool   	- did the guess hit one of your ships?
    nonce 					- bytes32 - nonce for square [i, j]
    proof 					- object 	- proof that the guess hit or missed a ship
    */
    respond_to_guess(i, j, signature) {
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //    DONE this function has been completed for you.
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // Check first that the guess is signed, if not, we don't respond
        if (!check_signature(JSON.stringify([i, j]), signature, this.opponentAddress)) { //check_signature defined in util.js
            // 如果检查对方玩家的签名是不符合要求的话，则调用accuse cheating function
            throw "received an invalid signature from opponent as initial board commit";
        }
        // get truth value for this cell along with the associated nonce
        const opening = this.my_board[i][j], nonce = this.nonces[i][j];
        // write proof for this opening
        const proof = get_proof_for_board_guess(this.my_board, this.nonces, [i, j]);
        // return to opponent
        return [opening, nonce, proof];
    }

    /* receive_response_to_guess
    \brief:
      called with the response from respond_to_guess()
    \params:
      response - [hit, proof] - the object returned from respond_to_guess()
  */
    receive_response_to_guess(i, j, response) {
        // unpack response
        let [opening, nonce, proof] = response;

        // verify that opponent responded to the query
        if (!verify_opening(opening, nonce, proof, this.opponent_commit, [i, j])) {
            throw "opponent's response is not an opening of the square I asked for";
        }
        // ##############################################################################
        // TODO store local state as needed to prove your winning moves in claim_win
        // Hint: What arguments do you need to pass to the contract to check a ship?
        // ##############################################################################
        // Your code here
        if(this.opponent_board[i][j] != 0){
            console.log("This position in the opponent's board has been hit before");
            return;
        }

        this.last_received_response = response;
        this.last_guest_leaf_index = i * BOARD_LEN + j;

        //check if I can check a ship
        // 如果在js端判断了对手确实点击了ship，则调用evm来进一步判断是否没有出错（有没有其他人在这个过程中作假）
        if(opening == true){//hit the ship
            // console.log("opening: " + opening);
            //call the contract to check a ship
            var opening_nonce = web3.utils.fromAscii(JSON.stringify(opening) + JSON.stringify(nonce));
            let index_of_guess_in_leaves = i * BOARD_LEN + j;

            // 设置一个event来获取它的return boolean值
            let check_ship_promise = Battleship.methods.check_one_ship(
                opening_nonce,
                proof,
                index_of_guess_in_leaves,
                this.opponentAddress).send({from: this.myAddress, gas: 3141592});

            // var check_ship_promise = new Promise(function(){});

            // evm can not return the result to web3.js
            check_ship_promise.then(()=>{
                this.opponent_board[i][j] = 2;
                console.log("opponent_board[i][j]: " + this.opponent_board[i][j]);
            });

        }else{//miss the target ship
            this.opponent_board[i][j] = 1;
        }
    }

    async accuse_timeout() {
        // ##############################################################################
        //    TODO implement accusing the opponent of a timeout
        //	  - Called when you press "Accuse Timeout"
        // ##############################################################################
        // Your code here
        console.log("this.contractID: " + contractID);
        Battleship.methods.claim_opponent_left(this.opponentAddress, contractID)
            .send({from: this.myAddress, gas: 3141592});
    }

    async handle_timeout_accusation(){
        Battleship.methods.handle_timeout(this.myAddress).send({
            from: this.myAddress,
            gas: 3141592
        }).then((result) => {return;});
    }

    async claim_timeout_winnings(){
        //		- Called when you press "Claim Timeout Winnings"
        // 		- Returns true if game is over
        // ##############################################################################
        // Your code here
        Battleship.methods.claim_timeout_winnings(this.opponentAddress, contractID, this.opponentContractID, this.opponentStringLock, this.stringLock)
            .send({from: this.myAddress, gas: 3141592});
    }

    /*
    accuse that the player is cheating
     */
    async accuse_cheating(){
        let [opening, nonce, proof] = this.last_received_response;
        let opening_nonce = web3.utils.fromAscii(JSON.stringify(opening) + JSON.stringify(nonce));
        let index_of_guess_in_leaves = this.last_guest_leaf_index;


        Battleship.methods.accuse_cheating(
            opening_nonce,
            proof,
            index_of_guess_in_leaves,
            this.opponentAddress,
            contractID,
            this.opponentContractID,
            this.opponentStringLock,
            this.stringLock).send({from: this.myAddress, gas: 3141592});
    }


    async forfeit_cheating_timeout_claimWin(){
        Battleship.methods.forfeit_cheating_timeout_claimWin(contractID, this.opponentContractID, this.opponentStringLock, this.stringLock).send({from: this.myAddress, gas: 3141592});
    }

    async claim_win(){
        Battleship.methods.claim_game_win(contractID, this.opponentContractID, this.opponentStringLock, this.stringLock).send({from: this.myAddress, gas: 3141592})
    }

    async forfeitGame(){
        console.log("this.opponentContractID: " + this.opponentContractID);
        console.log("this.contractID: " + contractID);

        Battleship.methods.forfeit(this.opponentAddress).send({from: this.myAddress, gas: 3141592});
    }
}



// $('#accuseTimeout').click( () =>{
//     let pAddress = $("#userIdInput").val();
//     Battleship.methods.claim_opponent_left('0x5B38Da6a701c568545dCfcB03FcB875f56beddC4', contractID).send({
//         from: pAddress,
//         gas: 3141592
//     }).then((result) => {return;});
// });


// $('#store').click(() => {
//     let pAddress = $("#userIdInput").val();
//     // let ante = $("#bitSetup").val();
//     let ante = 0.1;
//     let ante_wei = ante*10**18;
//     let stringLock = "hello world";
//     let timeLock =  new Date().valueOf() + 10000000;
//
//     Battleship.methods.storeBit('0x1788692A6E0d49de1e55074198d1011F8cd179B6', '0x5B38Da6a701c568545dCfcB03FcB875f56beddC4', ante_wei.toString(), stringLock, timeLock).send({
//         from: pAddress,
//         value: ante_wei,
//         gas: 3141592
//     }).then((result) => {return;});
// });
//
// $('#withdraw').click(() => {
//     let stringLock = "hello world";
//     let pAddress = $("#userIdInput").val();
//     Battleship.methods.withdraw("0x220ed8af68e5c4b3174060d506531e51489514deef802163401395164ff4ae07", stringLock).send({
//         from: pAddress,
//         gas: 3141592
//     }).then((result) => {return;});
// });



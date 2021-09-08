let socket = null;
let userId = null;
let pages = 0;
let count = 0;
let totalPages = 0;
let limit = 1;

// 定义对手的Ethereum地址以及eth赌注
let opponentPlayer = null;
let opponentBit = null;
// 定义自己的Ethereum地址以及eth赌注
let playerAddress = null;
let bit = null;
//强制关闭浏览器  调用websocket.close（）,进行正常关闭
window.onunload = function() {
	disconnect();
	web3.close();
}

// 判断点击事件来进行确认逻辑
$(document).on('click', function (event){
	let id = event.target.getAttribute("id");
	if(id == "match"){
		if (!confirm("请确认输入的赌注，是否进行匹配游戏用户？")){
			alert("点击了取消");
			return;
		}
	}
});

function connect(){
	userId = $("#userIdInput").val();
	// get().then((res) => {userId = res});
	// console.log(userId);
	var socketUrl="ws://127.0.0.1:5003/game/match/" + userId;
	socket = new WebSocket(socketUrl);
	//打开事件
	socket.onopen = function() {
		console.log("websocket 已打开 userId: " + userId);
	};
	//获得消息事件
	socket.onmessage = function(msg) {
		var serverMsg = "收到服务端信息: " + msg.data;
		const obj = JSON.parse(msg.data);
		// console.log(obj.code);
		// 判断接收的code是什么类型的，再执行对应的逻辑
		// 若接收到的是2005，则是需要本地客户端断开与服务器的连接
		// 若接收到的是2006，则是需要计算分页数据
		if (obj.code == "2006") {
			disconnect();
		} else if (obj.code == "2007") {
			count = obj.chatMessage.data.totalPageNums;
			const playerList = obj.chatMessage.data.playerInfoList;
			dynamicAddUser(count, playerList);
			goPage(1,10);
			// totalPages = Math.ceil(count/limit) ;
			// console.log(totalPages);
		} else if (obj.code == "2005") {
			alert("目前匹配大厅中无玩家！！");
		} else if (obj.code == "2009") {
			let flag = false;
			let opponentPlayer = obj.chatMessage.data.opponentAddress;
			let opponentBit = obj.chatMessage.data.opponentBit;
			if(confirm("请确认是否同意与玩家：" + opponentPlayer + "进行匹配？它的赌注为：" + opponentBit + "eth")){flag = true;}
			confirmMatch(flag, opponentPlayer);
		} else if (obj.code == "2011"){
			let opponentPlayer = obj.chatMessage.data;
			alert("对手玩家: " + opponentPlayer + "拒绝你的游戏邀请");
		}
		else if (obj.code == "2012") {
			playerAddress = userId;
			opponentPlayer = obj.chatMessage.data.opponentAddress;
			opponentBit = obj.chatMessage.data.opponentBit;
			alert("你已经与对手玩家 " + opponentPlayer + " 成功匹配");
			document.getElementById("divTable").style.display="none";
			document.getElementById("opponentPlayerInfo").style.display="";
			// 开始进入游戏阶段
			$('#opponentPlayerAddress').html("对手Ethereum地址： " + opponentPlayer);
			$('#opponentPlayerBit').html("对手设置的赌注为：" + opponentBit + " eth");
			// 调用开始游戏函数(在这个调用函数的时候，生成自己的stringLock)
			setup_game(playerAddress,opponentPlayer);

		} else if (obj.code == "2002") {
			alert(obj.chatMessage.data);
		} else if (obj.code == "2001") {
			disconnect();
			document.getElementById("battlePrepare").style.display="none";
			alert("你的账号已经在其他地方被登陆，请重新登陆！");
		} else if (obj.code == "2013"){
			disconnect();
			alert("该玩家已经建立了连接，您无法继续进行连接操作！");
		} else if (obj.code == "2000") {
			alert("连接成功！请继续操作");
			document.getElementById("battlePrepare").style.display="";
		} else if (obj.code == "2014"){
			alert("对手玩家及时做出反应，您的timeout accuse无效！");
		} else if (obj.code == "2015"){
			let commitment = obj.chatMessage.data.boardCommitment;
			let signature = obj.chatMessage.data.opponentBoardSignature;
			let stringLock = obj.chatMessage.data.opponentStringLock;
			let contractID = obj.chatMessage.data.opponentContractID;
			// 检查对方玩家发送过来的签名信息，若不通过，则认为是对方玩家存在不规范游戏行为，直接调用evm上的claim win的函数
			if (!check_signature(commitment, signature, opponentPlayer)) {
				// 通知对方玩家board guess signature无效
				notifyBoardGuessSignatureInvalid(opponentPlayer, signature);
				alert("received an invalid signature from opponent as initial board commit!");
				throw "received an invalid signature from opponent as initial board commit";
			}
			player.opponent_commit = commitment;
			player.opponent_commit_sig = signature;
			player.opponentStringLock = stringLock;
			player.opponentContractID = contractID;
			// 开始进入游戏开始界面
		} else if (obj.code == "2016"){
			let firstPlayerTurn = obj.chatMessage.data;
			enter_playing_game_ui(player, guess_square, firstPlayerTurn).then(r => console.log("game is start"));
		} else if (obj.code == "2017"){
			let boardGuess = obj.chatMessage.data.boardGuess;
			let boardI = obj.chatMessage.data.board_i;
			let boardJ = obj.chatMessage.data.board_j;
			let [opening, nonce, proof] = respond_guess(player, parseInt(boardI), parseInt(boardJ), boardGuess);
			playerRespondToBoardGuess(opponentPlayer, opening, proof, nonce);
		} else if (obj.code == "2018"){
			let opening = obj.chatMessage.data.opponentBoardOpening === "false" ? false : true;
			let p = obj.chatMessage.data.opponentBoardProof;
			let nonce = parseInt(obj.chatMessage.data.opponentBoardNonce);
			let proof = p.replace("[", "").replace("]", "").replace(/\"/g, "").split(",");
			receive_response_guess(parseInt(board_i), parseInt(board_j), player, opening, nonce, proof, (hit) => {
				// update squares with splash or explosion
				// inform the player turns to both two players, and make server judge that if the player is winner
				playerBoardTurn(opponentPlayer, opponentPlayer, hit);
				$('#player > .their-board #' + board_i + '-' + board_j).css('background-image', 'url(' + (hit ? EXPLOSION_IMG : SPLASH_IMG) + ')');
			}).then(r => console.log("receive successful"));
		} else if (obj.code == "2019"){
			// 更新playerTurn, 此时游戏尚未结束
			playerTurn = obj.chatMessage.data;
			// 更新玩家用户界面
			update_playing_game_ui()
		}  else if (obj.code == "2021"){
			// 正常游戏流程下的宣布玩家胜利
			let winnerAddress = obj.chatMessage.data;
			if (winnerAddress == playerAddress){
				claim_win().then(r => console.log("you are winner!"));
			}
			end_game_ui(winnerAddress);

		} else if (obj.code == "2022"){
			//由于forfeit game或者accuse timeout等事件发生，游戏提前结束，并通知双方玩家游戏赢家
			let winnerAddress = obj.chatMessage.data;
			end_game_ui(winnerAddress);
		} else if (obj.code == "2023"){
			// 通知玩家的board guess signature是无效的
			let invalidBoardGuessSignature = obj.chatMessage.data;
			alert("对方玩家通知您猜测的棋盘格子签名是无效的，请重新进行操作！");
		} else if (obj.code == "2024"){
			// 通知玩家accuse timeout成功有效
			let opponentAddress = obj.chatMessage.data;
			if (opponentAddress == playerAddress){
				accuse_timeout_win().then(r => console.log("accuse timeout success"));
			}
			end_game_ui(opponentAddress);
		}


		console.log(serverMsg);
	};
	//关闭事件
	socket.onclose = function() {
		console.log("websocket 已关闭 userId: " + userId);
	};
	//发生了错误事件
	socket.onerror = function() {
		console.log("websocket 发生了错误 userId : " + userId);
	}
}

function disconnect(){
	socket.close();
}

// 用户加入
function addUser(){
	if (checkWebSocketStatus()) {
		return;
	}
	var chatMessage = {};
	var sender = userId;
	var type = "ADD_USER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "开始加入......");
	socket.send(JSON.stringify(chatMessage));
}

//查询当前处于匹配的总人数
function queryAllInMatchUser(){

	if (checkWebSocketStatus()) {
		return;
	}
	// 确保输入的赌注不能为空并且要 >= 0
	if (isNaN(parseInt($('#bitSetup').val())) || parseInt($('#bitSetup').val()) <= 0){
		alert("Bet must be some positive integer!");
		return;
	}
	var chatMessage = {};
	var sender = userId;
	var type = "ENTER_MATCH";
	bit = $("#bitSetup").val();
	chatMessage.sender = sender;
	chatMessage.type = type;
	chatMessage.bit = bit;
	console.log("用户:" + sender + "开始查询匹配用户......");

	socket.send(JSON.stringify(chatMessage));
}



// 与指定用户进行匹配
function matchUser(val){
	if (checkWebSocketStatus()) {
		return;
	}
	let opponent = $(val).closest('tr').find('td').eq(1).text();
	// let opponentBit = $(val).closest('tr').find('td').eq(2).text();

	let chatMessage = {};
	let sender = userId;
	let type = "MATCH_USER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	// chatMessage.opponentBit = opponentBit;
	chatMessage.opponentID = opponent;
	console.log("用户:" + sender + "开始匹配......");
	socket.send(JSON.stringify(chatMessage));
	alert("已经发送请求给对方玩家，请稍等！");
}

// 取消匹配
function cancelMatch(){
	if (!confirm("请确认是否退出匹配大厅？")){
		alert("点击了取消");
		return;
	}
	if (checkWebSocketStatus()) {
		return;
	}
	document.getElementById("divTable").style.display="none";
	// $("#table_result").empty();
	// $("#pageUtil").empty();
	let chatMessage = {};
	let sender = userId;
	let type = "CANCEL_MATCH";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "取消匹配......");
	socket.send(JSON.stringify(chatMessage));
}

//确认当前用户是否同意匹配发送邀请的玩家
function confirmMatch(flag, opponentPlayerAddress){
	if (checkWebSocketStatus()) {
		return;
	}
	console.log(flag);
	if (flag) {
		let chatMessage = {};
		let sender = userId;
		let bit = $("#bitSetup").val();
		let type = "ACCEPT_MATCH";
		chatMessage.sender = userId;
		chatMessage.bit = bit;
		chatMessage.type = type;
		chatMessage.opponentPlayerAddress = opponentPlayerAddress;
		console.log("用户:" + sender + "确认匹配......");
		socket.send(JSON.stringify(chatMessage));
	} else {
		let chatMessage = {};
		let sender = userId;
		let type = "REJECT_MATCH";
		chatMessage.sender = userId;
		chatMessage.type = type;
		chatMessage.opponentPlayerAddress = opponentPlayerAddress;
		console.log("用户:" + sender + "拒绝匹配......");
		socket.send(JSON.stringify(chatMessage));
	}
}

// 处理timeout accuse（通知双方玩家关于accuse timeout的结果）
function accuseTimeoutResult(handleTimeoutAccuseResult){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "HANDLE_TIMEOUT_ACCUSE_RESULT";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.handleTimeoutAccuseResult = handleTimeoutAccuseResult;
	console.log("用户" + sender + "反应timeout accuse是无效的");
	socket.send(JSON.stringify(chatMessage));
}

// 通知对方玩家board guess signature无效
function notifyBoardGuessSignatureInvalid(opponentPlayerAddress, invalidSignature) {
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "NOTIFY_BOARD_GUESS_SIGNATURE_INVALID";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	chatMessage.invalidSignature = invalidSignature;
	console.log("用户" + sender + "反应timeout accuse是无效的");
	socket.send(JSON.stringify(chatMessage));
}

// 通知对方玩家被指控time out的玩家响应超时
function notifyAccuseTimeoutSuccess(opponentPlayerAddress){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "NOTIFY_ACCUSED_PLAYER_TIMEOUT_SUCCESS";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	console.log("用户" + sender + "反应timeout accuse是成功的");
	socket.send(JSON.stringify(chatMessage));
}

// 借助webSocket来传输board commitment以及对应的signature
function storeBoardInfo(commitment, signature, opponentPlayerAddress, contractID, stringLock){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "SETUP_BOARD_INFO";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	chatMessage.boardCommitment = commitment;
	chatMessage.boardSignature = signature;
	chatMessage.stringLock = stringLock;
	chatMessage.contractID = contractID;
	console.log("用户" + sender + "向对手玩家传输board info");
	socket.send(JSON.stringify(chatMessage));
}

// 借助webSocket来传输board guess
function playerGuessBoard(signed_guess, opponentPlayerAddress, i, j){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "BOARD_GUESS";
	chatMessage.sender = userId;
	chatMessage.type = type;
	// 将对应的board guess传给对手玩家进行判断
	chatMessage.i = i;
	chatMessage.j = j;
	chatMessage.boardGuess = signed_guess;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	console.log("用户" + sender + "向对手玩家传输board guess");
	socket.send(JSON.stringify(chatMessage));
}

// 借助webSocket来传输玩家发给对手玩家自己对于对手玩家的board guess的opening，proof，nonce，由对手玩家进行验证是否有效
function playerRespondToBoardGuess(opponentPlayerAddress, opening, proof, nonce){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "RESPOND_TO_BOARD_GUESS";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	chatMessage.opening = opening;
	chatMessage.proof = proof;
	chatMessage.nonce = nonce;
	console.log("用户" + sender + "向对手玩家传输对于board guess的respond");
	socket.send(JSON.stringify(chatMessage));
}

// 借助webSocket传输当前玩家轮数
function playerBoardTurn(turn, opponentPlayerAddress, boardGuessResult){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "BROADCAST_PLAYER_TURN";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	chatMessage.turn = turn;
	chatMessage.boardGuessResult = boardGuessResult;
	console.log("用户" + sender + "向双方玩家广播player's turn");
	socket.send(JSON.stringify(chatMessage));
}


// 借助webSocket通知双方玩家游戏提前结束
function broadcastGameWinner(winnerAddress, opponentPlayerAddress){
	if (checkWebSocketStatus()) {
		return;
	}
	let chatMessage = {};
	let sender = userId;
	let type = "BROADCAST_GAME_WINNER";
	chatMessage.sender = userId;
	chatMessage.type = type;
	chatMessage.winnerAddress = winnerAddress;
	chatMessage.opponentPlayerAddress = opponentPlayerAddress;
	console.log("用户" + sender + "向双方玩家广播游戏的赢家");
	socket.send(JSON.stringify(chatMessage));
}


// 游戏结束
function gameOver(){
	if (checkWebSocketStatus()) {
		return;
	}
	var chatMessage = {};
	var sender = userId;
	var type = "GAME_OVER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	// chatMessage.opponentPlayerAddress = opponentPlayer;
	console.log("用户:" + sender + "结束游戏");
	socket.send(JSON.stringify(chatMessage));
}

// 判断当前连接的WebSocket是否有效，即是否存在有效实例
function checkWebSocketStatus(){
	if (socket !=null) {
		if (socket.readyState != socket.OPEN) {
			alert("你已经断线，请重新连接服务器！");
			return true;
		}
	} else {
		alert("你还未连接服务器！");
		return true;
	}
	return false;
}



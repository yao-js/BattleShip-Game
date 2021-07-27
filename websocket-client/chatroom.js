let socket = null;
let userId = null;
let bit = null;
let pages = 0;
let count = 0;
let totalPages = 0;
let limit = 1;

// 定义对手的Ethereum地址以及eth赌注
let opponentPlayer = null;
let opponentBit = null;
//强制关闭浏览器  调用websocket.close（）,进行正常关闭
window.onunload = function() {
	disconnect()
	web3.close()
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
			alert("目前匹配大厅中无玩家！！")
		} else if (obj.code == "2009") {
			let flag = false;
			let opponentPlayer = obj.chatMessage.data.opponentAddress;
			let opponentBit = obj.chatMessage.data.opponentBit;
			if(confirm("请确认是否同意与玩家：" + opponentPlayer + "进行匹配？它的赌注为：" + opponentBit + "eth")){
				flag = true;
				confirmMatch(flag, opponentPlayer);
			} else{
				confirmMatch(flag, opponentPlayer);
			}
		} else if (obj.code == "2011"){
			let opponentPlayer = obj.chatMessage.data;
			alert("对手玩家: " + opponentPlayer + "拒绝你的游戏邀请");
		}
		else if (obj.code == "2012") {
			opponentPlayer = obj.chatMessage.data.opponentAddress;
			opponentBit = obj.chatMessage.data.opponentBit;
			alert("你已经与对手玩家 " + opponentPlayer + " 成功匹配");
			document.getElementById("divTable").style.display="none";
			document.getElementById("opponentPlayerInfo").style.display="";
			$('#opponentPlayerAddress').html("对手Ethereum地址： " + opponentPlayer);
			$('#opponentPlayerBit').html("对手设置的赌注为：" + opponentBit + " eth");
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

// 游戏中
function userInPlay(){
	if (checkWebSocketStatus()) {
		return;
	}
	var chatMessage = {};
	var sender = userId;
	var data = $("#newScoreInput").val();
	var type = "PLAY_GAME";
	chatMessage.sender = sender;
	chatMessage.data = data;
	chatMessage.type = type;
	console.log("用户:" + sender + "更新分数为" + data);
	socket.send(JSON.stringify(chatMessage));
}

// 游戏结束
function gameover(){
	if (checkWebSocketStatus()) {
		return;
	}
	var chatMessage = {};
	var sender = userId;
	var type = "GAME_OVER";
	chatMessage.sender = sender;
	chatMessage.type = type;
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



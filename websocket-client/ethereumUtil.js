// import Web3 from "web3";
// conversion functions
const eth_converstion_rate = 171.45;
const wei_to_dollars = wei => wei_to_eth(wei) * eth_converstion_rate;
const wei_to_eth = wei => wei / Math.pow(10, 18);

// default variable
var playerAddress = null

// define the default ethereum's rinkeby network node
let web3 = new Web3(Web3.givenProvider || "wss://rinkeby.infura.io/ws/v3/6c355a1bd0d7487e868ae499d320b4d9");

function queryBalance(){
    playerAddress = $("#userIdInput").val();
    console.log("需要查询的账户地址为：" + playerAddress);
    if (!web3.utils.isAddress(playerAddress)) {
        alert("该账户并不是有效的ETH账户或者不存在！！");
        return;
    } else {
        web3.eth.getBalance(playerAddress).then((res) => {
            alert("该用户账户的余额为：" + wei_to_eth(parseFloat(res)) + " eth");
            $('#player-account-balance').html(wei_to_eth(parseFloat(res)) + ' eth');
        });
    }
}


// 通过metamask来获取web3对象，进而获取对应的用户信息，如eth地址，eth余额等
async function get(){
    if (typeof window.ethereum !== 'undefined') {
        console.log('MetaMask is installed!');
    }
    // 实例化web3
    window.web3 = new Web3(ethereum);
    let web3 = window.web3;
    // 请求用户授权 解决web3js无法直接唤起Meta Mask获取用户身份
    const enable = await ethereum.enable();
    console.log(enable,11)
    // 授权获取账户
    let accounts = await web3.eth.getAccounts();
    // web3.eth.getAccounts().then((e)=>{console.log(e)})
    // 取第一个账户
    let myAccount = accounts[0];
    console.log(myAccount, 1);
    // 返回指定地址账户的余额
    let balance = await web3.eth.getBalance(myAccount);
    console.log(balance, 2)
    alert("该用户账户的余额为：" + wei_to_eth(parseFloat(balance)) + " eth");
    $('#player-account-balance').html(wei_to_eth(parseFloat(balance)) + ' eth');
    return myAccount;
}




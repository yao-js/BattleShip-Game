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
        "name": "cliamWin",
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
                "name": "_contractId",
                "type": "bytes32"
            },
            {
                "internalType": "string",
                "name": "_preimage",
                "type": "string"
            }
        ],
        "name": "withdraw",
        "outputs": [
            {
                "internalType": "bool",
                "name": "",
                "type": "bool"
            }
        ],
        "stateMutability": "nonpayable",
        "type": "function"
    }
]

abiDecoder.addABI(abi);

let contractAddress = "0x3ef85716e386d1a8a86d173387c61ac5c3651cf9";

let Battleship = new web3.eth.Contract(abi, contractAddress);

let contractID = null;
window.onload = function() {
    Battleship.events.addContract(function (err, event){
        if (err){
            return error(err);
        }
        contractID = event.returnValues._contractId;
        console.log(event.returnValues._contractId);
    });
}




$('#store').click(() => {
    let pAddress = $("#userIdInput").val();
    let ante = $("#bitSetup").val();
    let ante_wei = ante*10**18;
    let stringLock = "hello world";
    let timeLock =  new Date().valueOf() + 1000000;

    Battleship.methods.storeBit('0x1788692A6E0d49de1e55074198d1011F8cd179B6', ante_wei.toString(), stringLock, timeLock).send({
        from: pAddress,
        value: ante_wei,
        gas: 3141592
    }).then((result) => {return;});
});

$('#withdraw').click(() => {
    let stringLock = "hello world";
    let pAddress = $("#userIdInput").val();
    Battleship.methods.withdraw("0x220ed8af68e5c4b3174060d506531e51489514deef802163401395164ff4ae07", stringLock).send({
        from: pAddress,
        gas: 3141592
    }).then((result) => {return;});
});

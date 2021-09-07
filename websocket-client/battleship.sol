// SPDX-License-Identifier: GPL-3.0

pragma solidity >=0.4.22 <0.7.0;

/**
 * @title Owner
 * @dev Set & change owner
 */


contract HashedTimelock {

    // the default length of defined board
    uint32 constant BOARD_LEN = 4;

    struct LockContract{
        address payable player;
        address payable winner;
        address payable opponent;
        uint8 state;//game state transfer: inital:0 -> start:1 -> result claim:2
        uint256 amount; // store the bit amount of this battleship
        bytes32 hashlock; // use sha256 hash function
        uint timelock; // UNIX timestamp seconds - locked UNTIL this time
        bool withdrawn;
        bool refunded;
        bytes32 preimage; // it's secret, sha256(_preimage) should equal to hashlock
    }

    // 一个玩家对应一个游戏对局的info
    struct PlayInfo{
        address payable player;
        bytes32 bitContractID;
        bytes32 merkle_root;
        uint8 state;//game state transfer: inital:0 -> start:1 -> result claim:2
        //store the leaf index of player where there is a ship hit by the opponent's player
        uint256[] leaf_index_check_player;
    }

    struct TimeoutAccuse{
        uint timeout; // this variable stores the time of accuse opponent player timeout
        //        uint time_limit; // this variable is a constant which set to 1 minutes
        address timeout_claimed_party; // this variable stores timeout claimed opponent player
        address player;
        bool flag;
    }

    // constant variable
    uint private time_limit = 6 seconds;

    // //define the bit of one battleship
    // uint256 private bit;
    // address private p;
    // uint8 private state = 0;//game state transfer: inital:0 -> start:1 -> result claim:2


    modifier fundsSent(uint256 bit) {
        require(address(msg.sender).balance > bit, "msg.sender balance must be > bit");
        _;
    }
    modifier futureTimelock(uint _time) {
        // 唯一的要求就是 timelock 时间锁指定的时间要在最后一个区块产生的时间（now）之后
        require(_time > now, "timelock time must be in the future");
        _;
    }

    mapping (bytes32 => LockContract) contracts;
    // key is opponent player's eth address or player's address?, and value is timeout struct and msg.sender(player address)
    mapping (address => TimeoutAccuse) timeouts;
    // mapping the play info into one's player's eth address
    mapping (address => PlayInfo) pInfo;

    event addContract(address indexed defendant, bytes32 _contractId, bytes32 _hashlock);

    // denote the opponent player timeout
    event AccuseTimeout(address indexed defendant, address sender);

    // handle Timeout Accuse
    event HandleTimeoutAccuse(address indexed defendant, address sender, bool result);

    // request forfeit game
    event requestForfeitGame(address indexed defendant);

    // fetch the result player guess board ship
    event CheckPlayerShipResult(address indexed defendant, bool result);

    // inform opponent player exists irregular behavior
    event informPlayerCheating(address indexed defendant);

    function storeBit(address payable player, address payable opponent, uint bit, string calldata hash, uint _timelock)
    external payable fundsSent(bit) futureTimelock(_timelock) returns (bytes32 contractId)
    {
        // ensure the player's playInfo to be existed
        // require(pInfo[msg.sender].player != address(0));
        //确保当前的battleship的状态已经是开始游戏了
        uint8 state = 1;
        // 集体加密
        contractId = sha256(abi.encodePacked(player, bit, hash, _timelock));
        // 若具有相同参数的合约已经存在，这次新建合约的请求就会被拒绝。
        // sender 只有更改其中的一个参数，以创建一个不同的合约。
        if (haveContract(contractId))
            revert();
        // convert hash to hashLock to be stored in smart contract
        bytes32 hashLock = keccak256(abi.encodePacked(hash));
        contracts[contractId] = LockContract(player, address(0), opponent, state, bit, hashLock, _timelock, false, false, 0);
        // record the bitContractID into player's playInfo
        pInfo[msg.sender].bitContractID = contractId;
        // cId = bytes32ToString(contractId);
        emit addContract(msg.sender, contractId, hashLock);
        return contractId;
    }

    function store_board_commitment(bytes32 merkle_root) public {
        // ensure that each player's play info is null
        // require(pInfo[msg.sender].player == address(0));
        uint8 state = 1;
        uint256[] memory leaves = new uint256[](16);
        pInfo[msg.sender] = PlayInfo(msg.sender, 0, merkle_root, state, leaves);
        // pInfo[msg.sender].player = msg.sender;
        // Is it necessary?
        // pInfo[msg.sender].bitContractID = contractID;
        // pInfo[msg.sender].merkle_root = merkle_root;
    }

    function check_one_ship(bytes memory opening_nonce, bytes32[] memory proof,
        uint256 guess_leaf_index, address owner) public returns (bool result){
        assert(pInfo[msg.sender].player == msg.sender && pInfo[owner].player == owner);
        // msg.sender's address == owner's address
        //check the openning is "true";
        //web3.utils.fromAscii("true") retunrs "0x74727565"
        require(opening_nonce.length >= 4 && uint8(opening_nonce[0]) == 0x74
        && uint8(opening_nonce[1]) == 0x72
        && uint8(opening_nonce[2]) == 0x75
            && uint8(opening_nonce[3]) == 0x65);

        bytes32 com = pInfo[owner].merkle_root;
        uint256[] storage leaves = pInfo[owner].leaf_index_check_player;

        bool ret = verify_opening(opening_nonce, proof, guess_leaf_index, com);
        bool checkResult = false;
        if(ret == true){
            for(uint32 i = 0; i < leaves.length; i++){
                if(leaves[i] == guess_leaf_index){//if the index already exists in the array
                    checkResult = false;
                }
            }
            leaves.push(guess_leaf_index);
            checkResult = true;
        }

        emit CheckPlayerShipResult(msg.sender, checkResult);
        return checkResult;
    }

    // Verify the proof of a single spot on a single board
    // \args:
    //      opening_nonce - corresponds to web3.utils.fromAscii(JSON.stringify(opening) + JSON.stringify(nonce)));
    //      proof - list of sha256 hashes that correspond to output from get_proof_for_board_guess()
    //      guess - [i, j] - guess that opening corresponds to
    //      commit - merkle root of the board
    function verify_opening(bytes memory opening_nonce, bytes32[] memory proof, uint guess_leaf_index, bytes32 commit) pure public returns (bool result) {
        bytes32 curr_commit = keccak256(opening_nonce); // see if this changes hash
        uint index_in_leaves = guess_leaf_index;

        uint curr_proof_index = 0;
        uint i = 0;

        while (curr_proof_index < proof.length) {
            // index of which group the guess is in for the current level of Merkle tree
            // (equivalent to index of parent in next level of Merkle tree)
            uint group_in_level_of_merkle = index_in_leaves / (2**i);
            // index in Merkle group in (0, 1)
            uint index_in_group = group_in_level_of_merkle % 2;
            // max node index for currrent Merkle level
            uint max_node_index = ((BOARD_LEN * BOARD_LEN + (2**i) - 1) / (2**i)) - 1;
            // index of sibling of curr_commit
            uint sibling = group_in_level_of_merkle - index_in_group + (index_in_group + 1) % 2;
            i++;
            if (sibling > max_node_index) continue;
            if (index_in_group % 2 == 0) {
                curr_commit = keccak256(merge_bytes32(curr_commit, proof[curr_proof_index]));
                curr_proof_index++;
            } else {
                curr_commit = keccak256(merge_bytes32(proof[curr_proof_index], curr_commit));
                curr_proof_index++;
            }
        }
        return (curr_commit == commit);

    }

    // Helper Functions for verify_opening function
    function merge_bytes32(bytes32 a, bytes32 b) pure public returns (bytes memory) {
        bytes memory result = new bytes(64);
        assembly {
            mstore(add(result, 32), a)
            mstore(add(result, 64), b)
        }
        return result;
    }


    // function testing(address payable player, uint8 state, uint256 bit, string calldata _hashlock, uint _timelock)
    //     public view returns (bytes32 contractId){
    //     bytes32 lock = stringToBytes32(_hashlock);
    //     // address payable p = address(_hashlock);
    //     // contractId = this.newContract(player, state, bit, lock, _timelock);
    //     contractId = lock;
    //     return contractId;
    // }

    // function bytes32ToString(bytes32 x) internal view returns(string memory){
    //     bytes memory bytesString = new bytes(32);
    //     uint charCount = 0 ;
    //     for(uint j = 0 ; j<32;j++){
    //         byte char = byte(bytes32(uint(x) *2 **(8*j)));
    //         if(char !=0){
    //             bytesString[charCount] = char;
    //             charCount++;
    //         }
    //     }
    //     bytes memory bytesStringTrimmed = new bytes(charCount);
    //     for(uint j=0;j<charCount;j++){
    //         bytesStringTrimmed[j]=bytesString[j];
    //     }
    //     return string(bytesStringTrimmed);
    // }

    // function stringToBytes32(string memory source) internal view returns(bytes32 result){
    //     assembly{
    //         result := mload(add(source,32))
    //     }
    // }

    //判断当前evm中该合约ID的Player是否以及存在
    function haveContract(bytes32 _contractId)
    internal
    view
    returns (bool exists)
    {
        exists = (contracts[_contractId].player != address(0));
    }

    modifier contractExists(bytes32 _contractId) {
        require(haveContract(_contractId), "contractId does not exist");
        _;
    }
    modifier hashLockMatches(bytes32 _contractId, string memory _x) {
        require(
            contracts[_contractId].hashlock == keccak256(abi.encodePacked(_x)),
            "hashlock hash does not match"
        );
        _;
    }
    modifier withdrawable(bytes32 _contractId) {
        require(contracts[_contractId].winner == msg.sender, "withdrawable: not receiver");
        require(contracts[_contractId].withdrawn == false, "withdrawable: already withdrawn");
        require(contracts[_contractId].timelock > now, "withdrawable: timelock time must be in the future");
        require(contracts[_contractId].state == 1, "withdrawable: the game may over or not start");
        _;
    }

    //将合约里面锁住的钱发给赢家
    function withdraw(bytes32 _contractId, string memory _preimage) internal
    contractExists(_contractId) hashLockMatches(_contractId, _preimage) withdrawable(_contractId) returns (bool) {
        // judge if contracts[_contractId].hashlock == sha256(_preimage)
        LockContract storage c = contracts[_contractId];
        //this step is necessary ?
        // c.preimage = _preimage;
        c.withdrawn = true;
        c.state = 2;
        c.winner.transfer(c.amount);
        return true;
    }

    //judge if the player == msg.sender
    modifier refundable(bytes32 _contractId) {
        require(contracts[_contractId].player == msg.sender, "refundable: not winner");
        require(contracts[_contractId].refunded == false, "refundable: already refunded");
        require(contracts[_contractId].withdrawn == false, "refundable: already withdrawn");
        require(contracts[_contractId].timelock <= now, "refundable: timelock not yet passed");
        _;
    }

    function refund(bytes32 _contractId)
    external contractExists(_contractId) refundable(_contractId) returns (bool)
    {
        LockContract storage c = contracts[_contractId];
        c.refunded = true;
        c.winner.transfer(c.amount);
        return true;
    }


    // address public p1;//the first player
    // address public p2;//the second player
    // address private winner; //the winner of this contract battleship
    // uint8 public state = 0;//game state transfer: inital:0 -> start:1 -> result claim:2

    // uint256 public bit = 0; // the same bit that two player need to make

    //get the balance of contract address
    function getBalance() public view returns (uint256) {
        return address(this).balance;
    }


    function is_game_over(address opponentPlayer) public view returns (bool) {
        return (pInfo[opponentPlayer].state != 1) && (pInfo[msg.sender].state != 1);
    }

    //withdraw the contract balance to players
    // function withdrawAll()external{
    //     msg.sender.transfer(address(this).balance);
    // }

    constructor () public payable{

    }


    // // clear this battleship state, including player1's address and player2's address, bit and state
    // function endGame(address player, bytes32 _contractId) internal contractExists(_contractId) {
    //     require(contracts[_contractId].state == 1);
    //     require(contracts[_contractId].winner == address(0))

    //     bit = 0;
    //     state = 0;
    // }


    // Store the bids of each player
    // Start the game when both bids are received
    // The first player to call the function determines the bid amount.
    // Refund excess bids to the second player if they bid too much.
    // function store_bid(address payable player, string memory hash, uint _timelock) public payable returns (bytes32 contractId){

    //     // contractId = sha256(abi.encodePacked(msg.sender, msg.value, hash, _timelock));
    //     // require(haveContract(contractId), "contractId does not exist");
    //     //if there is enough existed players, set the battleship state to 1 and return
    //     // require(msg.value > bit);
    //     // set the state to 1 denotes that the game is starting
    //     uint8 state = 1;
    //     contractId = this.newContract(player, state, msg.value, hash, _timelock);
    //     return contractId;
    // }

    // Forfeit the game
    // give up the game
    // Regardless of cheating, board state, or any other conditions, this function
    // results in all funds being sent to the opponent and the game being over.
    // we can finish this step through Hyperledger Fabric
    // function forfeit(address payable opponent, bytes32 _contractId, bytes32 _preimage) public {
    //     // TODO
    //     assert(msg.sender == p1 || msg.sender == p2);//The sender must put the bit before.
    //     contracts[_contractId].winner = opponent;
    //     // 投降后将钱转给赢家 opponent
    //     withdraw(_contractId, _preimage);
    //     //
    //     clear_state();
    //     // opponent.transfer(address(this).balance);//transfer all funds to the opponent.
    // }

    function forfeit_cheating_timeout_claimWin(bytes32 winner_contractId, bytes32 opponent_contractId, string memory opponent_preimage, string memory winner_preimage)
    hashLockMatches(opponent_contractId, opponent_preimage) public{
        // ensure the contract is existed
        require(winner_contractId != opponent_contractId, "contract ID can not be the same");
        require(haveContract(winner_contractId) && haveContract(opponent_contractId), "the contract does not exist!");
        //make sure msg.sender == winner's contract player
        require(msg.sender == contracts[winner_contractId].player && msg.sender == contracts[opponent_contractId].opponent, "the forfeit game request player must be the correct contract player!");
        //ensure the state of battleship is starting
        require(contracts[winner_contractId].state == 1 && contracts[opponent_contractId].state == 1, "the game contract state must be 1 !");
        //ensure the game is end
        require(pInfo[msg.sender].state == 2 && pInfo[contracts[opponent_contractId].player].state == 2, "the game is still ongoing");
        contracts[opponent_contractId].winner = msg.sender;
        contracts[winner_contractId].winner = msg.sender;

        //        contracts[opponent_contractId].state = 2;
        //        contracts[winner_contractId].state = 2;
        // withdraw the balance of opponent's contract
        withdraw(opponent_contractId, opponent_preimage);
        // withdraw the balance of winner's contract
        withdraw(winner_contractId, winner_preimage);
        // remove contract of both two players
        delete(contracts[opponent_contractId]);
        delete(contracts[winner_contractId]);
        delete(pInfo[msg.sender]);
        delete(pInfo[contracts[opponent_contractId].player]);

    }

    function claim_game_win(bytes32 winner_contractId, bytes32 opponent_contractId, string memory opponent_preimage, string memory winner_preimage)
    hashLockMatches(opponent_contractId, opponent_preimage) public{
        require(winner_contractId != opponent_contractId, "contract ID can not be the same");
        require(haveContract(winner_contractId) && haveContract(opponent_contractId), "the contract does not exist!");
        require(msg.sender == contracts[winner_contractId].player && msg.sender == contracts[opponent_contractId].opponent, "the forfeit game request player must be the correct contract player!");
        require(contracts[winner_contractId].state == 1 && contracts[opponent_contractId].state == 1, "the game contract state must be 1 !");

        // require player that their ship guess numbers of board must >= 2
        require(pInfo[msg.sender].leaf_index_check_player.length >= 2, "The player can not claim win cause the number of ship guess have not satisfied the requirement yet!");


        contracts[opponent_contractId].winner = msg.sender;
        contracts[winner_contractId].winner = msg.sender;
        withdraw(opponent_contractId, opponent_preimage);
        withdraw(winner_contractId, winner_preimage);
        // remove contract of both two players
        delete(contracts[opponent_contractId]);
        delete(contracts[winner_contractId]);
        delete(pInfo[msg.sender]);
        delete(pInfo[contracts[opponent_contractId].player]);
    }

    function forfeit(address opponentPlayer) public {
        require(pInfo[msg.sender].player == msg.sender && pInfo[opponentPlayer].player == opponentPlayer, "the msg.sender and opponent player must exist");
        // ensure the game is end
        pInfo[msg.sender].state = 2;
        pInfo[opponentPlayer].state = 2;
        emit requestForfeitGame(opponentPlayer);
        // claimWin(winner_contractId, opponent_contractId, opponent_preimage, winner_preimage);
    }

    // request from player who accuse that his/her opponent player is cheated
    function accuse_cheating(bytes memory opening_nonce, bytes32[] memory proof,
        uint256 guess_leaf_index, address owner, bytes32 winner_contractId, bytes32 opponent_contractId, string memory opponent_preimage, string memory winner_preimage) public returns (bool result) {

        assert(msg.sender == pInfo[msg.sender].player && owner == pInfo[owner].player);

        bytes32 com = pInfo[owner].merkle_root;
        if(!verify_opening(opening_nonce, proof, guess_leaf_index, com)){
            // ensure the game is end
            pInfo[msg.sender].state = 2;
            pInfo[owner].state = 2;
            forfeit_cheating_timeout_claimWin(winner_contractId, opponent_contractId, opponent_preimage, winner_preimage);
            emit informPlayerCheating(owner);
            return true;
        }
        return false;

    }

    function claim_opponent_left(address opponentPlayer, bytes32 contractID) public {
        assert((msg.sender == contracts[contractID].player) && (opponentPlayer == contracts[contractID].opponent) && (contracts[contractID].state == 1));

        timeouts[opponentPlayer] = TimeoutAccuse(now, opponentPlayer, msg.sender, true);
//        if(timeouts[opponentPlayer].player == address(0)){
//            timeouts[opponentPlayer] = TimeoutAccuse(now, opponentPlayer, msg.sender, true);
//        } else{
//            timeouts[opponentPlayer].timeout = now;
//            timeouts[opponentPlayer].timeout_claimed_party = opponentPlayer;
//            // timeouts[opponentPlayer].player = msg.sender;
//            // // It denotes that i accuse timeout of the opponent player's operation
//            timeouts[opponentPlayer].flag = true;
//        }
        emit AccuseTimeout(opponentPlayer, msg.sender);
    }

    function handle_timeout(address payable opponentPlayer) public {
        assert((msg.sender == timeouts[opponentPlayer].timeout_claimed_party) && (timeouts[opponentPlayer].flag == true));
        if(now <= timeouts[opponentPlayer].timeout + time_limit){
            timeouts[opponentPlayer].timeout_claimed_party = address(0);
            timeouts[opponentPlayer].timeout = 0;
            timeouts[opponentPlayer].flag = false;
        }

        // if(timeouts[opponentPlayer].flag == false){
        //     emit HandleTimeoutAccuse(timeouts[opponentPlayer].player, msg.sender, false);
        // }
        emit HandleTimeoutAccuse(timeouts[opponentPlayer].player, msg.sender, timeouts[opponentPlayer].flag);

    }

    function claim_timeout_winnings(address opponentPlayer, bytes32 winner_contractId,
        bytes32 opponent_contractId, string memory opponent_preimage, string memory winner_preimage) public {
        assert((msg.sender == timeouts[opponentPlayer].player));
        require(opponentPlayer == timeouts[opponentPlayer].timeout_claimed_party, "should opponent == timeout_claimed_party");

        if(now >= timeouts[opponentPlayer].timeout + time_limit){
            timeouts[opponentPlayer].timeout_claimed_party = address(0);
            // It denotes that the game state is over
            pInfo[opponentPlayer].state = 2;
            pInfo[msg.sender].state = 2;
            forfeit_cheating_timeout_claimWin(winner_contractId, opponent_contractId, opponent_preimage, winner_preimage);
        }
    }

}
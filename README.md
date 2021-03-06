# BattleShip-Game
  
  该项目基于**Ethereum**区块链技术，**Javascript+CSS+HTML**前端技术以及**Java Websocket**后端技术和**Redis**数据库来实现了一个简易版本的在线战舰游戏。它借助**Blockchain Layer2**设计想法来提升EVM的TPS，并同时保证了安全性，同时借助**Merkle Tree**和**Merkle Membership Proof**技术来确保**off-chain channel**的安全性以及其数据的完整性，如下图所示：
  ![image](https://user-images.githubusercontent.com/55738417/122932845-a916bd80-d3a0-11eb-89c0-bef23382d580.png)
  

## 该在线战舰游戏系统主要的创新点如下：
   ### 1.基于Java Websocket以及Redis Cache实现了交互式游戏匹配机制
   >本系统借助 **WebSocket** 网络传输协议的双向数据传输特性，在 **Java SpringBoot** 封装好的 **WebSocket** 框架上结合 **Redis** 搭建起了一套交互式游戏匹配机制。通过 **Redis Cache** 来管理每个玩家用户连接 **WebSocket** 服务器的 **Session**，同时通过 **LFU** 算法管理 **session** 缓存，能更好的解决 **session** 缓存溢出的情况.
   ### 2.基于JavaScript+CSS+HTML、Web3.js以及Ethereum实现在线战舰（online battleship）游戏
   >本系统的前端交互式UI界面由 **JavaScript+jQuery+CSS+HTML** 实现，通过 **Web3.js** 封装好的API访问 **Ethereum** 区块链系统。主要的游戏逻辑均由本地的 **Javascript** 技术以及部署在 **Ethereum** 上的 **Smart Contract** 实现。
   ### 3.基于Hash Time-Locked Contract的Blockchain Layer2设计想法以及Solidity语言来设计Smart Contract
   **`Hash Time-Locked Contract Blockchain Layer2`** 设计想法源于 **`Lighting Network`**. 
   >本系统结合 `HTLC Layer2` 技术设计系统的 `System Model` 如下图所示：
   
   ![未命名文件 (1)](https://user-images.githubusercontent.com/55738417/132353985-abd0d92b-e7f7-4591-ab89-c51fa1f1b758.png)

   ### 4.基于Merkle Root以及Merkle Membership Proof技术来实现Off-Chain Channel
   >在游戏开始之前，双方玩家需要在自己的棋盘上设置 `ship` 并且根据这个设置好的棋盘生成一个`merkle root commitment`。接着通过websocket机制传输给双方本地保存。当游戏开始后，每当玩家点击了棋盘一个格子，会生成对应的 `board signed guess` 并发送给对方玩家，有对方玩家根据自己的 `merkle root commitment` 来判断这个 `board guess` 是否点击到了 `ship` 同时返回结果给该玩家，该结果包括 `merkle proof、hitting result、nonce` 。该玩家通过之前保存在本地的对手玩家的`merkle root commitment`来判断该结果是否有效（借助`merkle membership proof`）。

## 设计系统过程中遇到的问题
  ### 1. Smart Contract的安全性设计
   >在设计Hashed Time Lock Contract时候，由于是由用户直接通过`ETH Address`来发送`Transaction`到EVM上，例如此时有恶意用户直接请求游戏胜利，想要拿走双方赌注，这时候就要考虑`Smart Contract`中 `function`的安全性。
  ### 2. 游戏逻辑与以太坊区块链技术的融合
   >在战舰游戏中，如何通过部署在以太坊上的智能合约来实现对应的逻辑判断，如指控对方玩家超时未操作、指控对方玩家出老千以及放弃游戏认输等操作。后续通过在智能合约上设置多个事件，在JS端实现多个事件监听器来实时的订阅事件的触发以及返回结果。


## Install
This project uses **Java、JavaScript、Redis** 等。若需要下载请到对应官网进行下载配置。

## Usage

- 配置`ganache-cli`
- 在本地执行`ganache-cli`开启本地离线EVM节点。
- 通过 [Remix IDE](https://remix.ethereum.org/) 部署`battleship.sol`到本地的离线EVM节点。
- 更换 `BattleshipPlayer.js` 中的 `contractAddress` 为之前部署到本地离线EVM节点的 `contract address`。
- 启动 `SpringBoot APP`。
- 登陆 `index.html`进行 **Battleship Game**。

## Related Efforts
- [Battleship Game Demo](https://liangyihuai.blog.csdn.net/article/details/116459829) \- 本系统是基于Battleship Game开源项目继续完善的。
- [Match-Project](https://github.com/Yee-Q/match-project) \- 本系统是参考了该github博主的开源项目所继续实现的游戏对战平台。
- [Hashed Time Lock Contract](https://zhuanlan.zhihu.com/p/112228102) \- 本系统的`HTLC Blockchain Layer2`设计初衷是参考了该文章而来的。


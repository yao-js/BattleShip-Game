# BattleShip-Game
  
  该版本的BattleShip（战舰）游戏是基于CSDN链接 https://liangyihuai.blog.csdn.net/article/details/116459829 而来的开源项目，该项目是基于Ethereum区块链技术，Javascript+CSS+HTML前端技术以及Java Websocket后端技术和Redis数据库来实现了一个简易版本的在线战舰游戏。它借助Blockchain Layer2设计想法来提升EVM的TPS，并同时保证了安全性，同时借助Merkle Root和Merkle Proof技术来确保off-chain channel的安全性以及其数据的完整性，如下图所示：
  ![image](https://user-images.githubusercontent.com/55738417/122932845-a916bd80-d3a0-11eb-89c0-bef23382d580.png)
  
  该在线战舰游戏系统主要的创新点如下：
  
  1. 基于Java Websocket以及Redis Cache实现了交互式游戏匹配机制。
  2. 基于JavaScript+CSS+HTML、Web3.js以及Ethereum实现在线战舰（online battleship）游戏。
  3. 基于Hash Time-Locked Contract的Blockchain Layer2设计想法以及Solidity语言来设计Smart Contract。
  4. 基于Merkle Root以及Merkle Membership Proof技术来实现Off-Chain Channel

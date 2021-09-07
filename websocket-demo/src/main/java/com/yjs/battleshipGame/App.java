package com.yjs.battleshipGame;

import com.yjs.battleshipGame.websocket.ChatWebsocket;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yeeq
 */
@SpringBootApplication
@MapperScan("com.yjs.battleshipGame.*")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        // 同时开启检测线程
        new Thread(new ChatWebsocket.TimeoutTimerThread()).start();
    }
}

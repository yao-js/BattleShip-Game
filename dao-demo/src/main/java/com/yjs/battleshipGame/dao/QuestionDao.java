package com.yjs.battleshipGame.dao;

import com.yjs.battleshipGame.mapper.QuestionMapper;
import com.yjs.battleshipGame.entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author yeeq
 * @date 2021/2/28
 */
@Repository
public class QuestionDao {

    @Autowired
    private QuestionMapper mapper;

    /**
     * 从数据库获取所有 question
     */
    public List<Question> getAllQuestion() {
        return mapper.selectList(null);
    }
}

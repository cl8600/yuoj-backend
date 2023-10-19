package com.yupi.yuoj.judge;

import com.yupi.yuoj.model.entity.QuestionSubmit;
import com.yupi.yuoj.model.vo.QuestionVO;

public interface JudgeService {
    QuestionSubmit doJudge(long questionSubmitId);
}

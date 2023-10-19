package com.yupi.yuoj.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.yupi.yuoj.judge.codesandbox.model.JudgeInfo;
import com.yupi.yuoj.model.dto.question.JudgeCase;
import com.yupi.yuoj.model.dto.question.JudgeConfig;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.enums.JudgeInfoMessageEnum;

import java.util.List;

public class DefaultJudgeStrategy implements JudgeStrategy{
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {

        JudgeInfo judgeInfoResp = new JudgeInfo();

        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.WAITING;
        List<String> outputList = judgeContext.getOutputList();
        List<String> inputList = judgeContext.getInputList();
        Question question = judgeContext.getQuestion();
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();

        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();

        judgeInfoResp.setMemory(memory);
        judgeInfoResp.setTime(time);
        judgeInfoResp.setMessage(judgeInfoMessageEnum.getValue());

        // 判题逻辑：

        // 1.判断沙箱执行结果的输出数量是否和预期的值的数量相等
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResp.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResp;
        }
        // 2.依次判断每一项输出和预期输入是否相等
        for (int i = 0; i < judgeCaseList.size(); i++) {
            if (!judgeCaseList.get(i).equals(outputList.get(i))) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResp.setMessage(judgeInfoMessageEnum.getValue());
                return judgeInfoResp;
            }
        }
        // 3.判断题目限制是否符合要求 judgeConfig
        String judgeConfig = question.getJudgeConfig();
        JudgeConfig judgeConfigNeed = JSONUtil.toBean(judgeConfig, JudgeConfig.class);
        Long memoryLimitNeed = judgeConfigNeed.getMemoryLimit();
        Long timeLimitNeed = judgeConfigNeed.getTimeLimit();
        if (memory > memoryLimitNeed) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResp.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResp;
        }
        if (time > timeLimitNeed) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResp.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResp;
        }
        return judgeInfo;
    }
}

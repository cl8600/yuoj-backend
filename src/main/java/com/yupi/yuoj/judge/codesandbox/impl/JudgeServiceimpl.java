package com.yupi.yuoj.judge.codesandbox.impl;

import cn.hutool.json.JSONUtil;
import com.yupi.yuoj.common.ErrorCode;
import com.yupi.yuoj.exception.BusinessException;
import com.yupi.yuoj.judge.JudgeManager;
import com.yupi.yuoj.judge.JudgeService;
import com.yupi.yuoj.judge.codesandbox.CodeSandBox;
import com.yupi.yuoj.judge.codesandbox.CodeSandBoxFactory;
import com.yupi.yuoj.judge.codesandbox.CodeSandBoxProxy;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.yupi.yuoj.judge.codesandbox.model.JudgeInfo;
import com.yupi.yuoj.judge.strategy.DefaultJudgeStrategy;
import com.yupi.yuoj.judge.strategy.JudgeContext;
import com.yupi.yuoj.judge.strategy.JudgeStrategy;
import com.yupi.yuoj.model.dto.question.JudgeCase;
import com.yupi.yuoj.model.dto.question.JudgeConfig;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.entity.QuestionSubmit;
import com.yupi.yuoj.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuoj.model.enums.QuestionSubmitStatusEnum;
import com.yupi.yuoj.model.vo.QuestionVO;
import com.yupi.yuoj.service.QuestionService;
import com.yupi.yuoj.service.QuestionSubmitService;
import org.eclipse.parsson.JsonUtil;
import org.elasticsearch.Assertions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceimpl implements JudgeService {

    @Value("$sandbox.type:example")
    private String type;

    @Resource
    private QuestionService questionService;

    @Resource
    private JudgeManager judgeManager;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 判题服务
        // 1.传入题目提交id，获取对应题目，提交信息（代码，编程语言等。。。）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        Question question = questionService.getById(questionSubmit.getQuestionId());

        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"提交题目信息不存在");
        }
//        if (questionSubmit.getQuestionId() != questionSubmitId) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"题目不存在");
//        }
        // 2. 如果题目的提交状态不为等待中（WAITTING）就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"重复操作");
        }

        // 3.设置判题状态为“判题中”，防止重复判题。(相当于加锁)
        QuestionSubmit questionSubmitUdp = new QuestionSubmit();
        questionSubmitUdp.setId(questionSubmitId);
        questionSubmitUdp.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean b = questionSubmitService.updateById(questionSubmitUdp);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"题目的状态更新失败");
        }

        // 4. 调用代码沙箱执行代码
        CodeSandBox codeSandBox = CodeSandBoxFactory.newInstance(type);
        // 获取输入用例
        String judgeCase = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCase, JudgeCase.class);
        // 直接获取judgeCase的输入列表
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(questionSubmit.getCode())
                .language(questionSubmit.getLanguage())
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();

        // 5.据沙箱执行结果，设置题目的判题状态
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setQuestion(question);
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setQuestionSubmit(questionSubmit);

        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 修改数据库中的判题结果
        questionSubmitUdp = new QuestionSubmit();
        questionSubmitUdp.setId(questionSubmitId);
        questionSubmitUdp.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUdp.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
         b = questionSubmitService.updateById(questionSubmitUdp);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"题目的状态更新失败");
        }
        QuestionSubmit byId = questionSubmitService.getById(questionSubmitId);
        return byId;
    }
}

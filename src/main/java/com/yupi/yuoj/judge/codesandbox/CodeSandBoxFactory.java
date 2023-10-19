package com.yupi.yuoj.judge.codesandbox;

import com.yupi.yuoj.judge.codesandbox.impl.ExampleCodeSandBox;
import com.yupi.yuoj.judge.codesandbox.impl.RemoteCodeSandBox;
import com.yupi.yuoj.judge.codesandbox.impl.ThirdPartyCodeSandBox;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.yupi.yuoj.model.enums.QuestionSubmitLanguageEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * 沙箱代码静态工厂模式
 * 说明：根据传入的字符传参数来指定使用什么代码沙箱
 */
public class CodeSandBoxFactory {

    /**
     * 创建沙箱实例
     */
    public static CodeSandBox newInstance(String type){
        switch (type){
            case "sample":
               return new ExampleCodeSandBox();
            case "remote":
                return new RemoteCodeSandBox();
            case "thirdParty":
                return new ThirdPartyCodeSandBox();
            default:
                return new ExampleCodeSandBox();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()){
            String type = sc.next();
            CodeSandBox codeSandBox = CodeSandBoxFactory.newInstance(type);
            String code = "int main(){}";
            String language = QuestionSubmitLanguageEnum.JAVA.getValue();
            List<String> inputList = Arrays.asList("1 2", "3 4");
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .language(language)
                    .inputList(inputList).build();
            ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        }
    }

}

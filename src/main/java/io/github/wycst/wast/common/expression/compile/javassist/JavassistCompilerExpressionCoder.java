package io.github.wycst.wast.common.expression.compile.javassist;

import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.expression.ExpressionException;
import io.github.wycst.wast.common.expression.compile.CompilerEnvironment;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;
import io.github.wycst.wast.common.expression.compile.CompilerExpressionCoder;
import io.github.wycst.wast.common.expression.functions.JavassistExprFunction;
import io.github.wycst.wast.common.expression.invoker.VariableInvoker;
import javassist.*;

import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/11/13 14:12
 * @Description:
 */
public class JavassistCompilerExpressionCoder extends CompilerExpressionCoder {

    /**
     * 类型
     *
     * @param vars
     * @param environment
     * @return
     */
    @Override
    protected CompilerExpression compile(Map<String, Object> vars, CompilerEnvironment environment) {
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.importPackage(ValueHolder.class.getName());
            pool.importPackage(VariableInvoker.class.getName());
            pool.importPackage(ExprFunction.class.getName());
            pool.importPackage(JavassistExprFunction.class.getName());
            CtClass ctClass = pool.makeClass(vars.get("className").toString(), pool.get(JavassistCompilerExpression.class.getName()));

            // fields
            List<String> fieldSources = (List<String>) vars.get("fieldSources");
            if (fieldSources != null) {
                for (String fieldSource : fieldSources) {
                    CtField ctField = CtField.make(fieldSource, ctClass);
                    ctClass.addField(ctField);
                }
            }

            // Constructor
            String constructorBody = vars.get("constructorBody").toString();
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(CompilerEnvironment.class.getName())}, ctClass);
            ctConstructor.setBody(constructorBody);
            ctConstructor.setModifiers(Modifier.PUBLIC);
            ctClass.addConstructor(ctConstructor);

            // functions
            List<String> functionSources = (List<String>) vars.get("functionSources");
            if(functionSources != null) {
                for (String functionSource : functionSources) {
                    CtMethod functionMethod = CtMethod.make(functionSource, ctClass);
                    functionMethod.setModifiers(functionMethod.getModifiers() | Modifier.VARARGS);
                    ctClass.addMethod(functionMethod);
                }
            }

            // invoke object
            String invokeObjectSource = vars.get("invokeObjectSource").toString();
            CtMethod invokeObjectMethod = CtMethod.make(invokeObjectSource, ctClass);
            ctClass.addMethod(invokeObjectMethod);

            // invoke map
            String invokeMapSource = vars.get("invokeMapSource").toString();
            CtMethod invokeMapMethod = CtMethod.make(invokeMapSource, ctClass);
            ctClass.addMethod(invokeMapMethod);

            Class cls = ctClass.toClass();
            // clear
            ctClass.detach();
            return (CompilerExpression) cls.getDeclaredConstructors()[0].newInstance(environment);
        } catch (Exception e) {
            throw new ExpressionException("javassist compile fail", e);
        }
    }

}

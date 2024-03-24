package io.github.wycst.wast.common.compiler;

/**
 * java to compile code object
 *
 * @Date 2024/3/16 22:22
 * @Created by wangyc
 */
public class JavaSourceObject {

    public final String packageName;
    public final String className;
    public final String canonicalName;
    public final String javaSourceCode;

    public JavaSourceObject(String packageName, String className, String javaSourceCode) {
        this.packageName = packageName;
        this.className = className;
        this.javaSourceCode = javaSourceCode;
        this.canonicalName = packageName + "." + className;
    }
}

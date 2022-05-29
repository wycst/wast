package org.framework.light.common.compiler;

/**
 * @Author: wangy
 * @Date: 2021/9/21 0:17
 * @Description:
 */
public class MemoryClassLoader extends ClassLoader {

    private MemoryJavaFileObject javaFileObject;

    public MemoryClassLoader(MemoryJavaFileObject javaFileObject) {
        this.javaFileObject = javaFileObject;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] codeBytes = javaFileObject.getBytes();
        return defineClass(name, codeBytes, 0, codeBytes.length);
    }
}

package io.github.wycst.wast.common.compiler;

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

    public MemoryClassLoader() {
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if(javaFileObject == null) return null;
        byte[] codeBytes = javaFileObject.getBytes();
        return defineClass(name, codeBytes, 0, codeBytes.length);
    }

    public Class<?> loadClass(String name, byte[] codeBytes) {
        return defineClass(name, codeBytes, 0, codeBytes.length);
    }
}

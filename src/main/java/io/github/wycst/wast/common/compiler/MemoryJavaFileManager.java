package io.github.wycst.wast.common.compiler;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/9/21 0:06
 * @Description:
 */
public class MemoryJavaFileManager extends ForwardingJavaFileManager {

    private Map<String, MemoryJavaFileObject> fileObjectMap = new HashMap<String, MemoryJavaFileObject>();
    private MemoryJavaFileObject memoryJavaFileObject;

    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     */
    public MemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
//        if(fileObjectMap.containsKey(className)) {
//            return fileObjectMap.get(className);
//        }
        memoryJavaFileObject = new MemoryJavaFileObject(className, JavaFileObject.Kind.SOURCE);
//        fileObjectMap.put(className, fileObject);
        return memoryJavaFileObject;
    }

    public JavaFileObject createJavaFileObject(String name, String code) {
        return new MemoryInputJavaFileObject(name, code);
    }

    public MemoryJavaFileObject getMemoryJavaFileObject() {
        return memoryJavaFileObject;
    }

    /***
     * 输入
     *
     */
    class MemoryInputJavaFileObject extends SimpleJavaFileObject {
        final String code;

        MemoryInputJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}

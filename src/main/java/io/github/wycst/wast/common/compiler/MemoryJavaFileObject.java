package io.github.wycst.wast.common.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class MemoryJavaFileObject extends SimpleJavaFileObject {

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    /**
     * Construct a SimpleJavaFileObject of the given kind and with the
     * given URI.
     *
     * @param className
     * @param kind      the kind of this file object
     */
    protected MemoryJavaFileObject(String className, Kind kind) {
        super(URI.create(className + kind.extension), kind);
    }

    /**
     * create by JavaSourceObject
     *
     * @param sourceObject
     * @return
     */
    public static MemoryJavaFileObject from(JavaSourceObject sourceObject) {
        return new MemoryJavaFileObject(sourceObject.canonicalName, Kind.SOURCE);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return this.outputStream;
    }

    public byte[] getBytes() {
        byte[] bytes = this.outputStream.toByteArray();
        outputStream.reset();
        return bytes;
    }
}
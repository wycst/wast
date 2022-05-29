package org.framework.light.log;

import java.io.OutputStream;

public class ConsoleHandler extends java.util.logging.ConsoleHandler {

    @Override
    protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        super.setOutputStream(System.out);
    }
}

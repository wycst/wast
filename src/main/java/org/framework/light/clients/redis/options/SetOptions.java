package org.framework.light.clients.redis.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/6/3 22:35
 * @Description:
 */
public class SetOptions {

    private Long ex;
    private Long px;
    private boolean nx;
    private boolean xx;

    public SetOptions ex(long ex) {
        this.ex = ex;
        return this;
    }

    public SetOptions px(long px) {
        this.px = px;
        return this;
    }

    public SetOptions nx(boolean nx) {
        this.nx = nx;
        return this;
    }

    public SetOptions xx(boolean xx) {
        this.xx = xx;
        return this;
    }

    public List<String> buildCommands() {
        List<String> commands = new ArrayList<String>();
        if (ex != null) {
            commands.add("EX");
            commands.add(String.valueOf(ex));
        }
        if (px != null) {
            commands.add("PX");
            commands.add(String.valueOf(px));
        }
        if (nx) {
            commands.add("NX");
        }
        if (xx) {
            commands.add("XX");
        }
        return commands;
    }

}

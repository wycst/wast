package org.framework.light.common.template;

import org.framework.light.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板管理
 *
 * @Author: wangy
 * @Date: 2021/9/21 17:39
 * @Description:
 */
public class StringTemplateManager {

    // 模板列表
    private static Map<String, StringTemplate> resourceTemplates = new HashMap<String, StringTemplate>();

    /**
     * 获取资源模板对象
     *
     * @param resource
     * @return
     */
    public static synchronized StringTemplate getStringTemplate(String resource) {
        if(StringUtils.isEmpty(resource)) {
            return null;
        }
        if(!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        if(resourceTemplates.containsKey(resource)) {
            return resourceTemplates.get(resource);
        }
        String templateSource = StringUtils.fromResource(resource);
        if(templateSource == null) {
            return null;
        }
        StringTemplate template = new StringTemplate(templateSource);
        resourceTemplates.put(resource, template);
        return template;
    }

    
}

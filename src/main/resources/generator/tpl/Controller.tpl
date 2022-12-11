package ${basePackage}.controllers;

import javax.annotation.Resource;

import ${entityPackage}.${entityName};
import ${basePackage}.services.I${upperCaseModuleName}Service;

/***
 * <p>
 * 基于实体类的restful服务
 *
 * <p>
 * Entity  ${entityName}
 * <p>
 * Table   ${tableName}
 * 
 * @author ${author}
 * @date   ${date}
 *
 */
@Controller
@PathMapping("/${modulePath}")
@Restful
public class ${upperCaseModuleName}Controller {

	// 日志
	private Log log = LogFactory.getLog(${upperCaseModuleName}Controller.class);

	@Resource
	private I${upperCaseModuleName}Service ${lowerCaseModuleName}Service;

	/***
	 * 分页查询
	 * 
	 * @param params
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@PathMapping(value = "page", method = RequestMethod.POST)
	public RestResult page(${entityName} params, long pageNum, int pageSize) {

		log.info(" query4page");
		log.info(" pageNum {}, pageSize {}", pageNum, pageSize);

		Page<${entityName}> pageResult = Page.pageInstance(${entityName}.class);
		pageResult.setPage(pageNum);
		pageResult.setPageSize(pageSize);

		OqlQuery oqlQuery = OqlQuery.create();
		// 默认ID倒序
		oqlQuery.order("id", FieldOrder.Order.ASC);

		// 自定义查询字段列表，默认查询所有字段
		// oqlQuery.addSelectFields(...fieldNames);

		// 自定义条件区域
		if (params != null) {
			oqlQuery.addConditions(ObjectUtils.getNonEmptyFields(params));
		}

		${lowerCaseModuleName}Service.queryEntityPage(pageResult, oqlQuery, params);
		return new RestResult(pageResult);
	}

	/***
	 * 
	 * 列表查询（下拉选项数据源）
	 * 
	 * @param params
	 * @return
	 */
	@PathMapping(value = "list", method = RequestMethod.POST)
	public RestResult list(${entityName} params) {

		OqlQuery oqlQuery = OqlQuery.create();
		// 默认createDate排序
		oqlQuery.order("id", FieldOrder.Order.ASC);
		// 自定义查询字段列表，默认查询所有字段
		// oqlQuery.addSelectFields(...fieldNames);

		// 自定义条件区域
		if (params != null) {
			/*
			 * // eg: if name != null
			 * if(params.getName() != null) {
			 *   oqlQuery.addCondition("name", FieldCondition.Operator.Like);  
			 * }
			 *
			 */
			oqlQuery.addConditions(ObjectUtils.getNonEmptyFields(params));
		}
		return new RestResult(${lowerCaseModuleName}Service.queryEntityList(oqlQuery, params));
	}

    /***
     *
     * 根据ids列表查询
     *
     * @param ids
     * @return
     */
    @PathMapping(value = "getByIds")
    public RestResult getByIds(String ids) {
        return new RestResult(${lowerCaseModuleName}Service.queryEntityByIds(ids.split(",")));
    }

	/**
	 * 根据id查询实体对象
	 * 
	 * @param id
	 * @return
	 */
	@PathMapping( value = "get", method = RequestMethod.GET)
    public RestResult get${module}(String id) {
		${entityName} ${lowerCaseModuleName} = ${lowerCaseModuleName}Service.getEntity(id);
        return new RestResult(${lowerCaseModuleName});
    }
	
	/**
	 * 根据id删除实体对象
	 * 
	 * @param id
	 * @return
	 */
	@PathMapping( value = "delete", method = {RequestMethod.GET, RequestMethod.DELETE})
    public RestResult delete${module}(String id) {
		${lowerCaseModuleName}Service.deleteEntity(id);
        return new RestResult("success");
    }
	
	/***
	 * 保存或者更新实体对象
	 * 
	 * @param ${lowerCaseModuleName}
	 * @return
	 */
    @PathMapping( value = "save", method = {RequestMethod.POST, RequestMethod.PUT})
    public RestResult save${module}(@RequestBody ${entityName} ${lowerCaseModuleName}) {
    	if(${lowerCaseModuleName}.getId() == null) {
    		// 自定义输入以外的其他属性如创建时间
    		// ${lowerCaseModuleName}.setXX...
    		${lowerCaseModuleName}Service.insertEntity(${lowerCaseModuleName});
    	} else {
    		// 自定义输入以外的其他属性如创建时间
    		// ${lowerCaseModuleName}.setXX...
    		// true代表只更新非空参数属性，false或者不给更新全部字段
    		${lowerCaseModuleName}Service.updateEntity(${lowerCaseModuleName}, true);
    	}
        return new RestResult(${lowerCaseModuleName});
    }
}

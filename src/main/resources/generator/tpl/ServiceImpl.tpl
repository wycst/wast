package ${basePackage}.services.impls;

import Service;
import AbstractEntityService;

import ${entityPackage}.${entityName};
import ${basePackage}.services.I${upperCaseModuleName}Service;

/**
 * <p> 基于实体类的接口服务实现
 *
 * <p>Entity: ${entityName}
 * <p>Table:  ${tableName} 
 * 
 * @author ${author}
 * @date   ${date}
 *
 */
@Service
public class ${upperCaseModuleName}ServiceImpl extends AbstractEntityService<${entityName}> implements I${upperCaseModuleName}Service {

}

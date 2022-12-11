package io.github.wycst.wast.jdbc.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 自动构建配置
 *
 * @Author: wangy
 * @Date: 2021/9/2 0:04
 * @Description:
 */
public class GeneratorContext {

    // 输出路径
    private String outFilePath;
    // 是否压缩
    private boolean zip;

    // 基本包结构
    private String basePackage;
    // 表实体包结构，如果没有指定则自动为 ${basePackage}/entitys
    private String entityPackage;
    // 要自动生成的表信息
    private String[] tableNames;
    // 删除前缀标识
    private String deletePrefixAsEntity;
    // 使用lombokData
    private boolean useLombok;
    // 是否覆盖
    private boolean overwrite;
    // 作者信息
    private String author = "";

    private boolean generateController;
    private boolean generateService;
    private boolean generateViews;

    // 构建表格选项 key -> tableName
    private Map<String, GeneratorTableOption> tableOptions;

    // 输入输出结构
    private List<GeneratorTable> generatorTables;

    public String getOutFilePath() {
        return outFilePath;
    }

    public void setOutFilePath(String outFilePath) {
        this.outFilePath = outFilePath;
    }

    public boolean isZip() {
        return zip;
    }

    public void setZip(boolean zip) {
        this.zip = zip;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getEntityPackage() {
        return entityPackage;
    }

    public void setEntityPackage(String entityPackage) {
        this.entityPackage = entityPackage;
    }

    public String[] getTableNames() {
        return tableNames;
    }

    public void setTableNames(String[] tableNames) {
        this.tableNames = tableNames;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, GeneratorTableOption> getTableOptions() {
        return tableOptions;
    }

    public void setTableOptions(Map<String, GeneratorTableOption> tableOptions) {
        this.tableOptions = tableOptions;
    }

    public String getDeletePrefixAsEntity() {
        return deletePrefixAsEntity;
    }

    public void setDeletePrefixAsEntity(String deletePrefixAsEntity) {
        this.deletePrefixAsEntity = deletePrefixAsEntity;
    }

    public List<GeneratorTable> getGeneratorTables() {
        return generatorTables;
    }

    public void setGeneratorTables(List<GeneratorTable> generatorTables) {
        this.generatorTables = generatorTables;
    }

    public boolean isUseLombok() {
        return useLombok;
    }

    public void setUseLombok(boolean useLombok) {
        this.useLombok = useLombok;
    }

    public boolean isGenerateController() {
        return generateController;
    }

    public void setGenerateController(boolean generateController) {
        this.generateController = generateController;
    }

    public boolean isGenerateService() {
        return generateService;
    }

    public void setGenerateService(boolean generateService) {
        this.generateService = generateService;
    }

    public boolean isGenerateViews() {
        return generateViews;
    }

    public void setGenerateViews(boolean generateViews) {
        this.generateViews = generateViews;
    }

    /**
     * 写入文件
     */
    public void writeFile() throws IOException {

        List<GeneratorTable> generatorTables = getGeneratorTables();
        // NullPointerException()
        generatorTables.getClass();

        // 输出流
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        File dir = new File(outFilePath);
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            if (dir.isFile()) {
                throw new RuntimeException("目录作为文件已经存在");
            }
        }
        File file = new File(outFilePath + "/generator.zip");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        // 启用压缩
        ZipOutputStream zos = new ZipOutputStream(fileOutputStream);
        // 生成zip包
        if (isZip()) {

            String basePackageDir = basePackage == null ? "java/" : "java/" + basePackage.replace(".", "/");
            if (!basePackageDir.endsWith("/")) {
                basePackageDir += "/";
            }

//            ZipEntry entitysEntry = new ZipEntry(basePackageDir + "entitys/");
//            ZipEntry controllersEntry = new ZipEntry(basePackageDir + "controllers/");
//            ZipEntry servicesEntry = new ZipEntry(basePackageDir + "services/");
//            ZipEntry implsEntry = new ZipEntry(basePackageDir + "services/impls/");
//            ZipEntry apisEntry = new ZipEntry("vue/apis/");
//            ZipEntry viewsEntry = new ZipEntry("vue/views/");
//
//            // add dirs
//            zos.putNextEntry(entitysEntry);
//            zos.putNextEntry(controllersEntry);
//            zos.putNextEntry(servicesEntry);
//            zos.putNextEntry(implsEntry);
//            zos.putNextEntry(apisEntry);
//            zos.putNextEntry(viewsEntry);

            for (GeneratorTable generatorTable : generatorTables) {
                // entity
                String entityCode = generatorTable.getEntityCode();
                String controllerCode = generatorTable.getControllerCode();
                String serviceInfCode = generatorTable.getServiceInfCode();
                String serviceImplCode = generatorTable.getServiceImplCode();
                String apiJsCode = generatorTable.getApiJsCode();
                String vueCode = generatorTable.getVueCode();

                ZipEntry entityEntry = new ZipEntry(basePackageDir + "entitys/" + generatorTable.getEntityName() + ".java");
                zos.putNextEntry(entityEntry);
                zos.write(entityCode.getBytes());
                zos.closeEntry();

                if (isGenerateController()) {
                    // controllers
                    ZipEntry controllerEntry = new ZipEntry(basePackageDir + "controllers/" + generatorTable.getUpperCaseModuleName() + "Controller.java");
                    zos.putNextEntry(controllerEntry);
                    zos.write(controllerCode.getBytes());
                    zos.closeEntry();
                }

                if (isGenerateService()) {
                    // service inf
                    ZipEntry serviceInfEntry = new ZipEntry(basePackageDir + "services/I" + generatorTable.getUpperCaseModuleName() + "Service.java");
                    zos.putNextEntry(serviceInfEntry);
                    zos.write(serviceInfCode.getBytes());
                    zos.closeEntry();

                    // service impl
                    ZipEntry serviceImplEntry = new ZipEntry(basePackageDir + "services/impls/" + generatorTable.getUpperCaseModuleName() + "ServiceImpl.java");
                    zos.putNextEntry(serviceImplEntry);
                    zos.write(serviceImplCode.getBytes());
                    zos.closeEntry();
                }

                // if create views
                if (isGenerateViews()) {
                    // apis
                    ZipEntry apiJsEntry = new ZipEntry("vue/apis/" + generatorTable.getLowerCaseModuleName() + ".js");
                    zos.putNextEntry(apiJsEntry);
                    zos.write(apiJsCode.getBytes());
                    zos.closeEntry();

                    // views
                    ZipEntry vueEntry = new ZipEntry("vue/views/" + generatorTable.getUpperCaseModuleName() + ".vue");
                    zos.putNextEntry(vueEntry);
                    zos.write(vueCode.getBytes());
                    zos.closeEntry();
                }

            }

            zos.close();
        } else {
        }
    }
}

package com.fbg.cn.rent_springboot.common.util;

import org.apache.commons.lang.ObjectUtils;
import org.apache.velocity.VelocityContext;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.fbg.cn.rent_springboot.common.util.StringUtil.lineToHump;


/**
 *
 * @author thinkam
 * @date 17-10-31
 */
public class MybatisGeneratorUtil {
	/**
	 * generatorConfig模板路径
	 */
	private static String generatorConfig_vm = "/templates/vm/generatorConfig.vm";
	/**
	 * Service模板路径
 	 */
	private static String service_vm = "/templates/vm/Service.vm";
	/**
	 * ServiceImpl模板路径
 	 */
	private static String serviceImpl_vm = "/templates/vm/ServiceImpl.vm";

	private static final String PROJECT_NAME = "rent_springboot";

	/**
	 * 根据模板生成generatorConfig.xml文件
	 * @param jdbcDriver   驱动路径
	 * @param jdbcUrl      链接
	 * @param jdbcUsername 帐号
	 * @param jdbcPassword 密码
	 * @param database      数据库
	 * @param packageName  包名
	 */
	public static void generator(
			String jdbcDriver,
			String jdbcUrl,
			String jdbcUsername,
			String jdbcPassword,
			String database,
			String packageName,
			Map<String, String> lastInsertIdTables) throws Exception {

		generatorConfig_vm = MybatisGeneratorUtil.class.getResource(generatorConfig_vm).getPath();
		System.out.println("generatorConfig.vm的路径:"+generatorConfig_vm);
		service_vm = MybatisGeneratorUtil.class.getResource(service_vm).getPath().replaceFirst("/", "");
		System.out.println("service.vm的路径:"+service_vm);
		serviceImpl_vm = MybatisGeneratorUtil.class.getResource(serviceImpl_vm).getPath().replaceFirst("/", "");
		System.out.println("serviceImpl.vm的路径:"+serviceImpl_vm);
		String targetProject = PROJECT_NAME + "-dao";

		System.out.println(MybatisGeneratorUtil.class.getResource("/").getPath());
		String basePath = MybatisGeneratorUtil.class.getResource("/").getPath().replace("/target/classes/", "").replace(targetProject, "");
		System.out.println("basePath=" + basePath);
		String generatorconfigXml = MybatisGeneratorUtil.class.getResource("/").getPath().replace("/target/classes/", "") + "/src/main/resources/generatorConfig.xml";
		System.out.println("逆向工程配置文件"+generatorconfigXml);

		targetProject = basePath ;//+ targetProject;
		System.out.println("targetproject=" + targetProject);
		String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = '" + database + "';";

		System.out.println("========== 开始生成generatorConfig.xml文件 ==========");
		List<Map<String, Object>> tables = new ArrayList<>();
		try {
			VelocityContext context = new VelocityContext();
			Map<String, Object> table;

			// 查询定制前缀项目的所有表
			JdbcUtil jdbcUtil = new JdbcUtil(jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword);
			List<Map> result = jdbcUtil.selectByParams(sql, null);
			for (Map map : result) {
				System.out.println(map.get("TABLE_NAME"));
				table = new HashMap<>(2);
				table.put("table_name", map.get("TABLE_NAME"));
				table.put("model_name", lineToHump(ObjectUtils.toString(map.get("TABLE_NAME"))));
				tables.add(table);
			}
			jdbcUtil.release();

			String targetProjectSqlMap = basePath ;//+ PROJECT_NAME + "-dao";
			context.put("tables", tables);
			context.put("generator_javaModelGenerator_targetPackage", packageName + ".dao.model");
			context.put("generator_sqlMapGenerator_targetPackage", packageName + ".dao.xml");
			context.put("generator_javaClientGenerator_targetPackage", packageName + ".dao.mapper");
			context.put("targetProject", targetProject);
			context.put("targetProject_sqlMap", targetProjectSqlMap);
			context.put("last_insert_id_tables", lastInsertIdTables);
			VelocityUtil.generate(generatorConfig_vm, generatorconfigXml, context);
			/// 删除dao模块旧代码。慎重开启，自己添加的代码会丢失。
 			/*
 			deleteDir(new File(targetProject + "/src/main/java/" + package_name.replaceAll("\\.", "/") + "/dao/model"));
			deleteDir(new File(targetProject + "/src/main/java/" + package_name.replaceAll("\\.", "/") + "/dao/mapper"));
			deleteDir(new File(targetProject_sqlMap + "/src/main/java/" + package_name.replaceAll("\\.", "/") + "/dao/mapper"));
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("========== 结束生成generatorConfig.xml文件 ==========");

		System.out.println("========== 开始运行MybatisGenerator ==========");
		List<String> warnings = new ArrayList<>();
		File configFile = new File(generatorconfigXml);
		ConfigurationParser cp = new ConfigurationParser(warnings);
		Configuration config = cp.parseConfiguration(configFile);
		//设置不覆盖之前以前的代码
		DefaultShellCallback callback = new DefaultShellCallback(false);
		MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
		myBatisGenerator.generate(null);
		for (String warning : warnings) {
			System.out.println(warning);
		}
		System.out.println("========== 结束运行MybatisGenerator ==========");

		System.out.println("========== 开始生成Service ==========");
		String ctime = new SimpleDateFormat("yy-MM-dd").format(new Date());
		String servicePath = basePath +  "/src/main/java/" + packageName.replaceAll("\\.", "/") + "/service";
		System.out.println("业务接口路径:"+servicePath);//"-service" + PROJECT_NAME +
		String serviceImplPath = basePath +  "/src/main/java/" + packageName.replaceAll("\\.", "/") + "/service/impl";
		System.out.println("业务接口实现路径:"+serviceImplPath);//PROJECT_NAME + "-service" +
		for (int i = 0; i < tables.size(); i++) {
			String model = lineToHump(ObjectUtils.toString(tables.get(i).get("table_name")));
			String service = servicePath + "/" + model + "Service.java";
			String serviceImpl = serviceImplPath + "/" + model + "ServiceImpl.java";
			// 生成service
			File serviceFile = new File(service);
			if (!serviceFile.exists()) {
				VelocityContext context = new VelocityContext();
				context.put("package_name", packageName);
				context.put("model", model);
				context.put("ctime", ctime);
				VelocityUtil.generate(service_vm, service, context);
				System.out.println(service);
			}
			// 生成serviceImpl
			File serviceImplFile = new File(serviceImpl);
			if (!serviceImplFile.exists()) {
				VelocityContext context = new VelocityContext();
				context.put("package_name", packageName);
				context.put("model", model);
				context.put("mapper", StringUtil.toLowerCaseFirstOne(model));
				context.put("ctime", ctime);
				VelocityUtil.generate(serviceImpl_vm, serviceImpl, context);
				System.out.println(serviceImpl);
			}
		}
		System.out.println("========== 结束生成Service ==========");
	}

	// 递归删除非空文件夹
	/*public static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File file : files) {
				deleteDir(file);
			}
		}
		dir.delete();
	}*/

}
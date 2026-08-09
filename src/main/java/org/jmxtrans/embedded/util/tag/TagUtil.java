package org.jmxtrans.embedded.util.tag;

import java.util.Properties;

public class TagUtil {

    public static String getTagValFromEnv(String tagEnvName) {
		assert tagEnvName != null;
		String tagVal = null;
		// 1、获取系统的相关属性，包括文件编码、操作系统名称、区域、用户名等，此属性一般由jvm自动获取，不能设置
		// NOTE: Properties.contains(...) 继承自 Hashtable.contains，匹配的是值而非键，
		// 此处应使用 containsKey 判断系统属性是否存在，否则永远无法命中系统属性。
        Properties properties = System.getProperties();
        if(properties.containsKey(tagEnvName)){
        	tagVal = System.getProperty(tagEnvName);
        }
        
        // 2、获取指定的环境变量的值。环境变量是依赖于系统的外部命名值
 		String result = System.getenv(tagEnvName);
 		if (result != null) {
 			tagVal =  result;
 		}
 		return tagVal;
    }
    
}

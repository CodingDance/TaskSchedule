package com.yiting.taskschedule.common;

import freemarker.core.Configurable;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created by hzyiting on 2016/7/12.
 */
public class StringTemplateMerger {

	private static final Logger logger = LoggerFactory.getLogger(StringTemplateMerger.class);

	private static Configuration cfg = new Configuration();

	public static void setResource(Resource resource) throws IOException {
		cfg.setDirectoryForTemplateLoading(resource.getFile());
	}

	public static String mergeTemplate(Map dataMap, String templateFilename) {
		try {
			return mergeTemplate(dataMap, templateFilename, "utf-8");
		} catch (Exception e) {
			logger.warn("freemarker merge error.", e);
		}
		return null;
	}

	public static String mergeTemplate(Map dataMap, String templateFilename, String encoding) throws TemplateException, IOException {
		cfg.setObjectWrapper(new DefaultObjectWrapper());
		cfg.setSetting(Configurable.NUMBER_FORMAT_KEY, "###0.##");
		cfg.setDefaultEncoding(encoding);
		cfg.setClassForTemplateLoading(StringTemplateMerger.class, "/");
		Template temp = cfg.getTemplate(templateFilename);
		StringWriter writer = new StringWriter();
		temp.process(dataMap, writer);
		writer.flush();
		return writer.toString();
	}

	public static Template getTemplate(String templateFilename, String encoding) throws TemplateException, IOException {
		cfg.setObjectWrapper(new DefaultObjectWrapper());
		cfg.setSetting(Configurable.NUMBER_FORMAT_KEY, "###0.##");
		cfg.setDefaultEncoding(encoding);
		return cfg.getTemplate(templateFilename);
	}

}
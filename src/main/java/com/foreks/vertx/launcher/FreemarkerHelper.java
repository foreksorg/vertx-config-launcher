package com.foreks.vertx.launcher;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * Created by sercan on 6/24/16.
 */
public class FreemarkerHelper {
	public static String processTemplate(Path path) throws IOException, TemplateException {
		Configuration configuration = new Configuration(new Version(2, 3, 23));
		configuration.setNumberFormat("0.######");
		configuration.setTemplateLoader(new FileTemplateLoader(path.getParent().toFile()));

		Template template = configuration.getTemplate(path.getFileName().toString());
		StringWriter writer = new StringWriter();
		template.process(System.getProperties(), writer);
		writer.flush();
		writer.close();
		return writer.getBuffer().toString();
	}
}

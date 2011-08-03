package com.bleedingwolf.ratpack

import groovy.text.SimpleTemplateEngine
import javax.servlet.http.HttpServletRequest

/*
 * View层的渲染类。它主要调用了groovy提供的`SimpleTemplateEngine`。自己则提供了以下功能：
 *
 * 1. 根据根目录及文件名，寻找对应的template模板文件
 * 2. 如果没找到，使用自己定义的异常页面，提示错误
 * 3. 渲染过程中出现错误，使用自己的异常页面，提示错误，并将某些包下的异常，增加`stack-thirdparty`的css（可以控制它们的显示样式，如变灰，隐藏等）
 */
class TemplateRenderer {

	/*
	 * 模板所在的根目录。
	 */
	String templateRoot = null

	/*
	 * 传入模板所的根目录
	 */
	TemplateRenderer(tr) {
		templateRoot = tr
	}

	/*
	 * 根据模板的文件名和context，渲染之。如果出现错误，显示错误页面。
	 */
	String render(templateName, context = [:]) {
		String text = ''

		try {
			text += loadTemplateText(templateName)
		} catch (java.io.IOException ex) {
			text += loadResource('com/bleedingwolf/ratpack/exception.html').text
			context = [
				title: 'Template Not Found',
				message: 'Template Not Found',
				metadata: [
					'Template Name': templateName,
				],
				stacktrace: ""
			]
		}

		renderTemplate(text, context)
	}

	/*
	 * 渲染错误页面
	 */
	String renderError(Map context) {
		String text = loadResource('com/bleedingwolf/ratpack/exception.html').text

		renderTemplate(text, context)
	}

	/*
	 * 渲染异常页面。
	 */
	String renderException(Throwable ex, HttpServletRequest req) {
		def stackInfo = decodeStackTrace(ex)

		String text = loadResource('com/bleedingwolf/ratpack/exception.html').text
		Map context = [
			title: ex.class.name,
			message: ex.message,
			metadata: [
				'Request Method': req.method.toUpperCase(),
				'Request URL': req.requestURL,
				'Exception Type': ex.class.name,
				'Exception Location': "${stackInfo.rootCause.fileName}, line ${stackInfo.rootCause.lineNumber}",
			],
			stacktrace: stackInfo.html
		]

		renderTemplate(text, context)
	}

	/*
	 * 得到模板内容
	 */
	protected loadTemplateText(templateName) {
		String text = ''
		String fullTemplateFilename = [templateRoot, templateName].join(File.separator)

		try {
			new File(fullTemplateFilename).eachLine { text += it + '\n' }
		} catch (java.io.FileNotFoundException origEx) {
			def resource = loadResource(templateName)
			if (!resource) {
				throw new java.io.FileNotFoundException(templateName)
			}
			text += resource.text
		}
		return text
	}

	/*
	 * 将第三方异常信息上，套一个`<span class='stack-thirdparty'>`，以控制其显示方式。
	 */
	protected Map decodeStackTrace(Throwable t) {
		// FIXME
		// this doesn't really make sense, but I'm not sure
		// how to create a `firstPartyPrefixes` list.
		def thirdPartyPrefixes = ['sun', 'java', 'groovy', 'org.codehaus', 'org.mortbay']

		String html = '';
		html += t.toString() + '\n'
		StackTraceElement rootCause = null

		for (StackTraceElement ste: t.getStackTrace()) {
			if (thirdPartyPrefixes.any { ste.className.startsWith(it) }) {
				html += "<span class='stack-thirdparty'>        at ${ste}\n</span>"
			} else {
				html += "        at ${ste}\n"
				if (null == rootCause) rootCause = ste
			}
		}

		return [html: html, rootCause: rootCause]
	}

	/*
	 * 调用groovy提供的`SimpleTemplateEngine`，渲染模板。
	 */
	protected String renderTemplate(String text, Map context) {
		SimpleTemplateEngine engine = new SimpleTemplateEngine()
		def template = engine.createTemplate(text).make(context)
		return template.toString()
	}

	/*
	 * 通过currentThread.contextClassLoader来取得资源
	 */
	protected InputStream loadResource(String path) {
		Thread.currentThread().contextClassLoader.getResourceAsStream(path)
	}
}

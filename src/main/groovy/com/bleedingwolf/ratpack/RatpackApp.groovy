package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.routing.Route
import com.bleedingwolf.ratpack.routing.RoutingTable

/*
 * 该类表示整个web程序，将保存程序级的配置信息，并启动web server
 */
class RatpackApp {

	/*
	 * 保存http method和与之对应的RoutingTable. 我们写的如：
	 *
	 *     get("/person/:personid") {
	 *         "This is the page for person ${urlparams.personid}"
	 *     }
	 *     get("/company/:companyname/invoice/:invoiceid") {
	 *         def company = CompanyDAO.getByName(urlparams.companyname)
	 *         def invoice = company.getInvoice(urlparams.invoiceid)
	 *     }
	 *
	 * 这样的代码，最终都会变成handlers里的数据
	 */
	def handlers = [
		'GET': new RoutingTable(),
		'POST': new RoutingTable(),
	]

	/*
	 * 服务器配置
	 */
	def config = [
		port: 5000
	]

	/*
	 * 添加新的config
	 */
	def set = { setting, value ->
		config[setting] = value
	}

	/*
	 * 同`register(method, path, handler)`，只是methods中可有多个method
	 */
	void register(List methods, path, handler) {
		methods.each {
			register(it, path, handler)
		}
	}

	/*
	 * 对于
	 *     get("/person/:personid") {
	 *         "This is the page for person ${urlparams.personid}"
	 *     }
	 *
	 * 我们将得到：
	 * method - get
	 * path - /person/:personid
	 * handler - {"This is the page for person ${urlparams.personid}"}
	 * 
	 * 该这些信息保存起来，以供后用
	 */
	void register(method, path, handler) {
		method = method.toUpperCase()

		if (path instanceof String) {
			path = new Route(path)
		}

		def routingTable = handlers[method]
		if (routingTable == null) {
			routingTable = new RoutingTable()
			handlers[method] = routingTable
		}
		routingTable.attachRoute path, handler
	}

	/*
	 * 得到该method(get/set/...)对应的routingTabble，并处理请求
	 */
	Closure getHandler(method, subject) {
		return handlers[method.toUpperCase()].route(subject)
	}

	/*
	 * get("/person/:personid") {
	 *     "This is the page for person ${urlparams.personid}"
	 * }
	 */
	def get = { path, handler ->
		register('GET', path, handler)
	}

	/*
	 * post("/create") {
	 *     ...
	 * }
	 */
	def post = { path, handler ->
		register('POST', path, handler)
	}

	/*
	 * put("/xxx") {
	 *     ...
	 * }
	 */
	def put = { path, handler ->
		register('PUT', path, handler)
	}

	/*
	 * delete("/xxx") {
	 *     ...
	 * }
	 */
	def delete = { path, handler ->
		register('DELETE', path, handler)
	}

	/*
	 *
	 */
	public void prepareScriptForExecutionOnApp(String scriptName) {
		prepareScriptForExecutionOnApp(new File(scriptName))
	}

	/*
	 * 调用`GroovyScriptEngine`启动一个groovy脚本程序。
	 *
	 * 实际上我们会使用`binaries/ratpack`这个脚本，将我们写的groovy程序传进来，运行它。
	 */
	public void prepareScriptForExecutionOnApp(File scriptFile) {
		GroovyScriptEngine gse = new GroovyScriptEngine(scriptFile.canonicalPath.replace(scriptFile.name, ''))
		def app = this

		// 给脚本设置binding，我们写的
		// get('/xxx') {}
		// 这样语句中的get/post等，就是在这里绑定上的，它们都是闭包
		Binding binding = new Binding()
		binding.setVariable('get', app.get)
		binding.setVariable('post', app.post)
		binding.setVariable('put', app.put)
		binding.setVariable('delete', app.delete)
		binding.setVariable('set', app.set)
		gse.run scriptFile.name, binding
	}
}

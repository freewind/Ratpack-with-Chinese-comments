package com.bleedingwolf.ratpack;

/*
 * 该类是程序入口类，它将运行我们写的groovy文件，并启动web app
 */
class RatpackRunner {

	RatpackApp app = new RatpackApp()

	void run(File scriptFile) {
		// 运行脚本文件
    	app.prepareScriptForExecutionOnApp(scriptFile)
		// 启动web app
		RatpackServlet.serve(app)
	}
}

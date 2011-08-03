package com.bleedingwolf.ratpack.routing

import com.bleedingwolf.ratpack.RatpackRequestDelegate

/*
 * 保存各handler，即一个path pattern和它的处理器。
 *
 *     get("/person/:personid") {
 *         "This is the page for person ${urlparams.personid}"
 *     }
 *
 * 其中的`/person/:personid`即为path pattern，后面的`{"This is the page for person ${urlparams.personid}"}`即为处理器。
 */
class RoutingTable {

	/* 保存route与处理器列表 */
    def routeHandlers = []

	/* 增加一个route与处理器 */
    def attachRoute(route, handler) {
        routeHandlers << [route: route, handler: handler]
    }

	/* 处理请求。如果找到匹配的handler，则返回一个包装了它的闭包，运行时调用该handler。否则返回null */
    def route(subject) {
        def found = routeHandlers.find { null != it.route.match(subject) }
        if (found) {
            def urlparams = found.route.match(subject)
            def foundHandler = { ->
				// 在运行时，该closure的delegate实际上已经被赋值为RatpackRequestDelegate
				// 所以下一句右边的delegate，指的是RatpackRequestDelegate
                found.handler.delegate = delegate
                found.handler()

				// 总之言之，handler中的操作，都是基于那个RatpackRequestDelegate
				// get('/xxx') {
				// ... 这里的代码，对应的就是found.handler，所以在这里，我们可以调用`RatpackRequestDelegate`中的方法及属性，如：
				// render, contentType, json, urlparams, params, headers, 等等
				// }
            }
			// 将其delegate设为`RatpackRequestDelegate`
            foundHandler.delegate = new RatpackRequestDelegate()
            foundHandler.delegate.urlparams = urlparams
            return foundHandler
        }
        return null
    }
}

package com.bleedingwolf.ratpack
import org.json.JSONObject

/*
 * 该类用于读取request中的信息，并设置response，及渲染模板
 */
public class RatpackRequestDelegate {

    def renderer

    def params = [:]
    def urlparams = [:]
    def headers = [:]

    def request = null
    def response = null
    def requestParamReader = new RatpackRequestParamReader()

	/* 给response设置header */
    void setHeader(name, value) {
        response.setHeader(name.toString(), value.toString())
    }

	/* 设置request,同时读取其params，以及headers */
    void setRequest(req) {
        request = req
        params.putAll(requestParamReader.readRequestParams(req))
        
        req.headerNames.each { header ->
            def values = []
            req.getHeaders(header).each { values << it }
            if(values.size == 1)
                values = values.get(0)
            headers[header.toLowerCase()] = values
        }
    }

	/* 以html方式（如果未指定的话）渲染template */
    String render(templateName, context=[:]) {
        if(!response.containsHeader('Content-Type')) {
            setHeader('Content-Type', 'text/html')
        }
        renderer.render(templateName, context)
    }

	/* 设置response的contentType */
    void contentType(String contentType) {
        setHeader("Content-Type",contentType)
    }

	/* 以json方式渲染输出 */
    String renderJson(o) {
        if (!response.containsHeader("Content-Type")) {
            contentType("application/json")
        }
		new JSONObject(o).toString()
    }

}

package com.bleedingwolf.ratpack.routing
import java.util.regex.Pattern

/*
 * 该类用于对定义的path pattern进行解析，可判断一个请求的url，是否满足其定义。
 *
 * 如，我们已经定义了:
 *
 *     get("/company/:companyname/invoice/:invoiceid") {}
 *
 * 则`/company/:companyname/invoice/:invoiceid`就是这个Route类需要处理的path pattern。
 * 其特点为，使用`:`加一个变量名，作为占位符。这种占位符，是不包含`/?&#`的
 */
class Route {

	/* 用于保存定义的path */
    def path = ""
	/* 解析path后，对应的正则表达式(String类型)。原path中的占位符，都被替换为([^/?&#]+) */
    def regex = ""
	/* 从path中，解析出来的占位符名称 */
    def names = []

	/* 传入path，并解析之 */
    Route(p) {
        path = p
        parsePath()
    }

	/*
	 * 解析path pattern，将其中的占位符中的名称取出放到names中，同时，将它们替换为对应的正则表达式:([^/?&#]+)
	 */
    def parsePath() {
        regex = path
        
        def placeholderPattern = Pattern.compile("(:\\w+)") // 占位符的格式
        placeholderPattern.matcher(path).each {
            def name = it[1][1..-1] // 忽略开头的冒号
            regex = regex.replaceFirst(it[0], "([^/?&#]+)") // 占位符换成正则
            names << name
        }
    }

	/*
	 * 判断该url是否符合该path。是则返回解析出来的params map，否则返回null
	 */
    def match(url) {
        def params = [:] // 空map
        def matcher = Pattern.compile(regex).matcher(url)
        if (matcher.matches()) {
            names.eachWithIndex { it, i ->
                params[it] = matcher[0][i+1] // param_name : param_value
            }
            return params
        }
        else {
            return null
        }
    }
}

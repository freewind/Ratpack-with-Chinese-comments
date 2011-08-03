package com.bleedingwolf.ratpack;

/*
 * 该类用于从request中读取参数。参数的格式（是否带`[]`及带几个`[]`）也有着特别的意义，具体参看方法`readRequestParams(req)`。
 */
public class RatpackRequestParamReader {
	
	/*
	 * This method will attempt to parse params with subscripts into array and 
	 * map structures. For instance:
	 *     a=1&b=2 becomes [a:1,b:2] as usual.
	 *     a[]=1&a[]=2 becomes [a:[1,2]]
	 *     a[x]=1&a[y]=2 becomes [a:[x:1,y:2]]
	 *     a[x][]=1&a[x][]=2 becomes [a:[x:[1,2]]]
	 * Throws an Exception if subscripts are unparsable.
	 * 
	 * See also Ruby Rack's implementation:
	 * http://github.com/chneukirchen/rack/blob/master/lib/rack/utils.rb#L69
	 */
    def readRequestParams(req) {
		def params = [:]
        req.parameterNames.each { p ->
            def values = req.getParameterValues(p)
			values.each {value ->
				storeParam(subscripts(p), value, params)
            }
        }
		params
    }

	/*
	 * 得到原始的params，不会根据param name进行特别处理。
	 *
	 *     a[x]=1&a[y]=2
	 *
	 * 将会得到[ "a[x]":1, "a[y]":2 ]
	 */
	void readRequestRawParams(req) {
		def params = [:]
		req.parameterNames.each { p ->
			def values = req.getParameterValues(p)
			if (values.length == 1)
				values = values[0]
			params[p] = values
		}
		params
	}

	/*
	 * 将subscripts中的各name，及参数value，以恰当的方式保存在params中。其规则见`readRequestParams(req)`方法。
	 *
	 * subscripts形如: `[]`, `[""]`, `["abc"]`, `["abc", "def"]`
	 * params可能为List，可能为Map。如果subscripts[0]为空，params须为List，否则须为Map
	 *
	 * 它的目的是，通过反复调用这个函数，最终，我们可以把这样的一堆params:
	 *
	 *     a[x][]=1&a[x][]=2&.......
	 *
	 * 最后变为：
	 *
	 *     [a:[x:[1,2]], ... ]
	 *
	 * 这个算法写得真难懂，需改进。
	 */
	def storeParam(subscripts, value, params) {
		// Subscript "" means the param name contained [], array syntax.
		if (subscripts.size() == 0) { // subscripts不应该不空
			throw new RuntimeException("Malformed request params")
		} else if (subscripts.size() == 1) { // 只有一个普通的参数
			def subscript = subscripts[0]
			if (subscript == "") {
				params << value // ?
			} else {
				params[subscript] = value
			}
		} else {
			def subscript = subscripts[0]
			def nextSubscript = subscripts[1]
			if (subscript == "" && nextSubscript == "") {
				throw new RuntimeException("Malformed request params")
			}
			def empty = [:]
			if (nextSubscript == "") {
				empty = []
			}
			if (subscript == "") {
				if (!params instanceof List) {
					throw new RuntimeException("Malformed request params")
				}
				if (params.isEmpty() || params[-1].containsKey(nextSubscript)) {
					params << empty
				} 
				storeParam(subscripts[1..-1],value,params[-1])
			} else {
				if (!params instanceof Map) {
					throw new RuntimeException("Malformed request params")
				}
				params[subscript] = params[subscript] ?: empty
				storeParam(subscripts[1..-1],value,params[subscript])
			}
			
		}
	}

	/*
	 * 参数string可能为: abc, abc[], abc[x], abc[x][], abc[x]def
	 * 以及不正确的形式：abc[xx
	 *
	 * 对于正确的，返回值为:
	 *     abc -> ["abc"]
	 *     abc[] -> ["abc", ""]
	 *     abc[x] -> ["abc", "x"]
	 *     abc[x][] -> ["abc", "x", ""]
	 *     abc[x]def -> ["abc", "x", "def"]
	 *
	 * 不正确的返回原值:
	 *     abc[xx -> [ "abc[xx" ]
	 */
	def subscripts(string) {
		def subscripts = []
		def symbol = ""
		int nesting = 0
		def wellFormed = true;
		string.each {c ->
			switch(c) {
			case '[':
				if (++nesting != 1) {
					wellFormed = false;
				}
				if (symbol != "") {
					subscripts << symbol
					symbol = ""	
				}
				break;
			case  ']':
				if (--nesting != 0) {
					wellFormed = false;
				}
				subscripts << symbol
				symbol = ""
				break;
			default:
				symbol += c	
			}
		}
		wellFormed &= nesting == 0
		if (symbol != "") {
			subscripts << symbol
		}
		// TODO: consider throwing exception if !wellFormed 
		wellFormed ? subscripts : [string]
	}
	
}

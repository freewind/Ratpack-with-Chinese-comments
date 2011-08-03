package com.bleedingwolf.ratpack

/*
 * 这个类似乎没用上
 */
class Ratpack {
    
    static def app(closure) {
        def theApp = new RatpackApp()
        closure.delegate = theApp
        closure.call()
        return theApp
    }

}

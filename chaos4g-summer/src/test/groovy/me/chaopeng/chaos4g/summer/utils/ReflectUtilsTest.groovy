package me.chaopeng.chaos4g.summer.utils

import me.chaopeng.chaos4g.summer.aop.annotations.AspectMe
import me.chaopeng.chaos4g.summer.ioc.annotations.Inject
import spock.lang.Specification
import test.Class1

import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * me.chaopeng.chaos4g.summer.utils.ReflectUtilsTest
 *
 * @author chao
 * @version 1.0 - 2016-06-05
 */
class ReflectUtilsTest extends Specification {

    Method getMethodByName(Object object, String name) {
        return object.class.declaredMethods.find {name == it.name}
    }

    Field getFieldByName(Object object, String name) {
        return object.class.declaredFields.find {name == it.name}
    }

    def "get methods by annotation"() {
        expect:
        ReflectUtils.getMethodsByAnnotation(new Class1.Class1Inner(), AspectMe.class).collect {it.name}.sort() == ["a", "b", "c"].sort()
    }

    def "GetFieldsByAnnotation"() {
        expect:
        ReflectUtils.getFieldsByAnnotation(new Class1.Class1Inner(), Inject.class).collect {it.name}.sort() == ["i1", "i2"].sort()
    }

    def "call mathod"() {
        def obj = new Class1.Class1Inner()

        expect:
        ReflectUtils.callMethod(obj, getMethodByName(obj, "a"), null) == 1
        ReflectUtils.callMethod(obj, getMethodByName(obj, "b"), 1) == 1
        ReflectUtils.callMethod(obj, getMethodByName(obj, "c"), "ss") == "ss"
    }

    def "get set field"() {
        def obj = new Class1.Class1Inner()
        def field = getFieldByName(obj, "i1")

        expect:
        ReflectUtils.setField(obj, field, 1)
        ReflectUtils.getField(obj, field) == 1

    }
}
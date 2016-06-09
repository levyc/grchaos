package me.chaopeng.chaos4g.summer

import com.google.common.base.CaseFormat
import com.google.common.eventbus.EventBus
import groovy.util.logging.Slf4j
import me.chaopeng.chaos4g.summer.bean.NamedBean
import me.chaopeng.chaos4g.summer.bean.PackageScan
import me.chaopeng.chaos4g.summer.excwptions.SummerException
import me.chaopeng.chaos4g.summer.ioc.annotations.Bean
import me.chaopeng.chaos4g.summer.ioc.annotations.Inject
import me.chaopeng.chaos4g.summer.ioc.lifecycle.Initialization
import me.chaopeng.chaos4g.summer.utils.ReflectUtils

import java.lang.ref.WeakReference

/**
 * me.chaopeng.chaos4g.summer.Summer
 *
 * @author chao
 * @version 1.0 - 2016-06-03
 */
@Slf4j
class Summer {

    private SummerClassLoader classLoader
    private EventBus eventBus
    private Map<String, Object> namedBeans = new HashMap<>()
    private List<WeakReference<Object>> anonymousBeans = new LinkedList<>()
    private List<PackageScan> watchPackages = new LinkedList<>()
    private Set<String> watchClasses = new HashSet<>()
    private AbstractSummerModule module
    private boolean isInit = false


    Summer(String srcRoot = null, boolean autoReload = false) {
        classLoader = SummerClassLoader.create(srcRoot)
        eventBus = classLoader.eventBus
        eventBus.register(this)
    }

    ////////////////////////////////////
    // Life Cycle
    ////////////////////////////////////

    synchronized void loadModule(AbstractSummerModule module) {
        if (!isInit) {
            this.module = module
            module.summer = this
            module.configure()
            isInit = true
        }
    }

    synchronized void start() {
        doInject()
        doinitializate()
        module.start()
    }

    synchronized void upgrade() {

    }

    synchronized void stop() {
        module.stop()
    }

    private void doInject() {

    }

    private void doInject(Object object, Map m=namedBeans, boolean isUpgrade=false){
        def fields = ReflectUtils.getFieldsByAnnotation(object, Inject.class)
        fields.each { field ->
            def inject = field.getAnnotation(Inject.class)
            def name = inject.value().isEmpty() ? field.getName() : inject.value()
            def bean = m.get(name)
            if (bean == null) {
                throw new SummerException("no bean named $name")
            } else {
                ReflectUtils.setField(object, field, bean)
            }
        }
    }

    private void doinitializate(){

    }

    private void doinitializate(Object object) {
        if (object in Initialization) {
            (object as Initialization).initializate()
        }
    }

    ////////////////////////////////////
    // bean define
    ////////////////////////////////////

    NamedBean bean(String name, Object object, boolean isUpgrade = false) {
        synchronized (this) {

            // check upgrade
            def oldBean = namedBeans.get(name)
            if (oldBean != null) {
                if (!isUpgrade) {
                    throw new SummerException("Bean ${name} is duplicated. ")
                }
            }

            // put into map only if !isUpgrade
            if (!isUpgrade) {
                namedBeans.put(name, object)
            }

            return NamedBean.builder().name(name).object(object).build()
        }
    }

    NamedBean bean(Object object, boolean isUpgrade = false) {
        Bean bean = object.class.getAnnotation(Bean.class)
        if (bean != null) {
            String name
            if (!bean.value().isEmpty()) {
                name = bean.value()
            } else {
                name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, object.class.simpleName)
            }
            watchClasses.add(object.class.name)
            return this.bean(name, object, isUpgrade)
        }
        return null
    }

    NamedBean beanFromClass(Class clazz, boolean isUpgrade = false) {
        def o = clazz.newInstance()
        return bean(o, isUpgrade)
    }

    NamedBean beanFromClassName(String className, boolean isUpgrade = false) {
        return beanFromClass(classLoader.findClass(className), isUpgrade)
    }

    Map<String, Object> beansFromClasses(List<Class> classes, boolean isUpgrade = false) {
        return classes.findResults {
            beanFromClass(it, isUpgrade)
        }.collectEntries {
            [(it.name): it.object]
        }
    }

    Map<String, Object> beansFromPackage(PackageScan packageScan, boolean isUpgrade = false) {
        if (!isUpgrade) {
            watchPackages.add(packageScan)
        }
        return beansFromClasses(classLoader.scanPackage(packageScan).toList(), isUpgrade)
    }

    ////////////////////////////////////
    // get bean(s)
    ////////////////////////////////////

    Object getBean(String name) {
        return namedBeans.get(name)
    }

    public <T> Map<String, T> getBeansByType(Class<T> clazz) {
        Map<String, T> res = [:]

        namedBeans.findAll { _, v -> v in T }.each {k, v -> res.put(k, v as T)}

        return res
    }

    public <T> Map<String, T> getBeansInPackage(String packageName, Class<T> clazz = Object.class) {
        Map<String, T> res = [:]

        namedBeans.findAll { k, v ->
            v.getClass().name.startsWith(packageName) && v in T
        }.each { k, v ->
            res.put(k, v as T)
        }

        return res;
    }

}
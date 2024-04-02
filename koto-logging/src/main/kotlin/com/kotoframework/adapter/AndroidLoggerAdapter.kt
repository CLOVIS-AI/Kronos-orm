package com.kotoframework.adapter

import com.kotoframework.interfaces.Logger
import com.kotoframework.interfaces.invoke0

/**
 * Adapter [Logger] implementation integrating
 */
public class AndroidLoggerAdapter(private val tag: String) : Logger {
    // Access Android Log API by reflection, because Android SDK is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logClass = Class.forName("android.util.Log")
    private val isLoggableMethod = logClass.getMethod("isLoggable", String::class.java, Int::class.javaPrimitiveType)
    private val vMethod = logClass.getMethod("v", String::class.java, String::class.java, Throwable::class.java)
    private val dMethod = logClass.getMethod("d", String::class.java, String::class.java, Throwable::class.java)
    private val iMethod = logClass.getMethod("i", String::class.java, String::class.java, Throwable::class.java)
    private val wMethod = logClass.getMethod("w", String::class.java, String::class.java, Throwable::class.java)
    private val eMethod = logClass.getMethod("e", String::class.java, String::class.java, Throwable::class.java)

    private object Levels {
        const val VERBOSE = 2
        const val DEBUG = 3
        const val INFO = 4
        const val WARN = 5
        const val ERROR = 6
    }

    override fun isTraceEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.VERBOSE) as Boolean
    }

    override fun trace(msg: String, e: Throwable?) {
        vMethod.invoke0(null, tag, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.DEBUG) as Boolean
    }

    override fun debug(msg: String, e: Throwable?) {
        dMethod.invoke0(null, tag, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.INFO) as Boolean
    }

    override fun info(msg: String, e: Throwable?) {
        iMethod.invoke0(null, tag, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.WARN) as Boolean
    }

    override fun warn(msg: String, e: Throwable?) {
        wMethod.invoke0(null, tag, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.ERROR) as Boolean
    }

    override fun error(msg: String, e: Throwable?) {
        eMethod.invoke0(null, tag, msg, e)
    }
}

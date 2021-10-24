package com.mooner.starlight.api.helper

import com.mooner.starlight.plugincore.method.Method
import com.mooner.starlight.plugincore.method.MethodFunction
import com.mooner.starlight.plugincore.method.MethodType
import com.mooner.starlight.plugincore.project.JobLocker
import com.mooner.starlight.plugincore.project.Project
import java.util.function.Consumer
import kotlin.concurrent.schedule

class TimerMethod: Method<TimerMethod.Timer>() {

    class Timer {
        fun schedule(millis: Long): java.util.Timer {
            /*
            locker.acquire -> run                   -> if(Timer.isRunning()) else -> locker.reease()
                                |--   Timer.schedule() --|->       await          ->-|
             */
            val jobName = Thread.currentThread().name
            JobLocker.requestLock(jobName)
            return java.util.Timer().apply {
                schedule(millis) {
                    //callback.accept(null)
                    println("timer called")
                    JobLocker.requestRelease(jobName)
                }
            }
        }

        /*
         * 메모리 존나샘
         */
        fun schedule(initialDelay: Long, period: Long): java.util.Timer {
            val jobName = Thread.currentThread().name
            JobLocker.requestLock(jobName)
            return java.util.Timer().apply {
                schedule(initialDelay, period) {
                    //callback.accept(null)
                    println("timer called")
                }
            }
        }
    }

    override val name: String = "Timer"
    override val type: MethodType = MethodType.OBJECT
    override val functions: List<MethodFunction> = listOf(
        function {
            name = "schedule"
            args = arrayOf(Long::class.java, Consumer::class.java)
            returns = java.util.Timer::class.java
        }
    )
    override val instanceClass: Class<Timer> = Timer::class.java

    override fun getInstance(project: Project): Any {
        return Timer()
    }

}
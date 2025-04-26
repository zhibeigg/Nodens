package org.gitee.nodens.core

/**
 * @param duration 持续时间，-1为永久
 * */
class TempAttributeData(val duration: Long, val attributeData: List<IAttributeData>, val deathRemove: Boolean = true) {

    private val timestamp = System.currentTimeMillis()

    val timeStampOver
        get() = System.currentTimeMillis() - timestamp

    val timeStampClose
        get() = duration + timestamp

    val countdown
        get() = (timeStampClose - System.currentTimeMillis()).coerceAtLeast(0L)

    val closed: Boolean
        get() = if (duration == -1L) false else timeStampOver < duration
}
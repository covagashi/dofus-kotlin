package org.starloco.locos.script

interface ScriptMapper<T> {
    fun from(o: Any?): T
    fun to(v: T): Any?
}

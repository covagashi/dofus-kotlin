package org.starloco.locos.invoker

import java.lang.reflect.Method

class EventDispatcherInvoker<T>(val handler: Any, val methods: Method, val message: T)

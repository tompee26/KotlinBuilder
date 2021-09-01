package com.tompee.kotlinbuilder.processor

import javax.lang.model.element.Element

class ProcessorException(val element: Element, message: String) : Throwable(message)
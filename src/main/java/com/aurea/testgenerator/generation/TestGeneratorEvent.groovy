package com.aurea.testgenerator.generation

import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.CallableDeclaration
import org.springframework.context.ApplicationEvent

abstract class TestGeneratorEvent extends ApplicationEvent {
    TestGeneratorResult result
    CallableDeclaration callable
    Unit unit

    TestGeneratorEvent(Object source, Unit unit, CallableDeclaration callable, TestGeneratorResult result) {
        super(source)
        this.unit = unit
        this.result = result
        this.callable = callable
    }
}

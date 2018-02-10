package com.aurea.testgenerator.generation.constructors

import com.aurea.testgenerator.generation.*
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
abstract class AbstractConstructorTestGenerator implements TestGenerator {

    @Autowired
    JavaParserFacade solver

    @Autowired
    TestGeneratorResultReporter reporter

    @Override
    Collection<TestGeneratorResult> generate(Unit unit) {
        List<TestGeneratorResult> results = []
        new VoidVisitorAdapter<JavaParserFacade>() {
            @Override
            void visit(ConstructorDeclaration n, JavaParserFacade arg) {
                if (shouldBeVisited(unit, n)) {
                    TestGeneratorResult result = generate(n)
                    if (!result.type) {
                        result.type = getType()
                    }
                    reporter.publish(result, unit, n)
                    results << result
                }
            }
        }.visit(unit.cu, solver)
        results
    }

    protected abstract TestGeneratorResult generate(ConstructorDeclaration cd)

    protected abstract TestType getType()

    protected boolean shouldBeVisited(Unit unit, ConstructorDeclaration cd) {
        true
    }
}

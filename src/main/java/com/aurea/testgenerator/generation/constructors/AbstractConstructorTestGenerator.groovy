package com.aurea.testgenerator.generation.constructors

import com.aurea.testgenerator.generation.*
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Log4j2
abstract class AbstractConstructorTestGenerator implements TestGenerator {

    @Autowired
    JavaParserFacade solver

    @Autowired
    TestGeneratorResultReporter reporter

    @Autowired
    NomenclatureFactory namerFactory

    @Override
    Collection<TestGeneratorResult> generate(Unit unit) {
        List<TestGeneratorResult> results = []
        new VoidVisitorAdapter<JavaParserFacade>() {
            @Override
            void visit(ConstructorDeclaration n, JavaParserFacade arg) {
                if (shouldBeVisited(unit, n)) {
                    try {
                        TestGeneratorResult result = generate(n, unit)
                        if (!result.type) {
                            result.type = getType()
                        }
                        reporter.publish(result, unit, n)
                        results << result
                    } catch (Exception e) {
                        log.error "Unhandled error while generating for $unit.fullName", e
                    }
                }
            }
        }.visit(unit.cu, solver)
        results
    }

    protected abstract TestGeneratorResult generate(ConstructorDeclaration cd, Unit unitUnderTest)

    protected abstract TestType getType()

    protected boolean shouldBeVisited(Unit unit, ConstructorDeclaration cd) {
        true
    }
}

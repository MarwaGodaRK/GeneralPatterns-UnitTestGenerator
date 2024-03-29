package com.aurea.testgenerator.generation

import com.aurea.common.JavaClass
import com.aurea.testgenerator.generation.ast.TestUnit
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.generation.names.TestClassNomenclature
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import groovy.util.logging.Log4j2
import one.util.streamex.StreamEx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Log4j2
class UnitTestGenerator {

    List<TestGenerator> generators
    NomenclatureFactory nomenclatureFactory

    @Autowired
    UnitTestGenerator(List<TestGenerator> generators, NomenclatureFactory nomenclatureFactory) {
        this.generators = generators
        this.nomenclatureFactory = nomenclatureFactory
        log.info "Registered generators: ${this.generators.collect {it.class.simpleName}}"
    }

    Optional<TestUnit> tryGenerateTest(Unit unitUnderTest) {
        PackageDeclaration pd = unitUnderTest.cu.getPackageDeclaration().get()
        TestClassNomenclature classNomenclature = nomenclatureFactory.getTestClassNomenclature(unitUnderTest.javaClass)
        String testName = classNomenclature.requestTestClassName(unitUnderTest)
        ClassOrInterfaceDeclaration testClass = newTestClass(testName)
        CompilationUnit testCu = new CompilationUnit(pd,
                unitUnderTest.cu.getImports(),
                NodeList.nodeList(testClass), null)
        markAsGenerated(testClass, testCu)
        Unit test = new Unit(testCu, new JavaClass(pd.nameAsString, testName), null)
        TestUnit testUnit = new TestUnit(test)
        List<TestGeneratorResult> testGeneratorResults = StreamEx.of(generators).flatMap {
            it.generate(unitUnderTest).stream()
        }.toList()

        testUnit.addTestsAndDependencies(StreamEx.of(testGeneratorResults).flatMap { it.tests.stream() }.toList())

        boolean hasTests = testGeneratorResults.any { it.tests }
        return hasTests ? Optional.of(testUnit) : Optional.empty()
    }

    @Override
    String toString() {
        generators.collect {it.class.simpleName }.join('||')
    }

    private static ClassOrInterfaceDeclaration newTestClass(String testName) {
        new ClassOrInterfaceDeclaration(EnumSet.of(Modifier.PUBLIC), false, testName)
    }

    private static void markAsGenerated(
            ClassOrInterfaceDeclaration testClass,
            CompilationUnit testCu) {
        testClass.addSingleMemberAnnotation("Generated", '"GeneralPatterns"')
        testCu.addImport(Imports.GENERATED)
    }
}

package com.aurea.testgenerator

import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.coverage.CoverageCollector
import com.aurea.testgenerator.coverage.CoverageService
import com.aurea.testgenerator.coverage.NoCoverageService
import com.aurea.testgenerator.extensions.Extensions
import com.aurea.testgenerator.generation.MethodLevelTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorResultReporter
import com.aurea.testgenerator.generation.UnitTestGenerator
import com.aurea.testgenerator.generation.VisitReporter
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.source.JavaSourceFinder
import com.aurea.testgenerator.source.PathUnitSource
import com.aurea.testgenerator.source.SourceFilters
import com.aurea.testgenerator.source.UnitSource
import com.aurea.testgenerator.source.UnitTestWriter
import com.aurea.testgenerator.value.ArbitraryPrimitiveValuesFactory
import com.aurea.testgenerator.value.ArbitraryReferenceTypeFactory
import com.aurea.testgenerator.value.ValueFactory
import com.aurea.testgenerator.value.random.ValueFactoryImpl
import com.github.javaparser.JavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock

abstract class MatcherPipelineTest extends Specification {

    @Rule
    TemporaryFolder folder = new TemporaryFolder()

    ProjectConfiguration cfg = new ProjectConfiguration()
    UnitSource source
    Pipeline pipeline
    UnitTestWriter unitTestWriter
    UnitTestGenerator unitTestGenerator
    CoverageService coverageService
    CoverageCollector coverageCollector
    ValueFactory valueFactory = new ValueFactoryImpl(
            new ArbitraryReferenceTypeFactory(),
            new ArbitraryPrimitiveValuesFactory())
    TestGeneratorResultReporter reporter = new TestGeneratorResultReporter(mock(ApplicationEventPublisher))
    VisitReporter visitReporter = new VisitReporter(mock(ApplicationEventPublisher))
    NomenclatureFactory nomenclatureFactory = new NomenclatureFactory()

    void setupSpec() {
        Extensions.enable()
    }

    void setup() {
        cfg.out = folder.newFolder("test-out").absolutePath
        cfg.src = folder.newFolder("src").absolutePath
        cfg.testSrc = folder.newFolder("test").absolutePath

        source = new PathUnitSource(new JavaSourceFinder(cfg), cfg, SourceFilters.empty(), getSymbolSolver())
        MethodLevelTestGenerator generator = generator()
        unitTestGenerator = new UnitTestGenerator([generator])
        unitTestWriter = new UnitTestWriter(cfg)
        coverageService = new NoCoverageService()
        coverageCollector = new CoverageCollector(coverageService)

        pipeline = new Pipeline(
                source,
                unitTestGenerator,
                SourceFilters.empty(),
                unitTestWriter)
    }

    String onClassCodeExpect(String code, String expectedTest) {
        createTestedCode(code)

        pipeline.start()

        File testFile = cfg.outPath.resolve('sample').resolve('FooTest.java').toFile()
        assertThat(testFile).describedAs("Expected test to be generated but it wasn't").exists()
        String resultingTest = cfg.outPath.resolve('sample').resolve('FooTest.java').toFile().text

        assertThat(resultingTest).isEqualToNormalizingWhitespace(expectedTest)
    }

    MatcherPipelineTest withClass(String code) {
        String fullText = """
            package sample;
            
            $code
            """

        String fileName = JavaParser.parse(code).types.first().nameAsString + ".java"
        File file = new File(cfg.srcPath.toFile().absolutePath + "/sample", fileName)
        file.parentFile.mkdirs()
        file.write fullText
        this
    }

    void onClassCodeDoNotExpectTest(String code) {
        createTestedCode(code)

        pipeline.start()

        File resultingTest = cfg.outPath.resolve('sample').resolve('FooTest.java').toFile()

        assertThat(resultingTest).doesNotExist()
    }

    private void createTestedCode(String code) {
        File testFile = new File(cfg.srcPath.toFile().absolutePath + "/sample", 'Foo.java')
        testFile.parentFile.mkdirs()
        testFile.write """
        package sample;

        $code
        """
    }

    JavaParserFacade getSolver() {
        JavaParserFacade.get(new CombinedTypeSolver(
                new JavaParserTypeSolver(cfg.srcPath.toFile()),
                new ReflectionTypeSolver()
        ))
    }

    JavaSymbolSolver getSymbolSolver() {
        new JavaSymbolSolver(new CombinedTypeSolver(
                new JavaParserTypeSolver(cfg.srcPath.toFile()),
                new ReflectionTypeSolver()
        ))
    }

    abstract MethodLevelTestGenerator generator()
}

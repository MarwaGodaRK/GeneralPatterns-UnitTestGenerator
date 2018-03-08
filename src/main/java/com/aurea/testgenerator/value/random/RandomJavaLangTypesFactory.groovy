package com.aurea.testgenerator.value.random

import com.aurea.testgenerator.generation.ast.DependableNode
import com.aurea.testgenerator.generation.merge.TestNodeMerger
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.value.MockExpressionBuilder
import com.aurea.testgenerator.value.NewExpressionBuilder
import com.aurea.testgenerator.value.PrimitiveValueFactory
import com.aurea.testgenerator.value.ReferenceTypeFactory
import com.aurea.testgenerator.value.Resolution
import com.aurea.testgenerator.value.Types
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.utils.Pair
import org.apache.commons.lang.math.RandomUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RandomJavaLangTypesFactory implements ReferenceTypeFactory {

    ValueFactory valueFactory
    PrimitiveValueFactory primitiveValueFactory
    boolean mockByDefault = true

    @Autowired
    RandomJavaLangTypesFactory(PrimitiveValueFactory primitiveValueFactory,
                               @Value("\${type.factory.mockByDefault:true}") boolean mockByDefault) {
        this.primitiveValueFactory = primitiveValueFactory
        this.mockByDefault = mockByDefault
    }

    @Override
    Optional<DependableNode<Expression>> get(ResolvedReferenceType type) {
        if (Types.isBoxedPrimitive(type)) {
            return Optional.of(primitiveValueFactory.get(Types.unbox(type)))
        }
        if (Types.isString(type)) {
            return Optional.of(DependableNode.from(new StringLiteralExpr(RandomStringPool.next())))
        }
        if (Types.isList(type) || Types.isCollection(type) || Types.isIterable(type)) {
            Optional<DependableNode<Expression>> componentValue = getCollectionComponentValue(type)
            DependableNode<Expression> expression = componentValue.map {
                it.dependency.imports << Imports.COLLECTIONS
                Expression component = it.node
                it.node = JavaParser.parseExpression("Collections.singletonList($component)")
                it
            }.orElseGet {
                DependableNode<Expression> listOfObject = new DependableNode<>()
                listOfObject.dependency.imports << Imports.COLLECTIONS
                listOfObject.node = JavaParser.parseExpression("Collections.singletonList(new Object())")
                listOfObject
            }
            return Optional.of(expression)
        }
        if (Types.isSet(type)) {
            Optional<DependableNode<Expression>> componentValue = getCollectionComponentValue(type)
            DependableNode<Expression> expression = componentValue.map {
                it.dependency.imports << Imports.COLLECTIONS
                Expression component = it.node
                it.node = JavaParser.parseExpression("Collections.singleton($component)")
                it
            }.orElseGet {
                DependableNode<Expression> setOfObject = new DependableNode<>()
                setOfObject.dependency.imports << Imports.COLLECTIONS
                setOfObject.node = JavaParser.parseExpression("Collections.singleton(new Object())")
                setOfObject
            }
            return Optional.of(expression)
        }
        if (Types.isMap(type)) {
            List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParameters = type.getTypeParametersMap()
            ResolvedType keyType
            ResolvedType valueType
            if (typeParameters.size() != 2) {
                keyType = Resolution.tryResolve(Types.OBJECT).get()
                valueType = Resolution.tryResolve(Types.OBJECT).get()
            } else {
                keyType = typeParameters[0].b
                valueType = typeParameters[1].b
            }

            Optional<DependableNode<Expression>> keyTypeValue = valueFactory.getExpression(keyType)
            Optional<DependableNode<Expression>> valueTypeValue = valueFactory.getExpression(valueType)
            if (keyTypeValue.present && valueTypeValue.present) {
                DependableNode<Expression> testNodeExpression = new DependableNode<>()
                DependableNode<Expression> keyExpression = keyTypeValue.get()
                DependableNode<Expression> valueExpression = valueTypeValue.get()
                TestNodeMerger.appendDependencies(testNodeExpression, keyExpression)
                TestNodeMerger.appendDependencies(testNodeExpression, valueExpression)

                testNodeExpression.node = JavaParser.parseExpression("ImmutableMap.of(${keyExpression.node}, ${valueExpression.node})")
                testNodeExpression.dependency.imports << Imports.IMMUTABLE_MAP

                return Optional.of(testNodeExpression)
            } else {
                return Optional.empty()
            }
        }
        if (Types.isDate(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression(
                    "new java.util.Date(${RandomUtils.nextInt(100_000)})"))
            expression.dependency.imports << Imports.DATE
            return Optional.of(expression)
        }
        if (Types.isSqlDate(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression(
                    "new java.sql.Date(${RandomUtils.nextInt(100_000)})"))
            expression.dependency.imports << Imports.SQL_DATE
            return Optional.of(expression)
        }
        if (Types.isException(type) || Types.isThrowable(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression('new Exception("Test exception")'))
            return Optional.of(expression)
        }
        if (Types.isRuntimeException(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression('new RuntimeException("Test exception")'))
            return Optional.of(expression)
        }
        if (Types.isLocale(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression('Locale.getDefault()'))
            expression.dependency.imports << Imports.LOCALE
            return Optional.of(expression)
        }
        if (Types.isEnumeration(type)) {
            ResolvedEnumDeclaration resolvedEnumDeclaration = type.typeDeclaration.asEnum()
            Optional<FieldAccessExpr> accessFirstEnum = resolvedEnumDeclaration.accessFirst()
            return accessFirstEnum.map { DependableNode.from(it) }
        }
        if (Types.isObject(type)) {
            DependableNode<Expression> expression = DependableNode.from(JavaParser.parseExpression('new Object()'))
            return Optional.of(expression)
        }
        return Optional.of(mockOrNew(type.qualifiedName))
    }

    private DependableNode<Expression> mockOrNew(String name) {
        if(mockByDefault){
            MockExpressionBuilder.build(name)
        }else{
            NewExpressionBuilder.buildDependableNode(name)
        }

    }

    private Optional<DependableNode<Expression>> getCollectionComponentValue(ResolvedReferenceType type) {
        List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParameters = type.getTypeParametersMap()
        if (typeParameters.empty) {
            return Optional.empty()
        }

        ResolvedType componentType = typeParameters.first().b
        return valueFactory.getExpression(componentType)
    }
}

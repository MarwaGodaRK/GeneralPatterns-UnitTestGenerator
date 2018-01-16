package com.aurea.methobase.meta.purity

import com.aurea.methobase.meta.JavaParserFacadeFactory
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference
import com.jasongoodwin.monads.Try

import java.util.function.BiPredicate

class UnaryExprPureFunctionPredicate implements BiPredicate<UnaryExpr, JavaParserFacade> {
    @Override
    boolean test(UnaryExpr unaryExpr, JavaParserFacade context) {
        boolean result = false
        Expression target = unaryExpr.expression
        if (target instanceof NameExpr) {
            SymbolReference<? extends ResolvedValueDeclaration> ref = Try.<SymbolReference<? extends ResolvedValueDeclaration>> ofFailable { context.solve(target) }
                                                                         .onFailure { JavaParserFacadeFactory.reportAsUnsolved(target)}
                                                                         .orElse(SymbolReference.unsolved(ResolvedValueDeclaration))
            if (ref.solved) {
                if (ref.correspondingDeclaration.parameter) {
                    result = ref.correspondingDeclaration.asParameter().getType().primitive
                }
            }
        }
        result
    }
}
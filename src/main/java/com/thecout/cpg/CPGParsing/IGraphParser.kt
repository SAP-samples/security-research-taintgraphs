package com.thecout.cpg.CPGParsing

import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration

open class IGraphParser(protected val translationUnitDeclarations: List<TranslationUnitDeclaration>) {


    open fun processGraph() {
        NotImplementedError("processGraph")
    }


}
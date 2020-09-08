package org.jetbrains.dokka.hugo

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.CommonmarkRenderer
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.MarkdownLocationProviderFactory
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.lang.StringBuilder


class HugoPlugin : DokkaPlugin() {

    val hugoPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val renderer by extending {
        (CoreExtensions.renderer
                providing { HugoRenderer(it) }
                override plugin<GfmPlugin>().renderer)
    }

    val locationProvider by extending {
        (dokkaBase.locationProviderFactory
                providing { HugoLocationProviderFactory(it) }
                override plugin<GfmPlugin>().locationProvider)
    }

    val rootCreator by extending {
        hugoPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        hugoPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaJekyll)
        } order { after(rootCreator) }
    }
}

class HugoRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override val preprocessors = context.plugin<HugoPlugin>().query { hugoPreprocessors }

    override fun buildPage(
        page: ContentPage,
        content: (StringBuilder, ContentPage) -> Unit
    ): String {
        val builder = StringBuilder()
        builder.append("+++\n")
        appendFrontMatter(page, builder)
        builder.append("+++\n")
        content(builder, page)
        return builder.toString()
    }

    private fun appendFrontMatter(page: ContentPage, builder: StringBuilder) {
        builder.append("title = \"${page.name}\"\n")
        builder.append("draft = false\n")
        builder.append("toc = false\n")
        builder.append("type = \"reference\"\n")

        // Add menu item for each package
        if (isPackage(page)) {
            builder.append("linktitle = \"${page.name}\"\n")
            builder.append("[menu.docs]\n")
            builder.append("  parent = \"hw-security-reference\"\n")
            builder.append("  weight = 1\n")
        }
    }

    private fun isPackage(page: ContentPage): Boolean {
        if (page.content.dci.kind == ContentKind.Packages) {
            return true
        }
        return false
    }
}

class HugoLocationProviderFactory(val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: RootPageNode) =
        HugoLocationProvider(pageNode, context)
}

class HugoLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DokkaLocationProvider(pageGraphRoot, dokkaContext, ".md") {
    override val PAGE_WITH_CHILDREN_SUFFIX = "_index"
}
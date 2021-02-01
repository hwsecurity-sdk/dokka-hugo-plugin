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
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer
import java.lang.StringBuilder
import org.jetbrains.dokka.plugability.configuration


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

// Hugo uses Goldmark since 0.60
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
        buildFrontMatter(page, builder)
        builder.append("+++\n\n")
        content(builder, page)
        return builder.toString()
    }

    private fun buildFrontMatter(page: ContentPage, builder: StringBuilder) {
        val hugoConfiguration = getConfig()
        val title = page.name.getTitle(hugoConfiguration)
        builder.append("title = \"${title}\"\n")
        builder.append("draft = false\n")
        builder.append("toc = false\n")
        builder.append("type = \"reference\"\n")

        // Add menu item for each package
        if (page is PackagePage) {
            val linkTitle = page.name.getLinkTitle(hugoConfiguration)
            builder.append("linktitle = \"${linkTitle}\"\n")
            builder.append("[menu.docs]\n")
            builder.append("  parent = \"hw-security-reference\"\n")
            builder.append("  weight = 1\n")
        }
    }

    private fun getConfig() : HugoConfiguration{
        var hugoConfiguration = HugoConfiguration()
        try {
            val config = configuration<HugoPlugin, HugoConfiguration>(context)
            config?.let {
                hugoConfiguration = config
            }
            return hugoConfiguration
        } catch (exception: Exception) {
            return hugoConfiguration
        }
    }

    override fun StringBuilder.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).asReversed().forEach { node ->
            if (node.isNavigable) {
                buildLink(node, page)
                append(" / ")
            } else append(node.name)
        }
        buildParagraph()
    }

    // copied from GfmPlugin
    private val PageNode.isNavigable: Boolean
        get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

    // copied from GfmPlugin
    private fun StringBuilder.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)!!) {
            append(to.name)
        }

    // copied from GfmPlugin
    private fun StringBuilder.buildParagraph() {
        append("\n\n")
    }

    private fun StringBuilder.buildB() {
        append("<b>")
    }

    private fun StringBuilder.buildEndB() {
        append("</b>")
    }

    override fun StringBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: StringBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) -> {
                childrenCallback()
                buildNewLine()
            }
            node.hasStyle(TextStyle.Paragraph) -> {
                buildParagraph()
                childrenCallback()
                buildParagraph()
            }
            node.dci.kind in setOf(ContentKind.Symbol) -> {
                buildB()
                childrenCallback()
                buildEndB()
            }
            else -> childrenCallback()
        }
    }

    override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
        fun isExternalHref(address: String) = address.contains(":/")

        if (isExternalHref(address)) {
//            if (inCodeBlock) {
//                // `[Link](/link)` -> ` `[`Link`](/link)` `
//                // whitespaces are important to properly stop code block in Hugo
//                wrap(" `[`", "`]($href)` ", body)
//            }
//            else {
            append("[")
            content()
            append("]($address)")
//            }
        } else {
//            if (inCodeBlock) {
//                wrap(" `[`", "`]({{< relref \"$href\" >}})` ", body)
//            }
//            else {
            append("[")
            content()
            append("]({{< relref \"$address\" >}})")
//            }
        }
    }

    override fun StringBuilder.buildCodeBlock(
        code: ContentCodeBlock,
        pageContext: ContentPage
    ) {
//        inCodeBlock = true
//        ensureParagraph()
        append(if (code.language.isEmpty()) "```java\n" else "```$code.language\n")
        code.children.forEach {
            buildContentNode(it, pageContext)
        }
//        ensureNewline()
        append("\n```\n")
//        appendLine()
//        inCodeBlock = false
    }

    override fun StringBuilder.buildCodeInline(
        code: ContentCodeInline,
        pageContext: ContentPage
    ) {
//        append("`")
        code.children.forEach {
            buildContentNode(it, pageContext)
        }
//        append("`")
    }

    override fun StringBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun StringBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DisplaySourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext, sourceSets)
        } else {
            val distinct = sourceSets.map {
                it to buildString { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DisplaySourceSet, String>::second, Pair<DisplaySourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                append(" ")
//                buildSourceSetTags(platforms.toSet())
                append(" $text ")
                buildNewLine()
            }
        }
    }

    // based on GfmPlugin
    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        append("<table>\n")

        buildNewLine()
        if (node.dci.kind == ContentKind.Sample || node.dci.kind == ContentKind.Parameters) {
            node.sourceSets.forEach { sourcesetData ->
//                append(sourcesetData.name)
                buildNewLine()
                buildTable(
                    node.copy(
                        children = node.children.filter { it.sourceSets.contains(sourcesetData) },
                        dci = node.dci.copy(kind = ContentKind.Main)
                    ), pageContext, sourceSetRestriction
                )
                buildNewLine()
            }
        } else {
            append("<thead>\n")
            append("<tr>\n")

            val size = node.header.size

            if (node.header.isNotEmpty()) {
                node.header.forEach {
                    append("<th>\n")
                    it.children.forEach {
                        it.build(this, pageContext, it.sourceSets)
                    }
                    buildNewLine()
                    append("</th>\n")
                }
                buildNewLine()
            } else {
                append("<th></th>\n".repeat(size))
            }
            append("</tr>\n")
            append("</thead>\n")
            append("<tbody>\n")

            node.children.forEach {
                append("<tr>\n")

//                val builder = StringBuilder()
                it.children.forEach {
                    append("<td>\n")
                    append("{{% md %}}\n")
                    append("\n")
                    append(buildString { it.build(this, pageContext) })
//                    builder.append("| ")
//                    builder.append(
//                        buildString { it.build(this, pageContext) }.replace(
//                            Regex("#+ "),
//                            ""
//                        )
//                    )  // Workaround for headers inside tables
                    append("\n")
                    append("{{% /md %}}\n")
                    append("</td>\n")
                }
//                append(builder.toString().withEntersAsHtml())
//                append(" | ".repeat(size - it.children.size))
                append("<td></td>\n".repeat(size - it.children.size))

                append("</tr>\n")
                append("\n")
            }
            append("</tbody>\n")
        }

        append("</table>\n")
    }

//    private fun String.withEntersAsHtml(): String = replace("\n", "<br>")

    // copied from GfmPlugin
    override fun StringBuilder.buildDivergent(
        node: ContentDivergentGroup,
        pageContext: ContentPage
    ) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, contentPage, sourceSet ->
                instance.before?.let { before ->
                    buildString { buildContentNode(before, pageContext, sourceSet) }
                } ?: ""
            }, { instance, contentPage, sourceSet ->
                instance.after?.let { after ->
                    buildString { buildContentNode(after, pageContext, sourceSet) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

//            buildSourceSetTags(sourceSets)
//            buildNewLine()
            instance.before?.let {
//                append("Brief description")
//                buildNewLine()
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

//            append("Content")
            buildNewLine()
            entry.groupBy {
                buildString {
                    buildContentNode(
                        it.first.divergent,
                        pageContext,
                        setOf(it.second)
                    )
                }
            }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if (sourceSets.size > 1) {
                        buildSourceSetTags(innerSourceSets)
                        buildNewLine()
                    }
                    innerInstance.divergent.build(
                        this@buildDivergent,
                        pageContext,
                        setOf(innerSourceSets.first())
                    ) // It's workaround to render content only once
                    buildNewLine()
                }

            instance.after?.let {
//                append("More info")
                buildNewLine()
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

            buildParagraph()
        }
    }

    private fun List<Pair<ContentDivergentInstance, DisplaySourceSet>>.getInstanceAndSourceSets() =
        this.let { Pair(it.first().first, it.map { it.second }.toSet()) }

    private fun StringBuilder.buildSourceSetTags(sourceSets: Set<DisplaySourceSet>) =
        append(sourceSets.joinToString(prefix = "[", postfix = "]") { it.name })

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
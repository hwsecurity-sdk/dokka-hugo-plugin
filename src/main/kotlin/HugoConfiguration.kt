package org.jetbrains.dokka.hugo

import org.jetbrains.dokka.plugability.ConfigurableBlock

data class HugoConfiguration(
    var titleReplace: HashMap<String, String>? = null,
    var titleCapitalize: Boolean = false,
    var linkTitleReplace: HashMap<String, String>? = null,
    var linkTitleCapitalize: Boolean = false,
) : ConfigurableBlock
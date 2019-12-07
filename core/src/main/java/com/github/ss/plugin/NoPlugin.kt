package com.github.ss.plugin

import com.github.ss.Core.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.github.ss.core.R.string.plugin_disabled)
}

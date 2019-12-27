package com.github.kxu03013112.plugin

import com.github.kxu03013112.Core.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.github.kxu03013112.core.R.string.plugin_disabled)
}

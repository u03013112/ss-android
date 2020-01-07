package com.github.kxu03013112free.plugin

import com.github.kxu03013112free.Core.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.github.kxu03013112free.core.R.string.plugin_disabled)
}

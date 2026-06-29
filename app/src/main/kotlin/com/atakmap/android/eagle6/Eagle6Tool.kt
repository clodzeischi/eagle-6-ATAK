package com.atakmap.android.eagle6

import android.content.Context
import android.graphics.drawable.Drawable
import com.atak.plugins.impl.AbstractPluginTool
import com.atakmap.android.plugintemplate.plugin.R

class Eagle6Tool(context: Context) : AbstractPluginTool(
    context,
    context.getString(R.string.app_name),
    context.getString(R.string.app_name),
    context.resources.getDrawable(R.drawable.ic_launcher) as Drawable,
    Eagle6DropDownReceiver.SHOW_PLUGIN
)

package com.atakmap.android.eagle6

import com.atak.plugins.impl.AbstractPlugin
import com.atak.plugins.impl.PluginContextProvider
import gov.tak.api.plugin.IServiceController

class Eagle6Plugin(serviceController: IServiceController) : AbstractPlugin(
    serviceController,
    Eagle6Tool(serviceController.getService(PluginContextProvider::class.java).pluginContext),
    Eagle6MapComponent()
)

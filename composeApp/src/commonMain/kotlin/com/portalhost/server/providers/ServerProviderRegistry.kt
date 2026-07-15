package com.portalhost.server.providers

import com.portalhost.model.ServerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServerProviderRegistry {
    private val providers = mutableMapOf<String, ServerProvider>()
    
    init {
        register(PaperProvider())
        register(FoliaProvider())
        register(PurpurProvider())
        register(VanillaProvider())
        register(FabricProvider())
        register(ForgeProvider())
        register(NeoForgeProvider())
    }
    
    fun register(provider: ServerProvider) {
        providers[provider.id] = provider
    }
    
    fun getProvider(id: String): ServerProvider? = providers[id]
    
    fun getProvidersForType(type: ServerType): List<ServerProvider> {
        return providers.values.filter { type in it.supportedTypes }.toList()
    }
    
    val allProviders: List<ServerProvider>
        get() = providers.values.toList()
    
    val providerIds: List<String>
        get() = providers.keys.toList()
}

object ServerProviderRegistryInstance {
    val instance = ServerProviderRegistry()
}

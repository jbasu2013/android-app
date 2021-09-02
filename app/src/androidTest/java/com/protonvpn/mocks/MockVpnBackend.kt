/*
 * Copyright (c) 2019 Proton Technologies AG
 * 
 * This file is part of ProtonVPN.
 * 
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.mocks

import com.protonvpn.MockSwitch
import com.protonvpn.android.ProtonApplication
import com.protonvpn.android.appconfig.AppConfig
import com.protonvpn.android.concurrency.DefaultDispatcherProvider
import com.protonvpn.android.models.config.UserData
import com.protonvpn.android.models.config.VpnProtocol
import com.protonvpn.android.models.profiles.Profile
import com.protonvpn.android.models.vpn.ConnectionParams
import com.protonvpn.android.models.vpn.Server
import com.protonvpn.android.vpn.AgentConnectionInterface
import com.protonvpn.android.vpn.CertificateRepository
import com.protonvpn.android.vpn.PrepareResult
import com.protonvpn.android.vpn.RetryInfo
import com.protonvpn.android.vpn.VpnBackend
import com.protonvpn.android.vpn.VpnState
import com.protonvpn.android.vpn.ikev2.StrongSwanBackend
import com.protonvpn.android.vpn.openvpn.OpenVpnBackend
import com.protonvpn.android.vpn.wireguard.WireguardBackend
import com.protonvpn.android.vpn.wireguard.WireguardContextWrapper
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.proton.core.network.domain.NetworkManager
import me.proton.vpn.golib.localAgent.NativeClient
import java.util.Random

typealias MockAgentProvider = (
    certInfo: CertificateRepository.CertificateResult.Success,
    hostname: String?,
    nativeClient: NativeClient
) -> AgentConnectionInterface

class MockVpnBackend(
    scope: CoroutineScope,
    networkManager: NetworkManager,
    certificateRepository: CertificateRepository,
    userData: UserData,
    appConfig: AppConfig,
    val protocol: VpnProtocol,
) : VpnBackend(
    userData = userData,
    appConfig = appConfig,
    networkManager = networkManager,
    certificateRepository = certificateRepository,
    vpnProtocol = protocol,
    mainScope = scope,
    dispatcherProvider = DefaultDispatcherProvider()
) {
    private var agentProvider: MockAgentProvider? = null
    private val random = Random()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setAgentProvider(provider: MockAgentProvider) {
        agentProvider = provider
    }

    override suspend fun prepareForConnection(
        profile: Profile,
        server: Server,
        scan: Boolean,
        numberOfPorts: Int,
        waitForAll: Boolean
    ): List<PrepareResult> {
        return if (MockSwitch.mockedConnectionUsed) {
            if (scan && failScanning)
                emptyList()
            else listOf(PrepareResult(this, object : ConnectionParams(
                    profile, server, server.getRandomConnectingDomain(), protocol) {}))
        } else {
            when (protocol) {
                VpnProtocol.IKEv2 -> {
                    val ikev2Backend = StrongSwanBackend(
                            random,
                            networkManager,
                            mainScope,
                            userData,
                            appConfig,
                            certificateRepository,
                            dispatcherProvider)
                    ikev2Backend.prepareForConnection(profile, server, scan, numberOfPorts)
                }
                VpnProtocol.OpenVPN -> {
                    val openVpnBackend = OpenVpnBackend(
                            random,
                            networkManager,
                            userData,
                            appConfig,
                            System::currentTimeMillis,
                            certificateRepository,
                            mainScope,
                            dispatcherProvider)
                    openVpnBackend.prepareForConnection(profile, server, scan, numberOfPorts)
                }
                else -> {
                    val wireguardBackend = WireguardBackend(
                            ProtonApplication.getAppContext(),
                            GoBackend(WireguardContextWrapper(ProtonApplication.getAppContext())),
                            networkManager,
                            userData,
                            appConfig,
                            certificateRepository,
                            dispatcherProvider,
                            scope
                    )
                    wireguardBackend.prepareForConnection(profile,server,scan,numberOfPorts)
                }
            }
        }
    }

    override suspend fun connect(connectionParams: ConnectionParams) {
        super.connect(connectionParams)
        vpnProtocolState = VpnState.Connecting
        vpnProtocolState = stateOnConnect
    }

    override suspend fun closeVpnTunnel(withStateChange: Boolean) {
        vpnProtocolState = VpnState.Disconnecting
        vpnProtocolState = VpnState.Disabled
    }

    override suspend fun reconnect() {
        vpnProtocolState = VpnState.Connecting
        vpnProtocolState = stateOnConnect
    }

    override fun createAgentConnection(
        certInfo: CertificateRepository.CertificateResult.Success,
        hostname: String?,
        nativeClient: NativeClient
    ) = agentProvider?.invoke(certInfo, hostname, nativeClient)
            ?: super.createAgentConnection(certInfo, hostname, nativeClient)

    override val retryInfo get() = RetryInfo(10, 10)

    var stateOnConnect: VpnState = VpnState.Connected
    var failScanning = false
}

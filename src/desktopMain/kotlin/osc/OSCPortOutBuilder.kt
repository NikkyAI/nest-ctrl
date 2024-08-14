// SPDX-FileCopyrightText: 2019 C. Ramakrishnan / Illposed Software
// SPDX-FileCopyrightText: 2021 Robin Vobruba <hoijui.quaero@gmail.com>
//
// SPDX-License-Identifier: BSD-3-Clause
package osc

import com.illposed.osc.OSCSerializerAndParserBuilder
import com.illposed.osc.transport.NetworkProtocol
import com.illposed.osc.transport.OSCPort
import com.illposed.osc.transport.OSCPortOut
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress

class OSCPortOutBuilder {
    var serializerBuilder: OSCSerializerAndParserBuilder? = null
    var remote: SocketAddress? = null
    var local: SocketAddress? = null
    var networkProtocol = NetworkProtocol.UDP

    @Throws(IOException::class)
    fun build(): OSCPortOut {
        requireNotNull(remote) { "Missing remote socket address / port." }

        if (local == null) {
            local = InetSocketAddress(OSCPort.generateWildcard(remote), 0)
        }

        if (serializerBuilder == null) {
            serializerBuilder = OSCSerializerAndParserBuilder()
        }

        return OSCPortOut(
            serializerBuilder, remote, local, networkProtocol
        )
    }

    fun setPort(port: Int): OSCPortOutBuilder {
        val address: SocketAddress = InetSocketAddress(port)
        local = address
        remote = address
        return this
    }

    fun setRemotePort(port: Int): OSCPortOutBuilder {
        remote = InetSocketAddress(port)
        return this
    }

    fun setLocalPort(port: Int): OSCPortOutBuilder {
        local = InetSocketAddress(port)
        return this
    }

    fun setSocketAddress(address: SocketAddress?): OSCPortOutBuilder {
        local = address
        remote = address
        return this
    }

    fun setLocalSocketAddress(address: SocketAddress?): OSCPortOutBuilder {
        local = address
        return this
    }

    fun setRemoteSocketAddress(address: SocketAddress?): OSCPortOutBuilder {
        remote = address
        return this
    }

    fun setNetworkProtocol(protocol: NetworkProtocol): OSCPortOutBuilder {
        networkProtocol = protocol
        return this
    }
}

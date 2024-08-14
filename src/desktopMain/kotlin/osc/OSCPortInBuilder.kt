// SPDX-FileCopyrightText: 2019 C. Ramakrishnan / Illposed Software
// SPDX-FileCopyrightText: 2021 Robin Vobruba <hoijui.quaero@gmail.com>
//
// SPDX-License-Identifier: BSD-3-Clause
package osc

import com.illposed.osc.MessageSelector
import com.illposed.osc.OSCMessageListener
import com.illposed.osc.OSCPacketDispatcher
import com.illposed.osc.OSCPacketListener
import com.illposed.osc.OSCSerializerAndParserBuilder
import com.illposed.osc.transport.NetworkProtocol
import com.illposed.osc.transport.OSCPort
import com.illposed.osc.transport.OSCPortIn
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress

class OSCPortInBuilder {
    private var parserBuilder: OSCSerializerAndParserBuilder? = null
    private var packetListeners: MutableList<OSCPacketListener>? = null
    private var local: SocketAddress? = null
    private var remote: SocketAddress? = null
    private var networkProtocol: NetworkProtocol = NetworkProtocol.UDP

    private fun addDefaultPacketListener(): OSCPacketListener {
        if (packetListeners == null) {
            packetListeners = ArrayList()
        }

        val listener: OSCPacketListener = OSCPortIn.defaultPacketListener()
        packetListeners!!.add(listener)

        return listener
    }

    @Throws(IOException::class)
    fun build(): OSCPortIn {
        requireNotNull(local) { "Missing local socket address / port." }

        if (remote == null) {
            remote = InetSocketAddress(OSCPort.generateWildcard(local), 0)
        }

        if (parserBuilder == null) {
            parserBuilder = OSCSerializerAndParserBuilder()
        }

        if (packetListeners == null) {
            addDefaultPacketListener()
        }

        return OSCPortIn(
            parserBuilder, packetListeners, local, remote, networkProtocol
        )
    }

    fun setPort(port: Int): OSCPortInBuilder {
        val address: SocketAddress = InetSocketAddress(port)
        local = address
        remote = address
        return this
    }

    fun setLocalPort(port: Int): OSCPortInBuilder {
        local = InetSocketAddress(port)
        return this
    }

    fun setRemotePort(port: Int): OSCPortInBuilder {
        remote = InetSocketAddress(port)
        return this
    }

    fun setSocketAddress(address: SocketAddress?): OSCPortInBuilder {
        local = address
        remote = address
        return this
    }

    fun setLocalSocketAddress(address: SocketAddress?): OSCPortInBuilder {
        local = address
        return this
    }

    fun setRemoteSocketAddress(address: SocketAddress?): OSCPortInBuilder {
        remote = address
        return this
    }

    fun setNetworkProtocol(protocol: NetworkProtocol): OSCPortInBuilder {
        networkProtocol = protocol
        return this
    }

    fun setPacketListeners(
        listeners: MutableList<OSCPacketListener>?
    ): OSCPortInBuilder {
        packetListeners = listeners
        return this
    }

    fun setPacketListener(listener: OSCPacketListener): OSCPortInBuilder {
        packetListeners = mutableListOf()
        packetListeners?.add(listener)
        return this
    }

    fun addPacketListener(listener: OSCPacketListener): OSCPortInBuilder {
        if (packetListeners == null) {
            packetListeners = ArrayList()
        }

        packetListeners!!.add(listener)
        return this
    }

    fun addMessageListener(
        selector: MessageSelector?, listener: OSCMessageListener?
    ): OSCPortInBuilder {
        var dispatcher: OSCPacketDispatcher = OSCPortIn.getDispatcher(packetListeners)

        if (dispatcher == null) {
            dispatcher = addDefaultPacketListener() as OSCPacketDispatcher
        }

        dispatcher.addListener(selector, listener)

        return this
    }
}

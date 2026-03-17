package com.example.myapplication.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * 简单的MySQL客户端实现
 * 直接使用Socket实现MySQL协议，绕过JDBC
 */
class SimpleMySQLClient(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val password: String,
    private val database: String
) {
    private val TAG = "SimpleMySQLClient"
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    
    /**
     * 连接到MySQL服务器
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始连接: $host:$port")
            
            // 创建Socket连接
            socket = Socket(host, port)
            socket?.soTimeout = 10000 // 10秒超时
            
            input = DataInputStream(socket?.getInputStream())
            output = DataOutputStream(socket?.getOutputStream())
            
            Log.d(TAG, "Socket连接成功")
            
            // 读取服务器握手包
            val handshake = readHandshakePacket()
            Log.d(TAG, "收到握手包: $handshake")
            
            // 发送认证包
            sendAuthPacket(handshake)
            Log.d(TAG, "已发送认证包")
            
            // 读取认证响应
            val authResponse = readAuthResponse()
            Log.d(TAG, "认证响应: $authResponse")
            
            if (!authResponse) {
                Log.e(TAG, "认证失败")
                close()
                return@withContext false
            }
            
            Log.d(TAG, "MySQL连接成功")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "连接失败", e)
            close()
            false
        }
    }
    
    /**
     * 执行查询（SELECT）
     */
    suspend fun query(sql: String): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行查询: $sql")
            
            // 发送查询命令
            sendQueryCommand(sql)
            
            // 读取结果
            val results = readQueryResults()
            
            Log.d(TAG, "查询成功，返回 ${results.size} 行")
            results
            
        } catch (e: Exception) {
            Log.e(TAG, "查询失败", e)
            emptyList()
        }
    }
    
    /**
     * 执行更新（INSERT/UPDATE/DELETE）
     * @return 受影响的行数,失败返回-1
     */
    suspend fun execute(sql: String): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "执行更新: ${sql.take(100)}...")  // 只打印前100个字符
            
            // 发送查询命令
            sendQueryCommand(sql)
            
            // 读取结果并返回受影响的行数
            val affectedRows = readUpdateResult()
            
            if (affectedRows >= 0) {
                Log.d(TAG, "✅ 更新成功,受影响行数: $affectedRows")
            } else {
                Log.w(TAG, "⚠️ 更新返回异常值: $affectedRows")
            }
            
            affectedRows
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 更新失败: ${e.message}", e)
            -1
        }
    }
    
    /**
     * 读取UPDATE/INSERT/DELETE的结果
     * @return 受影响的行数，失败返回-1
     */
    private fun readUpdateResult(): Int {
        try {
            // 读取包头
            val length = readInt3()
            val sequenceId = input!!.readByte()
            
            Log.d(TAG, "收到响应包: length=$length, sequenceId=$sequenceId")
            
            // 检查包长度是否合理
            if (length <= 0 || length > 16777215) {
                Log.e(TAG, "❌ 包长度异常: $length")
                return -1
            }
            
            // 读取整个包到字节数组
            val packetData = ByteArray(length)
            input!!.readFully(packetData)
            
            val firstByte = packetData[0].toInt() and 0xFF
            
            Log.d(TAG, "包类型字节: 0x${firstByte.toString(16)}")
            
            when {
                firstByte == 0xFF -> {
                    // 错误包
                    val errorCode = (packetData[1].toInt() and 0xFF) or ((packetData[2].toInt() and 0xFF) shl 8)
                    val errorMsg = String(packetData, 3, length - 3)
                    Log.e(TAG, "❌ MySQL错误: 错误码=$errorCode, 信息=$errorMsg")
                    throw Exception("MySQL错误: $errorCode - $errorMsg")
                }
                firstByte == 0x00 -> {
                    // OK包 - 解析affected_rows和insert_id
                    var pos = 1  // 跳过header字节
                    
                    // 读取affected_rows（length encoded integer）
                    val (affectedRows, affectedRowsLen) = parseLengthEncodedInteger(packetData, pos)
                    pos += affectedRowsLen
                    
                    // 读取insert_id（length encoded integer）
                    val (insertId, insertIdLen) = parseLengthEncodedInteger(packetData, pos)
                    pos += insertIdLen
                    
                    // 剩余的是status_flags(2), warnings(2), info等
                    
                    Log.d(TAG, "✅ OK包: affected_rows=$affectedRows, insert_id=$insertId")
                    return affectedRows.toInt()
                }
                else -> {
                    // 未知包类型 - 这可能是协议解析错误
                    Log.e(TAG, "❌ 未知的响应类型: 0x${firstByte.toString(16)}, 包长度=$length")
                    Log.e(TAG, "包内容(前20字节): ${packetData.take(20).joinToString(" ") { "0x%02X".format(it) }}")
                    
                    // 返回-1表示失败，而不是返回0
                    return -1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 读取更新结果异常: ${e.message}", e)
            return -1
        }
    }
    
    /**
     * 从字节数组中解析length encoded integer
     * @return Pair(值, 消耗的字节数)
     */
    private fun parseLengthEncodedInteger(data: ByteArray, offset: Int): Pair<Long, Int> {
        if (offset >= data.size) return Pair(0L, 0)
        
        val firstByte = data[offset].toInt() and 0xFF
        return when {
            firstByte < 0xFB -> {
                // 单字节值
                Pair(firstByte.toLong(), 1)
            }
            firstByte == 0xFC -> {
                // 2字节整数
                if (offset + 2 >= data.size) return Pair(0L, 1)
                val b1 = data[offset + 1].toInt() and 0xFF
                val b2 = data[offset + 2].toInt() and 0xFF
                val value = b1 or (b2 shl 8)
                Pair(value.toLong(), 3)
            }
            firstByte == 0xFD -> {
                // 3字节整数
                if (offset + 3 >= data.size) return Pair(0L, 1)
                val b1 = data[offset + 1].toInt() and 0xFF
                val b2 = data[offset + 2].toInt() and 0xFF
                val b3 = data[offset + 3].toInt() and 0xFF
                val value = b1 or (b2 shl 8) or (b3 shl 16)
                Pair(value.toLong(), 4)
            }
            firstByte == 0xFE -> {
                // 8字节整数
                if (offset + 8 >= data.size) return Pair(0L, 1)
                var value = 0L
                for (i in 1..8) {
                    val b = data[offset + i].toLong() and 0xFF
                    value = value or (b shl ((i - 1) * 8))
                }
                Pair(value, 9)
            }
            else -> {
                // 0xFB = NULL, 0xFF = error
                Pair(0L, 1)
            }
        }
    }
    
    /**
     * 读取长度编码的整数
     * 注意：这个方法会消耗输入流中的字节
     */
    private fun readLengthEncodedInteger(): Long {
        val firstByte = input!!.readByte().toInt() and 0xFF
        return when {
            firstByte < 0xFB -> {
                // 单字节值，直接返回
                firstByte.toLong()
            }
            firstByte == 0xFC -> {
                // 2字节整数
                readInt2().toLong()
            }
            firstByte == 0xFD -> {
                // 3字节整数
                readInt3().toLong()
            }
            firstByte == 0xFE -> {
                // 8字节整数
                readInt8()
            }
            else -> {
                // 0xFB = NULL, 0xFF = error
                0L
            }
        }
    }
    
    /**
     * 关闭连接
     */
    fun close() {
        try {
            input?.close()
            output?.close()
            socket?.close()
            Log.d(TAG, "连接已关闭")
        } catch (e: Exception) {
            Log.e(TAG, "关闭连接失败", e)
        }
    }
    
    /**
     * 读取握手包
     */
    private fun readHandshakePacket(): HandshakePacket {
        // 读取包头（3字节长度 + 1字节序号）
        val length = readInt3()
        val sequenceId = input!!.readByte()
        
        Log.d(TAG, "握手包长度: $length, 序号: $sequenceId")
        
        // 读取协议版本
        val protocolVersion = input!!.readByte()
        
        // 读取服务器版本（以null结尾的字符串）
        val serverVersion = readNullTerminatedString()
        
        // 读取连接ID（4字节）
        val connectionId = readInt4()
        
        // 读取认证插件数据第一部分（8字节）
        val authPluginData1 = ByteArray(8)
        input!!.readFully(authPluginData1)
        
        // 跳过填充字节（1字节）
        input!!.readByte()
        
        // 读取能力标志（2字节）
        val capabilityFlags1 = readInt2()
        
        // 读取字符集（1字节）
        val charset = input!!.readByte()
        
        // 读取状态标志（2字节）
        val statusFlags = readInt2()
        
        // 读取能力标志扩展（2字节）
        val capabilityFlags2 = readInt2()
        
        // 读取认证插件数据长度（1字节）
        val authPluginDataLen = input!!.readByte().toInt()
        
        // 跳过保留字节（10字节）
        input!!.skipBytes(10)
        
        // 读取认证插件数据第二部分
        val authPluginData2Len = maxOf(13, authPluginDataLen - 8)
        val authPluginData2 = ByteArray(authPluginData2Len)
        input!!.readFully(authPluginData2)
        
        // 读取认证插件名称
        val authPluginName = readNullTerminatedString()
        
        return HandshakePacket(
            protocolVersion = protocolVersion.toInt(),
            serverVersion = serverVersion,
            connectionId = connectionId,
            authPluginData = authPluginData1 + authPluginData2,
            authPluginName = authPluginName
        )
    }
    
    /**
     * 发送认证包
     */
    private fun sendAuthPacket(handshake: HandshakePacket) {
        val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        
        // 能力标志
        val capabilities = 0x00000200 or  // CLIENT_PROTOCOL_41
                          0x00008000 or  // CLIENT_SECURE_CONNECTION
                          0x00000008 or  // CLIENT_CONNECT_WITH_DB
                          0x00000004     // CLIENT_LONG_PASSWORD
        
        buffer.putInt(capabilities)
        
        // 最大包大小
        buffer.putInt(0x01000000)
        
        // 字符集（utf8mb4 = 45）
        buffer.put(45.toByte())
        
        // 填充23个字节
        buffer.put(ByteArray(23))
        
        // 用户名（以null结尾）
        buffer.put(user.toByteArray())
        buffer.put(0.toByte())
        
        // 密码（使用native_password加密）
        val encryptedPassword = encryptPassword(password, handshake.authPluginData)
        buffer.put(encryptedPassword.size.toByte())
        buffer.put(encryptedPassword)
        
        // 数据库名（以null结尾）
        buffer.put(database.toByteArray())
        buffer.put(0.toByte())
        
        // 认证插件名称
        buffer.put("mysql_native_password".toByteArray())
        buffer.put(0.toByte())
        
        // 发送数据包
        val data = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(data)
        
        sendPacket(data, 1)
    }
    
    /**
     * 读取认证响应
     */
    private fun readAuthResponse(): Boolean {
        val length = readInt3()
        val sequenceId = input!!.readByte()
        
        Log.d(TAG, "认证响应包长度: $length, 序号: $sequenceId")
        
        val header = input!!.readByte().toInt() and 0xFF
        
        return when (header) {
            0x00 -> {
                // OK包 - 必须读取完所有剩余字节！
                Log.d(TAG, "认证成功 (OK包)")
                
                // 跳过OK包的剩余内容（affected_rows, insert_id, status_flags等）
                val remaining = length - 1  // 减去已读取的header字节
                if (remaining > 0) {
                    input!!.skipBytes(remaining)
                    Log.d(TAG, "跳过OK包剩余 $remaining 字节")
                }
                
                true
            }
            0xFF -> {
                // ERR包
                val errorCode = readInt2()
                input!!.skipBytes(1) // SQL state marker
                val sqlState = ByteArray(5)
                input!!.readFully(sqlState)
                val remaining = length - 9
                val errorMessage = ByteArray(remaining)
                input!!.readFully(errorMessage)
                Log.e(TAG, "认证失败: ${String(errorMessage)}")
                false
            }
            else -> {
                Log.e(TAG, "未知响应: $header")
                // 跳过剩余字节
                val remaining = length - 1
                if (remaining > 0) {
                    input!!.skipBytes(remaining)
                }
                false
            }
        }
    }
    
    /**
     * 发送查询命令
     */
    private fun sendQueryCommand(sql: String) {
        val sqlBytes = sql.toByteArray()
        val data = ByteArray(sqlBytes.size + 1)
        data[0] = 0x03.toByte() // COM_QUERY
        System.arraycopy(sqlBytes, 0, data, 1, sqlBytes.size)
        
        Log.d(TAG, "发送查询命令，SQL长度: ${sqlBytes.size} 字节")
        Log.d(TAG, "SQL内容: $sql")
        
        sendPacket(data, 0)
        Log.d(TAG, "查询命令已发送")
    }
    
    /**
     * 读取查询结果
     */
    private fun readQueryResults(): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        
        try {
            // 读取第一个包
            val b1 = input!!.readByte().toInt() and 0xFF
            val b2 = input!!.readByte().toInt() and 0xFF
            val b3 = input!!.readByte().toInt() and 0xFF
            val length = b1 or (b2 shl 8) or (b3 shl 16)
            val sequenceId = input!!.readByte().toInt() and 0xFF
            
            Log.d(TAG, "包头字节: 0x${b1.toString(16)} 0x${b2.toString(16)} 0x${b3.toString(16)}")
            Log.d(TAG, "结果包长度: $length, 序号: $sequenceId")
            
            if (length > 100000) {
                Log.e(TAG, "包长度异常！可能读取位置错误")
                return emptyList()
            }
            
            val firstByte = input!!.readByte().toInt() and 0xFF
            Log.d(TAG, "第一个字节: 0x${firstByte.toString(16)} (十进制: $firstByte)")
            
            when {
                firstByte == 0xFF -> {
                    // 错误包
                    val errorCode = readInt2()
                    // 读取剩余错误信息
                    val remaining = length - 3
                    val errorMsg = ByteArray(remaining)
                    input!!.readFully(errorMsg)
                    Log.e(TAG, "查询返回错误，错误码: $errorCode, 信息: ${String(errorMsg)}")
                    return emptyList()
                }
                firstByte == 0x00 -> {
                    // OK包（UPDATE/INSERT/DELETE等非查询语句）
                    Log.d(TAG, "查询成功（OK包，非SELECT语句）")
                    // 跳过剩余字节
                    if (length > 1) {
                        input!!.skipBytes(length - 1)
                    }
                    return emptyList()
                }
                firstByte in 1..250 -> {
                    // 这是列数（结果集）
                    val columnCount = firstByte
                    Log.d(TAG, "列数: $columnCount，这是一个结果集")
                    
                    // 读取列定义
                    val columnNames = mutableListOf<String>()
                    for (i in 0 until columnCount) {
                        val colLength = readInt3()
                        val colSeq = input!!.readByte()
                        
                        // 读取列定义包内容
                        val colData = ByteArray(colLength)
                        input!!.readFully(colData)
                        
                        // 解析列名（简化：从包中提取）
                        val colName = parseColumnName(colData)
                        columnNames.add(colName)
                        Log.d(TAG, "列 $i: $colName")
                    }
                    
                    // 读取EOF包（如果服务器支持）
                    val eofLength = readInt3()
                    val eofSeq = input!!.readByte()
                    val eofHeader = input!!.readByte().toInt() and 0xFF
                    
                    if (eofHeader == 0xFE && eofLength < 9) {
                        // 跳过EOF包剩余内容
                        input!!.skipBytes(eofLength - 1)
                        Log.d(TAG, "读取到EOF包（列定义结束）")
                    } else {
                        // 不是EOF包，可能是第一行数据，需要回退
                        Log.w(TAG, "未找到EOF包，可能服务器不发送EOF")
                        // 这里需要处理第一行数据...
                    }
                    
                    // 读取行数据
                    var rowCount = 0
                    while (true) {
                        val rowLength = readInt3()
                        val rowSeq = input!!.readByte()
                        
                        val rowHeader = input!!.readByte().toInt() and 0xFF
                        
                        if (rowHeader == 0xFE && rowLength < 9) {
                            // EOF包，结束
                            Log.d(TAG, "读取完成，共 $rowCount 行")
                            break
                        }
                        
                        // 读取行数据
                        val rowData = mutableMapOf<String, Any?>()
                        
                        // 第一个字节已经读取，需要回退处理
                        var bytesRead = 1
                        var colIndex = 0
                        
                        // 读取第一列（使用已读取的字节）
                        if (rowHeader == 0xFB) {
                            // NULL值
                            rowData[columnNames[colIndex]] = null
                        } else if (rowHeader < 0xFB) {
                            // 字符串长度
                            val strLen = rowHeader
                            val strBytes = ByteArray(strLen)
                            input!!.readFully(strBytes)
                            rowData[columnNames[colIndex]] = String(strBytes)
                            bytesRead += strLen
                        }
                        colIndex++
                        
                        // 读取剩余列
                        while (bytesRead < rowLength && colIndex < columnCount) {
                            val lenByte = input!!.readByte().toInt() and 0xFF
                            bytesRead++
                            
                            if (lenByte == 0xFB) {
                                // NULL值
                                rowData[columnNames[colIndex]] = null
                            } else if (lenByte < 0xFB) {
                                // 字符串
                                val strBytes = ByteArray(lenByte)
                                input!!.readFully(strBytes)
                                rowData[columnNames[colIndex]] = String(strBytes)
                                bytesRead += lenByte
                            } else if (lenByte == 0xFC) {
                                // 2字节长度
                                val len = readInt2()
                                val strBytes = ByteArray(len)
                                input!!.readFully(strBytes)
                                rowData[columnNames[colIndex]] = String(strBytes)
                                bytesRead += 2 + len
                            } else if (lenByte == 0xFD) {
                                // 3字节长度
                                val len = readInt3()
                                val strBytes = ByteArray(len)
                                input!!.readFully(strBytes)
                                rowData[columnNames[colIndex]] = String(strBytes)
                                bytesRead += 3 + len
                            }
                            
                            colIndex++
                        }
                        
                        // 跳过剩余字节（如果有）
                        if (bytesRead < rowLength) {
                            input!!.skipBytes(rowLength - bytesRead)
                        }
                        
                        results.add(rowData)
                        rowCount++
                    }
                }
                else -> {
                    // 未知包类型
                    Log.e(TAG, "未知的包类型: 0x${firstByte.toString(16)}")
                    return emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取查询结果失败", e)
            return emptyList()
        }
        
        return results
    }
    
    /**
     * 从列定义包中解析列名
     */
    private fun parseColumnName(data: ByteArray): String {
        try {
            var pos = 0
            
            // 跳过catalog（length encoded string）
            pos += skipLengthEncodedString(data, pos)
            
            // 跳过schema（length encoded string）
            pos += skipLengthEncodedString(data, pos)
            
            // 跳过table（length encoded string）
            pos += skipLengthEncodedString(data, pos)
            
            // 跳过org_table（length encoded string）
            pos += skipLengthEncodedString(data, pos)
            
            // 读取name（列名）
            val nameLen = data[pos].toInt() and 0xFF
            pos++
            
            if (pos + nameLen <= data.size) {
                val nameBytes = data.copyOfRange(pos, pos + nameLen)
                return String(nameBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析列名失败", e)
        }
        
        return "unknown"
    }
    
    /**
     * 跳过Length Encoded String
     */
    private fun skipLengthEncodedString(data: ByteArray, offset: Int): Int {
        if (offset >= data.size) return 0
        
        val len = data[offset].toInt() and 0xFF
        return when {
            len < 0xFB -> 1 + len
            len == 0xFC -> 3 + ((data[offset + 1].toInt() and 0xFF) or ((data[offset + 2].toInt() and 0xFF) shl 8))
            len == 0xFD -> 4 + readInt3FromArray(data, offset + 1)
            else -> 1 // NULL or error
        }
    }
    
    /**
     * 从字节数组读取3字节整数
     */
    private fun readInt3FromArray(data: ByteArray, offset: Int): Int {
        if (offset + 2 >= data.size) return 0
        val b1 = data[offset].toInt() and 0xFF
        val b2 = data[offset + 1].toInt() and 0xFF
        val b3 = data[offset + 2].toInt() and 0xFF
        return b1 or (b2 shl 8) or (b3 shl 16)
    }
    
    /**
     * 发送数据包
     */
    private fun sendPacket(data: ByteArray, sequenceId: Int) {
        // 写入包头
        writeInt3(data.size)
        output!!.writeByte(sequenceId)
        
        // 写入数据
        output!!.write(data)
        output!!.flush()
    }
    
    /**
     * 加密密码
     */
    private fun encryptPassword(password: String, salt: ByteArray): ByteArray {
        if (password.isEmpty()) {
            return ByteArray(0)
        }
        
        val md = MessageDigest.getInstance("SHA-1")
        
        // SHA1(password)
        val hash1 = md.digest(password.toByteArray())
        
        // SHA1(SHA1(password))
        md.reset()
        val hash2 = md.digest(hash1)
        
        // SHA1(salt + SHA1(SHA1(password)))
        md.reset()
        md.update(salt, 0, 20)
        md.update(hash2)
        val hash3 = md.digest()
        
        // XOR(SHA1(password), SHA1(salt + SHA1(SHA1(password))))
        val result = ByteArray(20)
        for (i in 0 until 20) {
            result[i] = (hash1[i].toInt() xor hash3[i].toInt()).toByte()
        }
        
        return result
    }
    
    /**
     * 读取null结尾的字符串
     */
    private fun readNullTerminatedString(): String {
        val bytes = mutableListOf<Byte>()
        while (true) {
            val b = input!!.readByte()
            if (b == 0.toByte()) break
            bytes.add(b)
        }
        return String(bytes.toByteArray())
    }
    
    /**
     * 读取3字节整数（小端序）
     */
    private fun readInt3(): Int {
        val b1 = input!!.readByte().toInt() and 0xFF
        val b2 = input!!.readByte().toInt() and 0xFF
        val b3 = input!!.readByte().toInt() and 0xFF
        return b1 or (b2 shl 8) or (b3 shl 16)
    }
    
    /**
     * 写入3字节整数（小端序）
     */
    private fun writeInt3(value: Int) {
        output!!.writeByte(value and 0xFF)
        output!!.writeByte((value shr 8) and 0xFF)
        output!!.writeByte((value shr 16) and 0xFF)
    }
    
    /**
     * 读取2字节整数（小端序）
     */
    private fun readInt2(): Int {
        val b1 = input!!.readByte().toInt() and 0xFF
        val b2 = input!!.readByte().toInt() and 0xFF
        return b1 or (b2 shl 8)
    }
    
    /**
     * 读取4字节整数（小端序）
     */
    private fun readInt4(): Int {
        val b1 = input!!.readByte().toInt() and 0xFF
        val b2 = input!!.readByte().toInt() and 0xFF
        val b3 = input!!.readByte().toInt() and 0xFF
        val b4 = input!!.readByte().toInt() and 0xFF
        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
    }
    
    /**
     * 读取8字节整数（小端序）
     */
    private fun readInt8(): Long {
        val b1 = input!!.readByte().toLong() and 0xFF
        val b2 = input!!.readByte().toLong() and 0xFF
        val b3 = input!!.readByte().toLong() and 0xFF
        val b4 = input!!.readByte().toLong() and 0xFF
        val b5 = input!!.readByte().toLong() and 0xFF
        val b6 = input!!.readByte().toLong() and 0xFF
        val b7 = input!!.readByte().toLong() and 0xFF
        val b8 = input!!.readByte().toLong() and 0xFF
        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24) or 
               (b5 shl 32) or (b6 shl 40) or (b7 shl 48) or (b8 shl 56)
    }
    
    /**
     * 握手包数据类
     */
    data class HandshakePacket(
        val protocolVersion: Int,
        val serverVersion: String,
        val connectionId: Int,
        val authPluginData: ByteArray,
        val authPluginName: String
    )
}

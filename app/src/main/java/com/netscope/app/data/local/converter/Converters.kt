package com.netscope.app.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netscope.app.domain.model.Direction
import com.netscope.app.domain.model.DnsQueryType
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.Protocol
import com.netscope.app.domain.model.StatusCategory

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
            ?: emptyList()

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String =
        gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> =
        gson.fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
            ?: emptyMap()

    @TypeConverter
    fun fromProtocol(value: Protocol): String = value.name

    @TypeConverter
    fun toProtocol(value: String): Protocol =
        Protocol.entries.find { it.name == value } ?: Protocol.UNKNOWN

    @TypeConverter
    fun fromDirection(value: Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): Direction =
        Direction.entries.find { it.name == value } ?: Direction.OUTBOUND

    @TypeConverter
    fun fromHttpMethod(value: HttpMethod): String = value.name

    @TypeConverter
    fun toHttpMethod(value: String): HttpMethod =
        HttpMethod.entries.find { it.name == value } ?: HttpMethod.UNKNOWN

    @TypeConverter
    fun fromStatusCategory(value: StatusCategory): String = value.name

    @TypeConverter
    fun toStatusCategory(value: String): StatusCategory =
        StatusCategory.entries.find { it.name == value } ?: StatusCategory.UNKNOWN

    @TypeConverter
    fun fromDnsQueryType(value: DnsQueryType): String = value.name

    @TypeConverter
    fun toDnsQueryType(value: String): DnsQueryType =
        DnsQueryType.entries.find { it.name == value } ?: DnsQueryType.UNKNOWN
}
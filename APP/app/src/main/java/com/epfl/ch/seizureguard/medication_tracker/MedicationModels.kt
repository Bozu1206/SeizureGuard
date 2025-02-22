package com.epfl.ch.seizureguard.medication_tracker

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.google.gson.*
import java.lang.reflect.Type

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
}

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
        return LocalDateTime.parse(json?.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

data class Medication(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "Daily",
    val timeOfDay: List<LocalDateTime> = emptyList(),
    val shape: String = "tablet",
    val logs: List<MedicationLog>? = null
)

data class MedicationLog(
    val medicationId: String = "",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val effectiveness: Int = 5,
    val mood: Int = 5,
    val sideEffects: String? = null,
    val notes: String? = null
) 
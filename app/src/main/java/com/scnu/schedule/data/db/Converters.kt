package com.scnu.schedule.data.db

import androidx.room.TypeConverter
import com.scnu.schedule.domain.model.WeekKind
import com.scnu.schedule.domain.model.WeekPattern
import java.time.LocalDate

class Converters {
    @TypeConverter fun weekPatternToString(p: WeekPattern): String =
        "${p.kind.name}|${p.rangeStart}|${p.rangeEnd}|${p.customWeeks.sorted().joinToString(",")}"

    @TypeConverter fun stringToWeekPattern(s: String): WeekPattern {
        val parts = s.split("|")
        val kind = WeekKind.valueOf(parts[0])
        val custom = if (parts.size > 3 && parts[3].isNotEmpty())
            parts[3].split(",").mapNotNull { it.toIntOrNull() }.toSet() else emptySet()
        return WeekPattern(kind, parts[1].toInt(), parts[2].toInt(), custom)
    }

    @TypeConverter fun localDateToLong(d: LocalDate): Long = d.toEpochDay()
    @TypeConverter fun longToLocalDate(v: Long): LocalDate = LocalDate.ofEpochDay(v)
}

package com.batb4016.jarpick.domain

import com.batb4016.jarpick.data.PickHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySnapshotTest {
    @Test fun pickHistoryRecordsSelectedOptionSnapshots() {
        val history = PickHistoryEntity(
            id = "history",
            jarId = "jar",
            optionId = "option",
            optionTextSnapshot = "Pizza",
            jarNameSnapshot = "Dinner Jar",
            pickedAt = 10L,
            localDate = "2026-05-10",
            mode = DecisionMode.FAIR.name,
            accepted = null,
        )
        assertEquals("Pizza", history.optionTextSnapshot)
        assertEquals("Dinner Jar", history.jarNameSnapshot)
    }
}

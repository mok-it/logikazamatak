package mok.it.tortura.data.supabase.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import mok.it.tortura.data.supabase.SupabaseTables
import mok.it.tortura.data.supabase.dto.ItemDto
import mok.it.tortura.model.Item

class SupabaseMappersTest {

    @Test
    fun itemMapperKeepsGameScopeAndPurchaseLimit() {
        val model = ItemDto(
            id = 1,
            name = "Hint",
            price = 3,
            itemEffectId = 4,
            gameId = 5,
            maxPerTeam = 2,
        ).toModel()

        assertEquals(5, model.gameId)
        assertEquals(2, model.maxPerTeam)

        val insert = Item(
            name = "Multiplier",
            price = 8,
            itemEffectId = 9,
            gameId = 10,
            maxPerTeam = 1,
        ).toInsertDto()

        assertEquals(10, insert.gameId)
        assertEquals(1, insert.maxPerTeam)
    }

    @Test
    fun itemTableConstantsUseRenamedTables() {
        assertEquals("Items", SupabaseTables.ITEMS)
        assertEquals("ItemEffects", SupabaseTables.ITEM_EFFECTS)
    }
}

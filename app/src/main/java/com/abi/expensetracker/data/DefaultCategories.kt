package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Category

/**
 * Seed categories, with keywords aimed at the merchants a Nepali bank message actually
 * names.
 *
 * Deliberately few and broad. A long list of narrow categories is a list the user has to
 * maintain, and a spending breakdown with twenty slices answers nothing at a glance.
 *
 * There is no "Other": a transaction that matches nothing stays uncategorised, which the
 * trends screen shows as its own bar. A bucket called "Other" would look like a decision
 * the app had made rather than one still waiting to be made.
 */
object DefaultCategories {

    val ALL: List<Category> = listOf(
        Category(
            name = "Groceries & supplies",
            icon = "🛒",
            keywords = "bhatbhateni,bhat bhateni,supermarket,super market,mart,kirana," +
                "grocer,salesberry,departmental,general store"
        ),
        Category(
            name = "Transport & fuel",
            icon = "⛽",
            keywords = "petrol,pump,fuel,diesel,nepal oil,pathao,indrive,taxi,sajha," +
                "tootle,bus,transport"
        ),
        Category(
            name = "Dining & tea",
            icon = "☕",
            keywords = "restaurant,cafe,coffee,java,khaja,momo,bakery,foodmandu,food," +
                "hotel,kitchen,chiya"
        ),
        Category(
            name = "Utilities & internet",
            icon = "⚡",
            keywords = "nea,electricity,khanepani,water,worldlink,vianet,subisu,classic tech," +
                "internet,ntc,ncell,recharge,topup,top up,dishhome,netflix"
        ),
        Category(
            name = "Health & pharmacy",
            icon = "💊",
            keywords = "pharmacy,medical,medicine,hospital,clinic,norvic,grande,teaching," +
                "diagnostic,lab"
        ),
        Category(
            name = "Household & repairs",
            icon = "🧰",
            keywords = "hardware,repair,furniture,paint,plumb,electrician,carpenter,rent," +
                "maintenance"
        )
    )
}

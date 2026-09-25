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
            name = "Dining",
            icon = "🍽️",
            keywords = "restaurant,khaja,momo,bakery,foodmandu,food,hotel,kitchen"
        ),
        // Its own bucket because a daily chiya or coffee is a habit worth seeing apart
        // from meals. No bare "tea": matching is substring, and it would catch "team" and
        // "steak".
        Category(
            name = "Hot drinks & coffee",
            icon = "☕",
            keywords = "chiya,coffee,cafe,java,latte,espresso,cappuccino,milk tea"
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

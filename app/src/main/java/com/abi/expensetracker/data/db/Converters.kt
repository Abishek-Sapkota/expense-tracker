package com.abi.expensetracker.data.db

import androidx.room.TypeConverter
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.Source

class Converters {
    @TypeConverter fun directionToString(d: Direction): String = d.name
    @TypeConverter fun stringToDirection(s: String): Direction = Direction.valueOf(s)

    @TypeConverter fun sourceToString(s: Source): String = s.name
    @TypeConverter fun stringToSource(s: String): Source = Source.valueOf(s)

    @TypeConverter fun loanKindToString(k: LoanKind): String = k.name
    @TypeConverter fun stringToLoanKind(s: String): LoanKind = LoanKind.valueOf(s)
}

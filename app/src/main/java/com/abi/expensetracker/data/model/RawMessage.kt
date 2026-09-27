package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abi.expensetracker.data.StableId

/**
 * Verbatim message text, never modified and never deleted.
 *
 * This is the only irreplaceable table: the phone's SMS inbox eventually drops old
 * messages, so once a message is here it is the sole surviving copy. Parsing runs
 * off these rows and can be re-run any time the rules improve.
 */
@Entity(
    tableName = "raw_messages",
    indices = [Index("sentAt"), Index("sender")]
)
data class RawMessage(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val sentAt: Long,
    val source: Source,
    val importedAt: Long
) {
    companion object {
        /**
         * Deterministic across exports and devices, so re-importing a backup is a
         * no-op rather than a duplicate.
         */
        fun idFor(sender: String, body: String, sentAt: Long): String =
            StableId.sha256("$sender|$body|$sentAt")
    }
}

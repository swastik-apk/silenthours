package com.example.silenthours.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.silenthours.data.db.Converters

@Entity(tableName = "groups")
@TypeConverters(Converters::class)
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Default value for autoGenerate
    val name: String,
    val contactIds: List<String> // Changed from List<Contact>
)

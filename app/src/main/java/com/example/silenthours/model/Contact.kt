package com.example.silenthours.model

import androidx.room.Entity

// The primaryKeys attribute is used because 'id' is part of a composite key,
// or if you want to explicitly define it, even for a single key.
// For a single primary key, @PrimaryKey on the field is more common,
// but this is also valid. Let's stick to @PrimaryKey on the field for convention.
// @Entity(tableName = "contacts", primaryKeys = ["id"])

@Entity(tableName = "contacts")
data class Contact(
    @androidx.room.PrimaryKey // Explicitly define id as the primary key
    val id: String, // Contact ID from Android's Contact Provider
    val name: String,
    val phoneNumber: String?
)

package org.sportstechsolutions.apitacticsapp.model

enum class AccessRole {
    OWNER, VIEWER, EDITOR, NONE;

    fun canView() = this == VIEWER || this == EDITOR
    fun canEdit(): Boolean = this == EDITOR || this == OWNER
}

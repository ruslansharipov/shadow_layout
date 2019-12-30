package ru.surfstudio.shadow_layout

enum class BlurType(val id: Int) {

    RENDERSCRIPT(0),
    STACK(1);

    companion object {

        fun getById(id: Int): BlurType {
            return values().find { it.id == id } ?: RENDERSCRIPT
        }
    }
}
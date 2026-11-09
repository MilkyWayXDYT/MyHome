package com.robotpaw.myhome.game

/**
 * Питомец, которого игрок выбирает в финале
 * @param title - название
 * @param genitive - «для кого» (корм для кота)
 * @param toy - игрушка питомца (в винительном падеже: «возьмите мышку»)
 */
enum class PetKind(val title: String, val genitive: String, val toy: String) {
    CAT("Кот", "кота", "мышку"),
    DOG("Собака", "собаки", "мячик"),
    PIG("Свинья", "свиньи", "мячик")
}
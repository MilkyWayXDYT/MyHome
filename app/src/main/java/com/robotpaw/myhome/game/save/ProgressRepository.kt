package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.Rooms

/**
 * Сохраняет и загружает прогресс игры: пройденные задания и выбранного питомца
 * Калибровка не сохраняется: по решению команды игрок проходит её заново при каждом запуске
 * Новые данные (звёзды, лучшие результаты) добавляются сюда же отдельными ключами
 */
class ProgressRepository(private val storage: KeyValueStorage) {

    /**
     * Загружает прогресс. Лишнее из сохранения отбрасывается:
     * задания, которых больше нет в игре, и питомец, которого больше нет в списке
     */
    fun load(): GameProgress {
        val knownMissions = Rooms.ALL.flatMap { it.missions }.map { it.id }.toSet()
        val completed = storage.getStringSet(KEY_COMPLETED).filter { it in knownMissions }.toSet()
        val pet = storage.getString(KEY_PET)?.let { name -> PetKind.entries.firstOrNull { it.name == name } }
        return GameProgress(completedMissions = completed, pet = pet)
    }

    fun save(progress: GameProgress) {
        storage.putStringSet(KEY_COMPLETED, progress.completedMissions)
        storage.putString(KEY_PET, progress.pet?.name)
    }

    companion object {
        const val KEY_COMPLETED = "completed_missions"
        const val KEY_PET = "pet"
    }
}

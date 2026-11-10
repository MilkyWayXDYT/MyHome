package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.MissionRecord
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.Stars

/**
 * Сохраняет и загружает прогресс игры: пройденные задания, выбранного питомца, лучшие результаты и звёзды
 * Профиль калибровки хранится отдельно, в ProfileRepository
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
        val records = knownMissions.mapNotNull { id -> parseRecord(storage.getString(recordKey(id)))?.let { id to it } }.toMap()
        val stars = knownMissions.mapNotNull { id -> storage.getString(starsKey(id))?.toIntOrNull()?.let { id to it.coerceIn(0, Stars.MAX) } }.toMap()
        return GameProgress(completedMissions = completed, pet = pet, records = records, stars = stars)
    }

    fun save(progress: GameProgress) {
        storage.putStringSet(KEY_COMPLETED, progress.completedMissions)
        storage.putString(KEY_PET, progress.pet?.name)
        // идём по всем заданиям, чтобы при сбросе прогресса старые рекорды удалились
        Rooms.ALL.flatMap { it.missions }.forEach { mission ->
            storage.putString(recordKey(mission.id), progress.records[mission.id]?.let { encodeRecord(it) })
            storage.putString(starsKey(mission.id), progress.stars[mission.id]?.toString())
        }
    }

    private fun recordKey(missionId: String) = "$KEY_RECORD_PREFIX$missionId"

    private fun starsKey(missionId: String) = "$KEY_STARS_PREFIX$missionId"

    private fun encodeRecord(record: MissionRecord) = "${record.durationMs};${record.mistakes};${record.cleanPercent}"

    private fun parseRecord(text: String?): MissionRecord? {
        val parts = text?.split(";") ?: return null
        if (parts.size != 3) return null
        val duration = parts[0].toLongOrNull() ?: return null
        val mistakes = parts[1].toIntOrNull() ?: return null
        val clean = parts[2].toIntOrNull() ?: return null
        return MissionRecord(duration, mistakes, clean)
    }

    companion object {
        const val KEY_COMPLETED = "completed_missions"
        const val KEY_PET = "pet"
        const val KEY_RECORD_PREFIX = "record_"
        const val KEY_STARS_PREFIX = "stars_"
    }
}
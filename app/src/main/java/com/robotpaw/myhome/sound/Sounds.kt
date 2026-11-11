package com.robotpaw.myhome.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import com.robotpaw.myhome.R
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.PetKind

/**
 * Звуковой эффект
 * @param volume - громкость эффекта 0..1 (файлы выровнены по громкости, это подстройка на слух)
 */
enum class Sfx(@RawRes val res: Int, val volume: Float = 0.8f) {
    // меню и экраны
    TAP(R.raw.sfx_tap, 0.5f),
    DONE(R.raw.sfx_done),
    STOP(R.raw.sfx_stop, 0.7f),
    WELCOME(R.raw.sfx_welcome, 0.7f),

    // обучение
    DOOR_LOCK(R.raw.sfx_door_lock),
    CUBE(R.raw.sfx_cube),

    // гостиная
    PICTURE(R.raw.sfx_picture),
    PILLOW(R.raw.sfx_pillow),
    CARPET(R.raw.sfx_carpet, 0.6f),
    WATERING(R.raw.sfx_watering, 0.7f),

    // ванная
    BRUSH(R.raw.sfx_brush),
    SINK(R.raw.sfx_sink, 0.6f),
    TOWEL(R.raw.sfx_towel),
    MIRROR(R.raw.sfx_mirror_squeak, 0.6f),
    CLOTHESPIN(R.raw.sfx_clothespin),
    CLOTHES(R.raw.sfx_clothes),
    POWDER(R.raw.sfx_powder),
    WASHER_DOOR(R.raw.sfx_washer_door),
    WASHER(R.raw.sfx_washer, 0.7f),

    // кухня
    TRASH_BAG(R.raw.sfx_trash_bag),
    PLATE(R.raw.sfx_plate),
    KETTLE(R.raw.sfx_kettle_boil, 0.6f),
    POUR(R.raw.sfx_pour, 0.7f),
    TEABAG(R.raw.sfx_teabag),
    CHOP(R.raw.sfx_chop),
    WHISK(R.raw.sfx_whisk, 0.6f),
    FRY(R.raw.sfx_fry, 0.6f),

    // питомец
    CAT(R.raw.sfx_cat, 0.6f),
    DOG(R.raw.sfx_dog, 0.5f),
    PIG(R.raw.sfx_pig, 0.6f),
    SQUEAKY_TOY(R.raw.sfx_squeaky_toy, 0.7f),
    PET_FOOD(R.raw.sfx_pet_food),
    CRUNCH(R.raw.sfx_crunch, 0.7f)
}

/**
 * Звуки заданий: что звучит на шагах и когда предмет меняется на «после»
 */
object MissionSounds {

    /**
     * Звук после шага задания (номер шага с нуля), null - шаг без звука
     */
    fun step(kind: MissionKind, step: Int): Sfx? = when (kind) {
        MissionKind.CUBES -> if (step % 3 == 2) Sfx.CUBE else null              // положили кубик
        MissionKind.PILLOWS -> if (step % 2 == 1) Sfx.PILLOW else null          // положили подушку
        MissionKind.TABLE -> if (step % 2 == 1) Sfx.PLATE else null             // положили предмет
        MissionKind.CLOTHESPINS -> if (step % 2 == 1) Sfx.CLOTHESPIN else null  // прикрепили прищепку
        MissionKind.FLOWER -> if (step == 1) Sfx.WATERING else null             // наклонили лейку
        MissionKind.TOOTHBRUSH -> if (step == 2) Sfx.BRUSH else null            // почистили зубы
        MissionKind.TEA -> when (step) {
            1 -> Sfx.KETTLE     // подставили под чайник
            2 -> Sfx.POUR       // налили
            3 -> Sfx.TEABAG     // опустили пакетик
            else -> null
        }
        MissionKind.LAUNDRY -> when {
            step < 6 -> if (step % 2 == 1) Sfx.CLOTHES else null  // положили вещь в барабан
            step == 6 -> Sfx.POWDER
            step == 7 -> Sfx.WASHER_DOOR
            else -> null
        }
        MissionKind.PANCAKES -> when (step) {
            0 -> Sfx.WHISK      // взяли венчик, замешиваем
            9 -> Sfx.FRY        // тесто на сковороде
            else -> null
        }
        MissionKind.PET_TOY -> if (step % 2 == 1) Sfx.SQUEAKY_TOY else null   // бросили игрушку
        MissionKind.PET_FOOD -> if (step == 1) Sfx.PET_FOOD else null         // насыпали корм
        else -> null
    }

    /**
     * Звук, когда предмет в комнате меняется на «после» (экран смены предмета)
     */
    fun done(kind: MissionKind, pet: PetKind?): Sfx? = when (kind) {
        MissionKind.DOOR -> Sfx.DOOR_LOCK
        MissionKind.PICTURE -> Sfx.PICTURE
        MissionKind.VACUUM -> Sfx.CARPET
        MissionKind.TOOTHBRUSH -> Sfx.SINK
        MissionKind.TOWEL -> Sfx.TOWEL
        MissionKind.MIRROR -> Sfx.MIRROR
        MissionKind.LAUNDRY -> Sfx.WASHER
        MissionKind.TRASH -> Sfx.TRASH_BAG
        MissionKind.PET_FOOD -> Sfx.CRUNCH
        MissionKind.PET_CHOICE, MissionKind.PET_TOY, MissionKind.PET_BOWL -> voice(pet)
        else -> null
    }

    fun voice(pet: PetKind?): Sfx? = when (pet) {
        PetKind.CAT -> Sfx.CAT
        PetKind.DOG -> Sfx.DOG
        PetKind.PIG -> Sfx.PIG
        null -> null
    }
}

/**
 * Звук игры: короткие эффекты через SoundPool и тихая фоновая музыка через MediaPlayer
 * Один на приложение: init в MainActivity.onCreate, release в onDestroy
 * Настройки (музыка, эффекты) хранит MainActivity и передаёт сюда
 */
object Sounds {
    private const val MAX_STREAMS = 6
    private const val MUSIC_VOLUME = 0.3f

    // фоновая музыка по кругу
    private val PLAYLIST = listOf(R.raw.music_clouds, R.raw.music_sheep, R.raw.music_moon, R.raw.music_quiet)

    private var context: Context? = null
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Sfx, Int>()
    private val loaded = mutableSetOf<Int>()
    // попросили сыграть, пока звук ещё загружался (например, приветствие при запуске)
    private val pending = mutableSetOf<Int>()
    // звук действия в задании: новый шаг глушит прежний, чтобы звуки не наслаивались
    private var actionStream = 0

    private var player: MediaPlayer? = null
    private var track = 0
    private var started = false   // приложение на экране

    var effectsOn = true
    var musicOn = true
        set(value) {
            field = value
            if (value) resumeMusic() else stopMusic()
        }

    fun init(context: Context) {
        if (pool != null) return
        this.context = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build().apply {
            setOnLoadCompleteListener { p, id, status ->
                if (status != 0) return@setOnLoadCompleteListener
                loaded += id
                if (pending.remove(id)) {
                    val sfx = ids.entries.firstOrNull { it.value == id }?.key
                    if (sfx != null && effectsOn) p.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
                }
            }
        }
        Sfx.entries.forEach { sfx -> ids[sfx] = pool!!.load(this.context, sfx.res, 1) }
    }

    /**
     * Короткий эффект
     * @param action - звук действия в задании: глушит предыдущий звук действия
     */
    fun play(sfx: Sfx?, action: Boolean = false) {
        if (sfx == null || !effectsOn) return
        val p = pool ?: return
        val id = ids[sfx] ?: return
        if (id !in loaded) {
            pending += id
            return
        }
        if (action && actionStream != 0) p.stop(actionStream)
        val stream = p.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
        if (action) actionStream = stream
    }

    // заглушить звук действия (ушли с экрана задания)
    fun stopAction() {
        if (actionStream != 0) pool?.stop(actionStream)
        actionStream = 0
    }

    // приложение показалось на экране
    fun onStart() {
        started = true
        resumeMusic()
    }

    // приложение ушло в фон: музыка на паузу
    fun onStop() {
        started = false
        player?.takeIf { it.isPlaying }?.pause()
    }

    private fun resumeMusic() {
        if (!musicOn || !started) return
        val current = player
        if (current != null) {
            if (!current.isPlaying) current.start()
            return
        }
        startTrack(track)
    }

    private fun startTrack(index: Int) {
        val ctx = context ?: return
        track = index % PLAYLIST.size
        player = MediaPlayer.create(ctx, PLAYLIST[track])?.apply {
            setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            setOnCompletionListener {
                it.release()
                player = null
                if (musicOn && started) startTrack(track + 1)
            }
            start()
        }
    }

    private fun stopMusic() {
        player?.release()
        player = null
    }

    fun release() {
        stopMusic()
        pool?.release()
        pool = null
        ids.clear()
        loaded.clear()
        pending.clear()
        context = null
    }
}
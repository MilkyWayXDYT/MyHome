package com.robotpaw.myhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.developer.ComponentGallery
import com.robotpaw.myhome.developer.EmulatorPads
import com.robotpaw.myhome.developer.LevelSwitcher
import com.robotpaw.myhome.developer.withAllCompleted
import com.robotpaw.myhome.developer.withCompletedBefore
import com.robotpaw.myhome.developer.SignalOverlay
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.lifecycleScope
import com.robotpaw.myhome.calibration.CalibrationScreen
import com.robotpaw.myhome.emulator.EmulatorSignalSource
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionResult
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.RecordOutcome
import com.robotpaw.myhome.game.StarsOutcome
import com.robotpaw.myhome.game.toStats
import com.robotpaw.myhome.game.save.ProgressRepository
import com.robotpaw.myhome.game.save.SharedPreferencesStorage
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.menu.RoomScreen
import com.robotpaw.myhome.developer.SignalCheckScreen
import com.robotpaw.myhome.menu.GoalsScreen
import com.robotpaw.myhome.menu.MainMenuChoice
import com.robotpaw.myhome.menu.MainMenuScreen
import com.robotpaw.myhome.menu.RoomsScreen
import com.robotpaw.myhome.mission.InstructionScreen
import com.robotpaw.myhome.mission.MissionScreen
import com.robotpaw.myhome.mission.PauseChoice
import com.robotpaw.myhome.mission.PauseScreen
import com.robotpaw.myhome.mission.RestScreen
import com.robotpaw.myhome.mission.ResultChoice
import com.robotpaw.myhome.mission.ResultScreen
import com.robotpaw.myhome.mission.RoomScenes
import com.robotpaw.myhome.mission.SceneChangeScreen
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val emulator by lazy { EmulatorSignalSource(lifecycleScope) }

    private var isDebug = false

    private var gamepadTriggerListener: ((Int, Int) -> Unit)? = null

    fun setGamepadTriggerListener(listener: ((Int, Int) -> Unit)?) {
        gamepadTriggerListener = listener
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isGamepad = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
                event.isFromSource(InputDevice.SOURCE_GAMEPAD)

        if (event.action == MotionEvent.ACTION_MOVE && isGamepad) {
            val flexorValue = getTriggerValue(event, MotionEvent.AXIS_LTRIGGER)
            val extensorValue = getTriggerValue(event, MotionEvent.AXIS_RTRIGGER)

            gamepadTriggerListener?.invoke(
                (flexorValue * 100).roundToInt(),
                (extensorValue * 100).roundToInt()
            )

            if (gamepadTriggerListener != null) return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    // Клавиатура (как и геймпад — только в debug): F — сгибатель, J — разгибатель, с Shift — сильнее.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isDebug) return super.dispatchKeyEvent(event)

        val channel = when (event.keyCode) {
            KeyEvent.KEYCODE_F -> Channel.FLEXOR
            KeyEvent.KEYCODE_J -> Channel.EXTENSOR
            else -> return super.dispatchKeyEvent(event)
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> emulator.setChannel(channel, if (event.isShiftPressed) 100 else 60)
            KeyEvent.ACTION_UP -> emulator.setChannel(channel, 0)
        }
        return true
    }

    private fun getTriggerValue(event: MotionEvent, axis: Int): Float {
        val device = event.device ?: return 0f
        val range = device.getMotionRange(axis, event.source)
            ?: device.getMotionRange(axis)
            ?: return event.getAxisValue(axis).coerceIn(0f, 1f)

        val value = event.getAxisValue(axis)
        val span = range.max - range.min

        if (span <= 0f) return 0f

        return ((value - range.min) / span).coerceIn(0f, 1f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDebug = (applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0

        emulator.start()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyHomeApp(emulator = emulator, isDebug = isDebug)
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Calibration : Screen
    data object MainMenu : Screen
    data object Goals : Screen
    data object SignalCheck : Screen
    data object Gallery : Screen
    data object RoomList : Screen
    data class Room(val roomId: String, val wall: Int = 0) : Screen
    data class Rest(val roomId: String) : Screen
    data class Instruction(val mission: Mission) : Screen
    data class Play(val mission: Mission, val checkpoint: MissionCheckpoint = MissionCheckpoint()) : Screen
    data class Pause(val mission: Mission, val checkpoint: MissionCheckpoint) : Screen
    // roomCompleted - этим заданием пройдена вся комната
    data class Result(val result: MissionResult, val roomCompleted: Boolean, val record: RecordOutcome, val stars: StarsOutcome) : Screen
    // предмет в комнате меняется на «после», затем экран результата next
    data class Change(val next: Result) : Screen
}

private fun roomScreenOf(mission: Mission): Screen.Room {
    val room = Rooms.of(mission)
    return Screen.Room(room.id, room.missions.indexOf(mission))
}

@Composable
fun MyHomeApp(emulator: EmulatorSignalSource, isDebug: Boolean) {

    // Калибровка не сохраняется: при каждом запуске игрок проходит её заново.
    var screen by remember { mutableStateOf<Screen>(Screen.Calibration) }

    // Прогресс (пройденные задания, питомец) хранится между запусками
    val context = LocalContext.current
    val repository = remember {
        ProgressRepository(SharedPreferencesStorage(context.getSharedPreferences("myhome_save", Context.MODE_PRIVATE)))
    }
    var progress by remember { mutableStateOf(repository.load()) }
    LaunchedEffect(progress) { repository.save(progress) }
    var profile by remember { mutableStateOf(SignalProfile()) }

    var overlayExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    // Код состояния и задержка — для панели эмулятора.
    var stateCode by remember { mutableStateOf("00") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    val processor = remember(profile) { SignalProcessor(profile) }

    LaunchedEffect(emulator, processor) {
        emulator.frames.collect { frame ->
            stateCode = processor.process(frame).code
            withFrameNanos {
                latencyMs = System.nanoTime() / 1_000_000 - frame.timestampMs
            }
        }
    }

    val activity = LocalContext.current as? MainActivity

    SideEffect {
        if (isDebug) {
            activity?.setGamepadTriggerListener { l2, r2 ->
                emulator.setRaw(l2, r2)
            }
        } else {
            activity?.setGamepadTriggerListener(null)
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.setGamepadTriggerListener(null)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Экраны
        when (val current = screen) {
            Screen.Calibration -> CalibrationScreen(
                source = emulator,
                onFinished = {
                    profile = it
                    screen = Screen.MainMenu
                }
            )

            Screen.MainMenu -> MainMenuScreen(
                source = emulator,
                profile = profile,
                progress = progress,
                showDeveloperItems = isDebug,
                onChoice = { choice ->
                    screen = when (choice) {
                        MainMenuChoice.CONTINUE -> Screen.Room(progress.currentRoom().id)
                        MainMenuChoice.ROOMS -> Screen.RoomList
                        MainMenuChoice.GOALS -> Screen.Goals
                        MainMenuChoice.SIGNAL_CHECK -> Screen.SignalCheck
                        MainMenuChoice.RECALIBRATE -> Screen.Calibration
                        MainMenuChoice.GALLERY -> Screen.Gallery
                    }
                }
            )

            Screen.Goals -> GoalsScreen(
                source = emulator,
                profile = profile,
                onBack = { screen = Screen.MainMenu }
            )

            Screen.Gallery -> ComponentGallery(
                source = emulator,
                profile = profile,
                onBack = { screen = Screen.MainMenu }
            )

            Screen.SignalCheck -> SignalCheckScreen(
                source = emulator,
                profile = profile,
                onExit = { screen = Screen.MainMenu }
            )

            Screen.RoomList -> RoomsScreen(
                source = emulator,
                profile = profile,
                progress = progress,
                onOpen = { screen = Screen.Room(it.id) },
                onBack = { screen = Screen.MainMenu }
            )

            is Screen.Room -> key(current.roomId, current.wall) { RoomScreen(
                source = emulator,
                profile = profile,
                room = Rooms.byId(current.roomId),
                progress = progress,
                initialWall = current.wall,
                onStart = { screen = Screen.Instruction(it) },
                onBack = { screen = Screen.MainMenu }
            ) }

            is Screen.Instruction -> InstructionScreen(
                source = emulator,
                profile = profile,
                mission = current.mission,
                onStart = { screen = Screen.Play(current.mission) },
                onBack = { screen = roomScreenOf(current.mission) }
            )

            is Screen.Play -> key(current.mission.id) { MissionScreen(
                source = emulator,
                profile = profile,
                roomTitle = Rooms.of(current.mission).title,
                mission = current.mission,
                checkpoint = current.checkpoint,
                onDone = { result ->
                    val room = Rooms.of(current.mission)
                    val wasDone = progress.isDone(room)
                    val record = RecordOutcome.of(result, progress.recordOf(current.mission))
                    val stars = progress.starsFor(result)
                    progress = progress.withResult(result)
                    val resultScreen = Screen.Result(result, roomCompleted = !wasDone && progress.isDone(room), record = record, stars = stars)
                    screen = if (RoomScenes.of(current.mission.kind) != null) Screen.Change(resultScreen) else resultScreen
                },
                onPause = { checkpoint -> screen = Screen.Pause(current.mission, checkpoint) },
                pet = progress.pet ?: PetKind.CAT,
                onPetChosen = { progress = progress.withPet(it) }
            ) }

            is Screen.Pause -> PauseScreen(
                source = emulator,
                profile = profile,
                onChoice = { choice ->
                    screen = when (choice) {
                        PauseChoice.CONTINUE -> Screen.Play(current.mission, current.checkpoint)
                        PauseChoice.MENU -> Screen.MainMenu
                    }
                }
            )

            is Screen.Change -> key(current.next.result.mission.id) {
                val mission = current.next.result.mission
                val room = Rooms.of(mission)
                SceneChangeScreen(
                    mission = mission,
                    room = room,
                    isDone = { kind -> room.missions.any { it.kind == kind && progress.isCompleted(it) } },
                    onFinished = { screen = current.next }
                )
            }

            is Screen.Result -> ResultScreen(
                source = emulator,
                profile = profile,
                result = current.result,
                stats = current.result.log.toStats(),
                record = current.record,
                stars = current.stars,
                onChoice = { choice ->
                    screen = when (choice) {
                        ResultChoice.ROOM -> {
                            val room = Rooms.of(current.result.mission)
                            val next = Rooms.next(room)
                            when {
                                !current.roomCompleted -> roomScreenOf(current.result.mission)
                                room.restAfter -> Screen.Rest(room.id)
                                next != null -> Screen.Room(next.id)   // после обучения сразу в гостиную
                                else -> roomScreenOf(current.result.mission)
                            }
                        }
                        ResultChoice.AGAIN -> Screen.Play(current.result.mission)
                    }
                }
            )

            is Screen.Rest -> {
                val room = Rooms.byId(current.roomId)
                val next = Rooms.next(room)
                RestScreen(
                    source = emulator,
                    profile = profile,
                    roomTitle = room.title,
                    nextRoomTitle = next?.title,
                    summary = progress.summaryOf(room),
                    onNext = { screen = if (next != null) Screen.Room(next.id) else Screen.MainMenu }
                )
            }
        }

        // Эмулятор доступен только в debug-сборке.
        // Свёрнут — маленькие кнопки С / Р / С+Р у правого края, развёрнут — большая панель.
        if (isDebug && !overlayExpanded) {
            EmulatorPads(
                emulator = emulator,
                onOpenSettings = { overlayExpanded = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }

        // Быстрый переход по комнатам и заданиям (только debug)
        if (isDebug) {
            LevelSwitcher(
                progress = progress,
                onJumpToMission = { screen = Screen.Play(it) },
                onJumpToRoom = { room ->
                    progress = progress.withCompletedBefore(room)
                    screen = Screen.Room(room.id)
                },
                onUnlockAll = { progress = progress.withAllCompleted() },
                onReset = { progress = GameProgress() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(8.dp)
            )
        }

        if (isDebug && overlayExpanded) {

            SignalOverlay(
                expanded = overlayExpanded,

                onExpandedChange = {
                    overlayExpanded = it
                },

                emulator = emulator,
                stateCode = stateCode,
                latencyMs = latencyMs,

                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
            )
        }
    }
}

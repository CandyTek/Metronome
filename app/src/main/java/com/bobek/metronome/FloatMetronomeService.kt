package com.bobek.metronome

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bobek.metronome.data.Beats
import com.bobek.metronome.data.Gaps
import com.bobek.metronome.data.Sound
import com.bobek.metronome.data.Subdivisions
import com.bobek.metronome.data.Tempo
import com.bobek.metronome.data.Tick
import com.bobek.metronome.data.TickType
import com.bobek.metronome.databinding.FloatviewMetronomeBinding
import com.bobek.metronome.databinding.FragmentMetronomeBinding
import com.bobek.metronome.domain.Metronome
import com.bobek.metronome.view.TouchLinearLayout.OnTouchCallbackListener
import com.bobek.metronome.view.component.TickVisualization
import com.bobek.metronome.view.model.MetronomeViewModel

/** [R.layout.content_metronome_float] */
class FloatMetronomeService : LifecycleService(), ViewModelStoreOwner {
	override val viewModelStore = ViewModelStore()
	// fun getViewModelStore(): ViewModelStore = viewModelStore

	private var _binding: FloatviewMetronomeBinding? = null
	private val binding get() = _binding!!
	private lateinit var windowManager: WindowManager
	private lateinit var floatView: View
	private lateinit var params: WindowManager.LayoutParams
	private val tickReceiver = TickReceiver()
	
	// private val viewModel: MetronomeViewModel by viewModels()
	
	// Manually create the ViewModel using a ViewModelProvider
	private val viewModel: MetronomeViewModel by lazy {
		ViewModelProvider(this).get(MetronomeViewModel::class.java)
	}

	override fun onCreate() {
		super.onCreate()

		// 1. 设置悬浮窗 Layout
		windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
		_binding = FloatviewMetronomeBinding.inflate(LayoutInflater.from(this))

		binding.lifecycleOwner = this
		binding.metronome = viewModel

		floatView = binding.root

		// 2. 添加悬浮窗到 WindowManager
		params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
			else
				WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT
		)
		params.gravity = Gravity.TOP or Gravity.START
		params.x = 0
		params.y = 300
		if (VERSION.SDK_INT >= VERSION_CODES.P) {
			params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
		}


		windowManager.addView(floatView, params)

		// 3. 继续使用 LiveData
		viewModel.tempoData.observe(this) { tempo ->
			// 自动绑定 UI 已可，若需手动处理，也可写
		}

		// 4. 悬浮窗拖动处理
		// floatView.setOnTouchListener(...)

		initViewModel()
		initBinding()
		adjustLayoutToSystemBars()
		registerTickReceiver()
		setupMenu()
		

		metronome = Metronome(this, lifecycle) { publishTick(it) }
		metronome?.beats = beats
		metronome?.subdivisions = subdivisions
		metronome?.gaps = gaps
		metronome?.tempo = tempo
		metronome?.emphasizeFirstBeat = emphasizeFirstBeat
		metronome?.sound = sound

		viewModel.beatsData.observe(this) { beats = it }
		viewModel.subdivisionsData.observe(this) { subdivisions = it }
		viewModel.gapsData.observe(this) { gaps = it }
		viewModel.tempoData.observe(this) { tempo = it }
		viewModel.emphasizeFirstBeat.observe(this) { emphasizeFirstBeat = it }
		viewModel.sound.observe(this) { sound = it }
		viewModel.playing.observe(this) { playing = it }

		viewModel.subdivisionsData.observe(this) { subdivisionsData ->
				binding.content.subdivisionsSlider.setProgress(subdivisionsData.value)
		}
		viewModel.tempoData.observe(this) { tempoData ->
				binding.content.tempoSlider.setProgress(tempoData.value)
		}

		binding.btnQuit.setOnClickListener { 
			stopSelf()
		}
		binding.linTouchMoveBar.setOnTouchCallbackListener(mOnTouchCallbackListener);
	}

	/** 更新悬浮窗的位置，x、y为基于当前的偏移量  */
	private val mOnTouchCallbackListener: OnTouchCallbackListener = object : OnTouchCallbackListener {
		public override fun onTouchCallback(x: kotlin.Float, y: kotlin.Float) {
			params.y = (params.y + y).toInt()
			// wmParams.x += x;
			windowManager.updateViewLayout(floatView, params)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		unregisterTickReceiver()

		windowManager.removeView(floatView)
		// Clear the ViewModelStore when the service is destroyed
		viewModelStore.clear()
		_binding = null

		Log.d(TAG, "Lifecycle: onDestroy")
		playing = false
		metronome = null

	}


	private var lastTap: Long = 0


	

	private fun initViewModel() {
		viewModel.playing.observe(this) { playing ->
			if (playing) keepScreenOn() else doNotKeepScreenOn()
		}
	}

	private fun keepScreenOn() {
		getKeepScreenOnView().keepScreenOn = true
	}

	private fun doNotKeepScreenOn() {
		getKeepScreenOnView().keepScreenOn = false
	}

	private fun getKeepScreenOnView() = requireBinding().content.startStopButton

	@SuppressLint("ClickableViewAccessibility")
	private fun initBinding() {
		val binding = requireBinding()
		// binding.lifecycleOwner = viewLifecycleOwner
		binding.lifecycleOwner = this
		binding.metronome = viewModel

		binding.content.incrementTempoButton.setOnClickListener { incrementTempo() }
		binding.content.incrementTempoButton.setOnLongClickListener {
			repeat(LARGE_TEMPO_CHANGE_SIZE) { incrementTempo() }
			true
		}

		binding.content.decrementTempoButton.setOnClickListener { decrementTempo() }
		binding.content.decrementTempoButton.setOnLongClickListener {
			repeat(LARGE_TEMPO_CHANGE_SIZE) { decrementTempo() }
			true
		}

		binding.content.tapTempoButton.setOnClickListener { tapTempo() }
		binding.content.tapTempoButton.setOnTouchListener { view, event -> tapTempoOnTouchListener(view, event) }
	}

	private fun incrementTempo() {
		viewModel.tempoData.value?.value?.let {
			if (it < Tempo.MAX) {
				viewModel.tempoData.value = Tempo(it + 1)
			}
		}
	}

	private fun decrementTempo() {
		viewModel.tempoData.value?.value?.let {
			if (it > Tempo.MIN) {
				viewModel.tempoData.value = Tempo(it - 1)
			}
		}
	}

	private fun tapTempo() {
		val currentTime = System.currentTimeMillis()
		val tempoValue = calculateTapTempo(lastTap, currentTime)

		if (tempoValue > Tempo.MAX) {
			viewModel.tempoData.value = Tempo(Tempo.MAX)
		} else if (tempoValue >= Tempo.MIN) {
			viewModel.tempoData.value = Tempo(tempoValue)
		}

		lastTap = currentTime
	}

	private fun calculateTapTempo(firstTap: Long, secondTap: Long): Int = (60_000 / (secondTap - firstTap)).toInt()

	private fun tapTempoOnTouchListener(view: View, event: MotionEvent): Boolean {
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				view.isPressed = true
				view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
				view.performClick()
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				view.isPressed = false
			}
		}

		return true
	}

	private fun adjustLayoutToSystemBars() {
		ViewCompat.setOnApplyWindowInsetsListener(requireBinding().content.root) { view, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				leftMargin = insets.left
				rightMargin = insets.right
				bottomMargin = insets.bottom
			}
			WindowInsetsCompat.CONSUMED
		}
	}

	private fun registerTickReceiver() {
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(tickReceiver, IntentFilter(MetronomeService.ACTION_TICK))
		Log.d(TAG, "Registered tickReceiver")
	}

	private fun setupMenu() {
		// requireActivity().addMenuProvider(getMenuProvider(), this, Lifecycle.State.RESUMED)
	}

	private fun getMenuProvider() = object : MenuProvider {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.menu_metronome, menu)
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
			return when (menuItem.itemId) {
				R.id.action_settings -> {
					showSettings()
					true
				}

				else -> false
			}
		}
	}

	private fun showSettings() {
		// findNavController().navigate(R.id.action_MetronomeFragment_to_SettingsFragment)
	}

	

	private fun unregisterTickReceiver() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(tickReceiver)
		Log.d(TAG, "Unregistered tickReceiver")
	}

	inner class TickReceiver : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			extractTick(intent)
				?.also { tick -> Log.v(TAG, "Received $tick") }
				?.also { tick -> visualizeTick(tick) }
		}
	}

	private fun extractTick(intent: Intent): Tick? {
		return if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(MetronomeService.EXTRA_TICK, Tick::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(MetronomeService.EXTRA_TICK)
		}
	}

	private fun visualizeTick(tick: Tick) {
		if (tick.type == TickType.STRONG || tick.type == TickType.WEAK) {
			getTickVisualization(tick.beat)?.blink()
		}
	}

	private fun getTickVisualization(beat: Int): TickVisualization? = when (beat) {
		1 -> requireBinding().content.tickVisualization1
		2 -> requireBinding().content.tickVisualization2
		3 -> requireBinding().content.tickVisualization3
		4 -> requireBinding().content.tickVisualization4
		5 -> requireBinding().content.tickVisualization5
		6 -> requireBinding().content.tickVisualization6
		7 -> requireBinding().content.tickVisualization7
		8 -> requireBinding().content.tickVisualization8
		else -> null
	}

	private fun requireBinding(): FloatviewMetronomeBinding = binding!!
	
	companion object{
		private val TAG = FloatMetronomeService::class.java.simpleName
		const val ACTION_STOP = "com.bobek.metronome.intent.action.STOP"
		const val ACTION_REFRESH = "com.bobek.metronome.intent.action.REFRESH"
		const val ACTION_TICK = "com.bobek.metronome.intent.action.TICK"
		const val EXTRA_TICK = "com.bobek.metronome.intent.extra.TICK"

	}

    private var metronome: Metronome? = null

    var beats: Beats = Beats()
        get() = metronome?.beats ?: field
        set(beats) {
            field = beats
            metronome?.beats = beats
        }

    var subdivisions: Subdivisions = Subdivisions()
        get() = metronome?.subdivisions ?: field
        set(subdivisions) {
            field = subdivisions
            metronome?.subdivisions = subdivisions
        }

    var gaps: Gaps = Gaps()
        get() = metronome?.gaps ?: field
        set(gaps) {
            field = gaps
            metronome?.gaps = gaps
        }

    var tempo: Tempo = Tempo()
        get() = metronome?.tempo ?: field
        set(tempo) {
            field = tempo
            metronome?.tempo = tempo
        }

    var emphasizeFirstBeat: Boolean = true
        get() = metronome?.emphasizeFirstBeat ?: field
        set(emphasizeFirstBeat) {
            field = emphasizeFirstBeat
            metronome?.emphasizeFirstBeat = emphasizeFirstBeat
        }

    var sound: Sound = Sound.SQUARE_WAVE
        get() = metronome?.sound ?: field
        set(sound) {
            field = sound
            metronome?.sound = sound
        }

    var playing: Boolean
        get() = metronome?.playing == true
        set(playing) = if (playing) startMetronome() else stopMetronome()



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Lifecycle: onStartCommand")
        if (intent?.action == ACTION_STOP) {
            performStop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "Lifecycle: onBind")
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Lifecycle: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "Lifecycle: onRebind")
    }


    private fun startMetronome() {
        Log.e(TAG, "Start metronome")
        metronome?.playing = true
    }



    private fun stopMetronome() {
        Log.i(TAG, "Stop metronome")
        metronome?.playing = false
    }

   

    private fun publishTick(tick: Tick) {
        Intent(this, MetronomeFragment.TickReceiver::class.java)
            .apply { action = ACTION_TICK }
            .apply { putExtra(EXTRA_TICK, tick) }
            .let { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
    }

    private fun performStop() {
        Log.d(TAG, "Received stop command")
        playing = false
        Intent(this, MainActivity.RefreshReceiver::class.java)
            .apply { action = ACTION_REFRESH }
            .let { LocalBroadcastManager.getInstance(this).sendBroadcast(it) }
    }


    inner class LocalBinder : Binder() {
        fun getService(): FloatMetronomeService = this@FloatMetronomeService
    }


}

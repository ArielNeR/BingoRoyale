package com.bingoroyale.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bingoroyale.R
import com.bingoroyale.databinding.FragmentCallerBinding
import com.bingoroyale.model.BingoCard
import com.bingoroyale.ui.adapters.BallHistoryAdapter
import com.bingoroyale.viewmodel.CallerViewModel

class CallerFragment : Fragment() {

    private var _binding: FragmentCallerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CallerViewModel by viewModels()
    private lateinit var historyAdapter: BallHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            showExitConfirmation()
        }

        binding.btnNextBall.setOnClickListener {
            viewModel.drawNextBall()
            vibrate()
        }

        binding.btnNewGame.setOnClickListener {
            showNewGameConfirmation()
        }

        binding.btnToggleNetwork.setOnClickListener {
            viewModel.toggleNetwork()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = BallHistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = GridLayoutManager(requireContext(), 8)
            adapter = historyAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            // Actualizar contador
            binding.tvBallsRemaining.text = getString(
                R.string.balls_remaining,
                state.ballsRemaining
            )

            // Actualizar historial
            historyAdapter.submitList(state.drawnBalls.reversed())

            // Estado de red
            if (state.isNetworkActive) {
                binding.tvNetworkStatus.text = getString(R.string.network_active)
                binding.tvNetworkStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_green)
                )
                binding.btnToggleNetwork.text = "📴 Desactivar"

                binding.tvConnectedCount.visibility = View.VISIBLE
                binding.tvConnectedCount.text = "👥 ${state.connectedPlayers}"
            } else {
                binding.tvNetworkStatus.text = getString(R.string.network_inactive)
                binding.tvNetworkStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
                binding.btnToggleNetwork.text = "🌐 Red Local"
                binding.tvConnectedCount.visibility = View.GONE
            }

            // Desactivar botón si no hay más bolas
            binding.btnNextBall.isEnabled = !state.isGameFinished
            binding.btnNextBall.alpha = if (state.isGameFinished) 0.5f else 1f
        }

        viewModel.lastDrawnBall.observe(viewLifecycleOwner) { ball ->
            updateCurrentBall(ball)
            updateLastBalls()
        }
    }

    private fun updateCurrentBall(ball: Int?) {
        if (ball == null) {
            binding.tvBallNumber.text = "?"
            binding.tvBallLetter.text = ""
            binding.ballBackground.setBackgroundResource(R.drawable.bg_ball_empty)
            return
        }

        val letter = BingoCard.getLetterForNumber(ball)
        binding.tvBallLetter.text = letter
        binding.tvBallNumber.text = ball.toString()

        // Background según letra
        val bgRes = when (letter) {
            "B" -> R.drawable.bg_ball_b
            "I" -> R.drawable.bg_ball_i
            "N" -> R.drawable.bg_ball_n
            "G" -> R.drawable.bg_ball_g
            "O" -> R.drawable.bg_ball_o
            else -> R.drawable.bg_ball_empty
        }
        binding.ballBackground.setBackgroundResource(bgRes)

        // Animación
        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.ball_pop)
        binding.ballContainer.startAnimation(bounceAnim)
    }

    private fun updateLastBalls() {
        val drawnBalls = viewModel.gameState.value?.drawnBalls ?: return
        val lastFive = drawnBalls.takeLast(5).reversed().drop(1) // Sin la actual

        binding.lastBallsContainer.removeAllViews()

        if (lastFive.isEmpty()) {
            val textView = TextView(requireContext()).apply {
                text = "Aparecerán aquí"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                textSize = 12f
            }
            binding.lastBallsContainer.addView(textView)
            return
        }

        lastFive.forEachIndexed { index, ball ->
            val ballView = createSmallBallView(ball, index)
            binding.lastBallsContainer.addView(ballView)
        }
    }

    private fun createSmallBallView(ball: Int, index: Int): View {
        val size = resources.getDimensionPixelSize(R.dimen.ball_size_small)
        val margin = 4

        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
            }
            alpha = 1f - (index * 0.15f)
        }

        val background = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val letter = BingoCard.getLetterForNumber(ball)
            val bgRes = when (letter) {
                "B" -> R.drawable.bg_ball_b
                "I" -> R.drawable.bg_ball_i
                "N" -> R.drawable.bg_ball_n
                "G" -> R.drawable.bg_ball_g
                "O" -> R.drawable.bg_ball_o
                else -> R.drawable.bg_ball_empty
            }
            setBackgroundResource(bgRes)
        }

        val textView = TextView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            text = ball.toString()
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        container.addView(background)
        container.addView(textView)

        return container
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = requireContext().getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Vibrator::class.java)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    private fun showNewGameConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🔄 Nueva Partida")
            .setMessage("¿Iniciar una nueva partida? Se reiniciarán todas las bolas.")
            .setPositiveButton("Iniciar") { _, _ ->
                viewModel.startNewGame()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🚪 Salir")
            .setMessage("¿Deseas salir del modo cantador?")
            .setPositiveButton("Salir") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
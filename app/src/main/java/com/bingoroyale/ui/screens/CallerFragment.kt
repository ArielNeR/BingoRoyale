package com.bingoroyale.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        binding.btnBack.setOnClickListener { showExitDialog() }

        binding.btnNextBall.setOnClickListener {
            viewModel.drawNextBall()
            vibrate(50)
        }

        binding.btnNewGame.setOnClickListener { showNewGameDialog() }

        binding.btnToggleNetwork.setOnClickListener {
            viewModel.toggleNetwork()

            // Mostrar IP cuando se activa
            if (viewModel.gameState.value?.isNetworkActive == false) {
                val ip = viewModel.getServerIp()
                Toast.makeText(requireContext(), "📡 IP: $ip", Toast.LENGTH_LONG).show()
            }
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
            binding.tvBallsRemaining.text = getString(R.string.balls_remaining, state.ballsRemaining)
            historyAdapter.submitList(state.drawnBalls.reversed())

            // Network status
            val isNetworkActive = state.isNetworkActive
            binding.tvNetworkStatus.text = if (isNetworkActive)
                getString(R.string.network_active) else getString(R.string.network_inactive)
            binding.tvNetworkStatus.setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (isNetworkActive) R.color.accent_green else R.color.text_secondary)
            )
            binding.btnToggleNetwork.text = if (isNetworkActive) "📴 Desactivar" else "🌐 Red Local"

            // Connected players
            binding.tvConnectedCount.visibility = if (isNetworkActive) View.VISIBLE else View.GONE
            binding.tvConnectedCount.text = "👥 ${state.connectedPlayers}"

            // Disable button when no balls left
            binding.btnNextBall.isEnabled = !state.isGameFinished
            if (state.isGameFinished) {
                binding.btnNextBall.text = "✅ PARTIDA TERMINADA"
            } else {
                binding.btnNextBall.text = "🎱  SIGUIENTE NÚMERO"
            }
        }

        viewModel.lastDrawnBall.observe(viewLifecycleOwner) { ball ->
            updateCurrentBall(ball)
        }

        // Notificación cuando alguien canta BINGO
        viewModel.bingoNotification.observe(viewLifecycleOwner) { playerName ->
            if (playerName != null) {
                showBingoNotification(playerName)
                viewModel.clearBingoNotification()
            }
        }
    }

    private fun showBingoNotification(playerName: String) {
        vibrate(longArrayOf(0, 200, 100, 200, 100, 400))

        AlertDialog.Builder(requireContext(), R.style.BingoDialogTheme)
            .setTitle("🎉 ¡BINGO!")
            .setMessage("$playerName ha cantado BINGO.\n\n¿Verificar y confirmar?")
            .setPositiveButton("✅ Confirmar") { _, _ ->
                Toast.makeText(requireContext(), "BINGO confirmado para $playerName", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("❌ Rechazar") { _, _ ->
                Toast.makeText(requireContext(), "BINGO rechazado", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
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

        val bgRes = when (letter) {
            "B" -> R.drawable.bg_ball_b
            "I" -> R.drawable.bg_ball_i
            "N" -> R.drawable.bg_ball_n
            "G" -> R.drawable.bg_ball_g
            "O" -> R.drawable.bg_ball_o
            else -> R.drawable.bg_ball_empty
        }
        binding.ballBackground.setBackgroundResource(bgRes)

        // Animation
        binding.ballBackground.scaleX = 0.5f
        binding.ballBackground.scaleY = 0.5f
        binding.ballBackground.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun vibrate(duration: Long) {
        val vibrator = requireContext().getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = requireContext().getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    private fun showNewGameDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🔄 Nueva Partida")
            .setMessage("¿Iniciar una nueva partida?\nSe reiniciarán todas las bolas.")
            .setPositiveButton("Iniciar") { _, _ -> viewModel.startNewGame() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🚪 Salir")
            .setMessage("¿Deseas salir del modo cantador?\nLos jugadores serán desconectados.")
            .setPositiveButton("Salir") { _, _ -> findNavController().navigateUp() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
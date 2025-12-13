package com.bingoroyale.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import com.bingoroyale.databinding.FragmentPlayerBinding
import com.bingoroyale.model.BingoCard
import com.bingoroyale.model.NetworkEvent
import com.bingoroyale.ui.adapters.BingoCardAdapter
import com.bingoroyale.viewmodel.PlayerViewModel

class PlayerFragment : Fragment() {

    companion object {
        private const val TAG = "PlayerFragment"
    }

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModels()
    private var cardAdapter: BingoCardAdapter? = null
    private var currentMode = 75

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { showExitDialog() }

        binding.btnConnect.setOnClickListener {
            val isConnected = viewModel.gameState.value?.isConnected ?: false
            if (isConnected) {
                viewModel.disconnect()
            } else {
                viewModel.searchAndConnect()
                Toast.makeText(requireContext(), getString(R.string.searching), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBingo.setOnClickListener {
            viewModel.callBingo()
            vibrate(longArrayOf(0, 100, 50, 100, 50, 200))
        }

        binding.btnNewCard.setOnClickListener { showNewCardDialog() }

        binding.btnClearMarks.setOnClickListener {
            viewModel.clearAllMarks()
            vibrate(longArrayOf(0, 30))
        }
    }

    private fun setupRecyclerView() {
        cardAdapter = BingoCardAdapter { index ->
            viewModel.toggleCellMark(index)
            vibrate(longArrayOf(0, 20))
        }

        binding.rvBingoCard.apply {
            adapter = cardAdapter
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = GridLayoutManager(requireContext(), 5)
        }
    }

    private fun updateGridIfNeeded(mode: Int) {
        if (mode != currentMode) {
            currentMode = mode
            val columns = if (mode == 75) 5 else 9
            binding.rvBingoCard.layoutManager = GridLayoutManager(requireContext(), columns)
            binding.bingoHeader.visibility = if (mode == 75) View.VISIBLE else View.GONE
            Log.d(TAG, "Grid updated: columns=$columns")
        }
    }

    private fun observeViewModel() {
        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            Log.d(TAG, "State: mode=${state.mode}, connected=${state.isConnected}")

            // Actualizar grid
            updateGridIfNeeded(state.mode)

            // Actualizar cartón
            cardAdapter?.submitCard(state.card, state.markedCells)

            // Estado de conexión
            updateConnectionUI(state.isConnected, state.serverName)

            // Última bola
            updateLastBallUI(state.lastReceivedBall, state.mode)
        }

        viewModel.networkEvent.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe

            when (event) {
                is NetworkEvent.BallDrawn -> vibrate(longArrayOf(0, 50))
                is NetworkEvent.NewGame -> {
                    Toast.makeText(requireContext(), "🔄 Nueva partida", Toast.LENGTH_SHORT).show()
                }
                is NetworkEvent.Disconnected -> {
                    Toast.makeText(requireContext(), "📴 Desconectado", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            viewModel.clearNetworkEvent()
        }

        viewModel.showBingoAnimation.observe(viewLifecycleOwner) { show ->
            if (show == true) showBingoDialog()
        }

        viewModel.bingoNotification.observe(viewLifecycleOwner) { name ->
            if (name != null) {
                Toast.makeText(requireContext(), "🎉 $name cantó BINGO!", Toast.LENGTH_LONG).show()
                vibrate(longArrayOf(0, 100, 50, 100))
                viewModel.clearBingoNotification()
            }
        }
    }

    private fun updateConnectionUI(isConnected: Boolean, serverName: String?) {
        binding.tvConnectionStatus.text = if (isConnected)
            getString(R.string.connected_to, serverName ?: "Cantador")
        else
            getString(R.string.offline_mode)

        binding.tvConnectionStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isConnected) R.color.accent_green else R.color.text_secondary
            )
        )
        binding.btnConnect.text = if (isConnected) "🔌 Desconectar" else "🔗 Conectar"
    }

    private fun updateLastBallUI(ball: Int?, mode: Int) {
        if (ball != null && ball > 0) {
            binding.cardLastBall.visibility = View.VISIBLE
            val letter = if (mode == 75) BingoCard.getLetterForNumber(ball) else ""
            binding.tvReceivedBall.text = if (letter.isNotEmpty()) "$letter$ball" else ball.toString()

            val bgRes = when (letter) {
                "B" -> R.drawable.bg_ball_b
                "I" -> R.drawable.bg_ball_i
                "N" -> R.drawable.bg_ball_n
                "G" -> R.drawable.bg_ball_g
                "O" -> R.drawable.bg_ball_o
                else -> R.drawable.bg_ball_empty
            }
            binding.receivedBallBg.setBackgroundResource(bgRes)
        } else {
            binding.cardLastBall.visibility = View.GONE
        }
    }

    private fun showBingoDialog() {
        AlertDialog.Builder(requireContext(), R.style.BingoDialogTheme)
            .setTitle("🎉 ¡BINGO!")
            .setMessage("¡Has cantado BINGO!")
            .setPositiveButton("OK") { _, _ -> viewModel.dismissBingoAnimation() }
            .setCancelable(false)
            .show()
    }

    private fun showNewCardDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🔄 Nuevo Cartón")
            .setMessage("¿Generar nuevo cartón?")
            .setPositiveButton("Sí") { _, _ -> viewModel.generateNewCard() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🚪 Salir")
            .setMessage("¿Salir del modo jugador?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.disconnect()
                findNavController().navigateUp()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Ignorar
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cardAdapter = null
        _binding = null
    }
}
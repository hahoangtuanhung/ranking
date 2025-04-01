package com.example.rankingproject

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rankingproject.databinding.FragmentGameRankingBinding
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.facemesh.FaceMeshDetection.getClient
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.otaliastudios.cameraview.controls.AudioCodec
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.size.AspectRatio
import com.otaliastudios.cameraview.size.SizeSelectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

class GameRankingFragment : Fragment() {

    private var _binding: FragmentGameRankingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(requireActivity())[GameViewModel::class.java]
    }

    private var listRanking = mutableListOf<String>()
    private var imgRanking: String = ""
    private var indexRanking = 0
    private var isWaitingForAnswer = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val processedPositions = mutableSetOf<Int>()
    private lateinit var rankingAdapter: RankingAdapter
    private var isDestroyView = false
    private var canReloadRanking = false
    private var isStartGame = false
    private var isStartRanking = true
    private var isRandomRanking = false
    private var startTime: Long = 0
    private var lastPositionRanking = -1
    private var questions = listOf<Question>()
    private var questionList = listOf<String>()
    private var imageHeight = 0
    private var imageWidth = 0
    private val orientations = SparseIntArray().apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }
    private val listLeftEye =
        listOf(463, 414, 286, 258, 257, 259, 260, 467, 359, 255, 339, 254, 253, 252, 256, 341)
    private val listRightEye =
        listOf(247, 30, 29, 27, 28, 56, 190, 243, 112, 26, 22, 23, 24, 110, 25, 130)
    private val noseBottom = listOf(131, 4, 360)
    private val faceOval = listOf(10, 152)
    private var scanning = true
    private var isAllRandomCelebrityStart = true
    private var isAllRandomCelebrityComplete = false
    private var isAnimating = false
    private var currentOffset = 0f
    private var isMovingDown = true
    private var isClickStop = false
    private var maxMovingScanning = 0f
    val ranking1Game = Game(
        id = "ranking1",
        categoryItem = "Female Actress",
        name = "Ranking",
        type = "Ranking",
        category = "Ranking",
        questions = listOf(
            Question(
                question = "A1,A2,A3,A4,A5,A6,A7,A8,A9,A10",
                trueAnswer = "",
                falseAnswer = ""
            )
        ),
        thumb = "ic_female_ac_category",
        isFamous = false
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameRankingBinding.inflate(inflater, container, false)
        return binding.root

        setupCameraView()
        viewModel.setGame()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listRanking =
            viewModel.game.value?.questions?.first()?.question?.split(",")?.toMutableList()
                ?: mutableListOf()
        setupRcvRanking()
        setupCameraView()
        initObserver()
        handleRanking(listRanking)
        binding.layoutRankingGame.btnStop.hide()

        binding.layoutRankingGame.btnStart.setSingleClick {
            isStartGame = true
            isStartRanking = true
            canReloadRanking = false
            indexRanking = 0
            processedPositions.clear()
            binding.layoutRankingGame.btnStop.show()
            binding.layoutRankingGame.btnStop.setSingleClick {
                isClickStop = true
                isClickStop = true
                isStartGame = false
                isStartRanking = false
                canReloadRanking = false
                isRandomRanking = false
                indexRanking = 0
                lastPositionRanking = -1
                processedPositions.clear()

                // Hiện lại nút start, ẩn stop
                binding.layoutRankingGame.btnStart.show()
                binding.layoutRankingGame.btnStop.hide()

                // Reset lại danh sách
                questionList = questions.firstOrNull()?.question?.split(",") ?: listOf()
                listRanking = questionList.toMutableList()
                setupRcvRanking()
            }
            binding.layoutRankingGame.btnStart.clearAnimation()
            binding.layoutRankingGame.btnStart.hide()
        }

    }

    fun initObserver() {

        Log.d("gametest", "$ranking1Game")
        questions = ranking1Game.questions

        if (questions.isNotEmpty() && (ranking1Game.category == Constants.GameCategory.RANKING)) {
            questionList = questions[0].question.split(",")
            listRanking = questionList.toMutableList()
        }
        Log.d("questionList", "$questionList")
                binding.layoutRankingGame.root.show()
                binding.layoutRankingGame.rcvRanking.show()
                binding.layoutRankingGame.title.hide()
                binding.layoutRankingGame.imgRanking.show()
                setupRcvRanking()
                handleRanking(listRanking)
        viewModel.actionStopGame.observe(viewLifecycleOwner) {
            isClickStop = true
        }

    }

    private fun setupRcvRanking() {
        val listItem =
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10") //10 vi tri xep hang
        val adapter = context?.let { context ->
            RankingAdapter(listItem) { item, numberColum1, tvNumber2, position ->
                if (!isStartGame) {
                    return@RankingAdapter
                }
                if (!canReloadRanking) {
                    return@RankingAdapter
                }
                if (lastPositionRanking == position) {
                    return@RankingAdapter
                }
                lastPositionRanking = position
                handleAnswerRanking()
                if (!isRandomRanking && listRanking.size > 0) {
                    if (processedPositions.contains(position)) {
                        return@RankingAdapter
                    }
                    processedPositions.add(position)
                    // chay lai startRanking random khi chon vao item
                    if (listRanking.size > 1) {
                        isStartRanking = true
                        startRanking()
                    }
                    loadImageFromAsset(context, "ranking/$imgRanking.webp", item)
                    numberColum1.text = (position + 1).toString()
                    val animator = ObjectAnimator.ofFloat(
                        numberColum1, "translationX", numberColum1.width.toFloat(), 0f
                    )
                    animator.duration = 500
                    animator.start()
                    tvNumber2.hide()
                    indexRanking += 1
                    if (listRanking.size > 0) {
                        listRanking.remove(imgRanking)
                    }
                }
            }
        }

        binding.layoutRankingGame.rcvRanking.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )

        binding.layoutRankingGame.rcvRanking.adapter = adapter

    }

    private fun handleRanking(data: List<String>) {
        Log.d("handleRanking", "1")
        context?.let { context ->
            if (data.isEmpty()) return@let
            imgRanking = data.random()
            Log.d("handleRanking", "2")
            // chạy random
            loadImageFromAsset(
                context,
                "ranking/$imgRanking.webp",
                binding.layoutRankingGame.imgRanking //hiển thị 1 ảnh để người chơi chọn
            )

        }
    }

    private fun handleAnswerRanking() {
        if (isStartRanking || viewModel.game.value == null) return
        canReloadRanking = false
        isRandomRanking = System.currentTimeMillis() - startTime <= 2000
        if (!isRandomRanking) {
            startTime = System.currentTimeMillis()
            val updateInterval = 20L
            val maxDuration = 2000L

            val rankingRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < maxDuration) {
                        handleRanking(listRanking)
                        handler.postDelayed(this, updateInterval) // Chỉ thêm lại callback cần thiết
                    } else {
                        canReloadRanking = true
                        handler.removeCallbacks(this) // Chỉ loại bỏ Runnable này
                    }
                }
            }
            handler.removeCallbacks(rankingRunnable) // Xóa chỉ Runnable này trước khi đăng lại
            handler.post(rankingRunnable)
        }
    }

    private fun getScreenSize(context: Context): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels

        val dpInPixels = (60 * displayMetrics.density).toInt()
        val screenHeight = displayMetrics.heightPixels - dpInPixels

        return Pair(screenWidth, screenHeight)
    }

    private fun isValidRGB(image: Image): Boolean {
        val plane0 = image.planes[0]
        val plane1 = image.planes[1]
        val plane2 = image.planes[2]

        // Kiểm tra kích thước bộ đệm và stride của các plane
        if (plane0.pixelStride != 1 || plane1.pixelStride != 2 || plane2.pixelStride != 2) {
            return false // Sai định dạng pixelStride
        }

        if (plane0.rowStride < image.width || plane1.rowStride < image.width / 2 || plane2.rowStride < image.width / 2) {
            return false // Sai định dạng rowStride
        }

        // Lấy giá trị Y, U, V tại một điểm và kiểm tra
        val x = image.width / 2
        val y = image.height / 2
        val yb = plane0.buffer[x + plane0.rowStride * y].toInt() and 0xFF
        val ub = plane1.buffer[(x / 2) + plane1.rowStride * (y / 2)].toInt() and 0xFF
        val vb = plane2.buffer[(x / 2) + plane2.rowStride * (y / 2)].toInt() and 0xFF

        // Chuyển đổi sang RGB và kiểm tra giá trị kết quả
        val rgb = yuvToRGB(yb, ub, vb)
        return rgb.all { it in 0..255 }
    }

    private fun yuvToRGB(y: Int, u: Int, v: Int): IntArray {
        val c = y - 16
        val d = u - 128
        val e = v - 128

        val r = (1.164 * c + 1.596 * e).toInt().coerceIn(0, 255)
        val g = (1.164 * c - 0.392 * d - 0.813 * e).toInt().coerceIn(0, 255)
        val b = (1.164 * c + 2.017 * d).toInt().coerceIn(0, 255)

        return intArrayOf(r, g, b)
    }

    private fun setupCameraView() {
        val screenWidth = getScreenSize(requireContext()).first
        val screenHeight = getScreenSize(requireContext()).second
        var isGetSizeImage = false
        val detector = getClient(
            FaceMeshDetectorOptions.Builder().setUseCase(1).build()
        )
        val dimensWidth = SizeSelectors.minWidth(1000)
        val dimensHeight = SizeSelectors.minHeight(2000)
        val dimensions = SizeSelectors.and(dimensWidth, dimensHeight)
        val ratio = SizeSelectors.aspectRatio(
            AspectRatio.of(screenWidth, screenHeight), 0f
        )

        val result = SizeSelectors.or(
            SizeSelectors.and(ratio, dimensions), ratio, SizeSelectors.biggest()
        )
        binding.camera.setPictureSize(result)
        binding.camera.setPreviewStreamSize(result)
        val triangleData = TriangleData()

        // Gọi hàm để lấy danh sách index tam giác
        val indices = triangleData.getTriangleIndices()
        binding.camera.apply {
            setRequestPermissions(false)
            setLifecycleOwner(viewLifecycleOwner)
            audioBitRate = 320000
            audioCodec = AudioCodec.AAC
            videoBitRate = 5000000
            snapshotMaxHeight = 1080
            snapshotMaxWidth = 720
            open()

            addFrameProcessor { frame ->
                try {
                    if (isDestroyView) return@addFrameProcessor
                    if (frame.dataClass == Image::class.java) {
                        val image = frame.getData<Image>()
                        if (image.format != ImageFormat.YUV_420_888) return@addFrameProcessor
                        if (!isValidRGB(image) || image.height <= 0 || image.width <= 0) return@addFrameProcessor
                        if (!isGetSizeImage) {
                            isGetSizeImage = true
                            imageHeight = image.height
                            imageWidth = image.width
                        }
                        val faces = Tasks.await(
                            detector.process(
                                image, getRotationCamera()
                            )
                        )
                        val face = faces.first().allPoints

                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                val rightEyePoint = listRightEye.mapNotNull { index ->
                                    face.getOrNull(index)?.let { point ->
                                        PointF(point.position.x, point.position.y)
                                    }
                                }

                                val leftEyePoint = listLeftEye.mapNotNull { index ->
                                    face.getOrNull(index)?.let { point ->
                                        PointF(point.position.x, point.position.y)
                                    }
                                }

                                val nosePoints = noseBottom.mapNotNull { index ->
                                    face.getOrNull(index)?.let { point ->
                                        PointF(point.position.x, point.position.y)
                                    }
                                }

                                val facePoints = faceOval.mapNotNull { index ->
                                    face.getOrNull(index)?.let { point ->
                                        PointF(point.position.x, point.position.y)
                                    }
                                }

                                val faceCoordinates: List<PointF> = face.mapNotNull { point ->
                                    point?.let { PointF(it.position.x, it.position.y) }
                                }

                                            if (indexRanking == 10) {
                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    binding.layoutRankingGame.imgRanking.hide()
                                                }

                                            }

                                            if (faces.isEmpty()) {
                                                binding.layoutRankingGame.imgRanking.rotation = 0f
                                                binding.layoutRankingGame.imgRanking.translationX =
                                                    0f
                                                binding.layoutRankingGame.imgRanking.translationY =
                                                    0f
                                                image.close()
                                                return@launch
                                            }


                                            val middleX =
                                                nosePoints.map { it.x }.average().toFloat()
                                            val middleY =
                                                nosePoints.map { it.y }.average().toFloat()
                                            //Di chuyen theo mat
                                            val translatedX =
                                                binding.camera.width.toFloat() - (middleX * (binding.camera.width.toFloat() / imageHeight.toFloat()))
                                            val translatedY =
                                                middleY * (binding.camera.height.toFloat() / imageWidth.toFloat())
                                            //Diem mat
                                            val positionEyeLeft = Position(
                                                leftEyePoint.map { it.x }.average()
                                                    .toFloat(),
                                                leftEyePoint.map { it.y }.average()
                                                    .toFloat()
                                            )
                                            val positionEyeRight = Position(
                                                rightEyePoint.map { it.x }.average()
                                                    .toFloat(),
                                                rightEyePoint.map { it.y }.average()
                                                    .toFloat()
                                            )
                                            //Diem mui
                                            val positionNose = Position(middleX, middleY)
                                            //Hieu goc giua mat phai - mui va mat trai - mui
//                                            val angleDifference = determineHeadDirection(
//                                                positionNose,
//                                                positionEyeLeft,
//                                                positionEyeRight
//                                            )
                                            // Hieu khoang cach mat phai - mui va mat trai - mui
                                            val distanceDifference =
                                                calculateDistanceDifference(
                                                    positionNose,
                                                    positionEyeLeft,
                                                    positionEyeRight
                                                )
                                            // Tính khoảng cách giữa hai mắt
                                            val eyeDistance =
                                                sqrt(
                                                    (rightEyePoint.map { it.x }.average()
                                                        .toFloat() - leftEyePoint.map { it.x }
                                                        .average().toFloat()).pow(
                                                        2
                                                    ) + (rightEyePoint.map { it.y }.average()
                                                        .toFloat() - leftEyePoint.map { it.y }
                                                        .average().toFloat()).pow(
                                                        2
                                                    ))

                                            val textView =
                                                if (viewModel.game.value?.type == Constants.GameType.RANKING) {
                                                    null
                                                } else {
                                                    binding.layoutRankingGame.title
                                                }

                                            handleBeforeStartRanking(
                                                translatedX,
                                                translatedY,
                                                eyeDistance,
                                                distanceDifference,
                                                binding.layoutRankingGame.imgRanking,
                                                textView,
                                            )
                                            return@launch


                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun calculateDistancePointF(point1: PointF, point2: PointF): Float {
        return sqrt((point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2))
    }

    private fun getRotationCamera(): Int {
        return try {
            val cameraFacing = binding.camera.facing
            val cameraId = if (cameraFacing == Facing.BACK) "0" else "1"
            getRotationCompensation(cameraId, cameraFacing == Facing.FRONT)
        } catch (e: Exception) {
            e.printStackTrace()
            270
        }
    }

    private fun handleBeforeStartRanking(
        middleX: Float,
        middleY: Float,
        eyeDistance: Float,
        numberDifference: Float,
        imgView: ImageView,
        textView: TextView? = null,
        background: CardView? = null
    ) {
        val scaleFactor = 0.007f
        val scale = 1 + (eyeDistance * scaleFactor)
        imgView.x = middleX - imgView.width / 2
        imgView.y = middleY - 2.5f * imgView.height * scale
        imgView.scaleX = scale
        imgView.scaleY = scale
        imgView.rotationY = -numberDifference * 0.5f

        textView?.apply {
            x = middleX - width / 2
            y = middleY - 4.6f * height * scale
            scaleX = scale
            scaleY = scale
            rotationY = -numberDifference * 0.5f
        }

        background?.apply {
            x = middleX - width / 2
            y = middleY - 2f * height * scale
            scaleX = scale
            scaleY = scale
            rotationY = -numberDifference * 0.5f
        }

        if (isStartGame) {
            startRanking()
        }
    }

    private fun startRanking() {
        if (!isStartRanking) return
        isRandomRanking = System.currentTimeMillis() - startTime <= 2000
        isStartRanking = false

        if (!isRandomRanking) {
            startTime = System.currentTimeMillis()
            val updateInterval = 20L
            val maxDuration = 2000L

            val rankingRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < maxDuration) {
                        handleRanking(listRanking)
                        handler.postDelayed(this, updateInterval)
                    } else {
                        canReloadRanking = true
                        handler.removeCallbacks(this) // Xóa chỉ riêng Runnable này
                    }
                }
            }
            handler.removeCallbacks(rankingRunnable) // Đảm bảo xóa Runnable cụ thể nếu tồn tại
            handler.post(rankingRunnable)
        }
    }

    private fun calculateEuclideanDistance(point1: Position, point2: Position): Float {
        return sqrt((point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2))
    }

    private fun calculateDistanceDifference(
        nosePoint: Position, leftEyePoint: Position, rightEyePoint: Position
    ): Float {
        val distanceLeft = calculateEuclideanDistance(nosePoint, leftEyePoint)
        val distanceRight = calculateEuclideanDistance(nosePoint, rightEyePoint)

        return distanceRight - distanceLeft
    }

    private fun getRotationCompensation(cameraId: String, isFrontFacing: Boolean): Int {
        try {
            activity?.let { activity ->
                val orientation = orientations[activity.windowManager.defaultDisplay.rotation]
                val cameraManager =
                    activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
                val sensorOrientation = cameraManager?.getCameraCharacteristics(cameraId)
                    ?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: return 270

                val rotation = if (isFrontFacing) {
                    (sensorOrientation + orientation) % 360
                } else {
                    (sensorOrientation - orientation + 360) % 360
                }
                return rotation
            }
            return 270
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 270
    }

    override fun onResume() {
        if (!binding.camera.isOpened) {
            binding.camera.open()
        }
        super.onResume()
    }

    override fun onPause() {
        if (binding.camera.isOpened) {
            binding.camera.close()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }
}

data class Position(val x: Float, val y: Float)
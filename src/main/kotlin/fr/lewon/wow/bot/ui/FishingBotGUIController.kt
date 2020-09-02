package fr.lewon.wow.bot.ui

import fr.lewon.wow.bot.util.RobotUtil
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import java.awt.*
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs


class FishingBotGUIController : Initializable {

    @FXML
    private lateinit var actionsPane: TitledPane
    @FXML
    private lateinit var gameScreenRegionSelector: ChoiceBox<String>
    @FXML
    private lateinit var floaterDisplayImageView: ImageView
    @FXML
    private lateinit var status: Label

    private val checkImg = ImageIO.read(javaClass.getResource("/check.png"))

    private var fishingSpellPos = Pair(0, 0)

    private var fishingOn = false

    private lateinit var graphicsDevicesAndIds: List<Pair<GraphicsDevice, String>>

    private var currentScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice

    private var displayBounds: Rectangle? = null
    private var postDisplayImageTreatment: (BufferedImage) -> Unit = {}

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        this.graphicsDevicesAndIds = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                .map { Pair(it, it.iDstring) }

        graphicsDevicesAndIds.forEach {
            this.gameScreenRegionSelector.items.add(it.second)
        }
        this.gameScreenRegionSelector.value = currentScreen.iDstring
        gameScreenRegionSelector.selectionModel.selectedIndexProperty().addListener { _, _, newVal ->
            currentScreen = graphicsDevicesAndIds[newVal.toInt()].first
        }

        Thread {
            display()
        }.start()
    }

    private fun display() {
        var moving = false
        var dx1 = 0.0
        var dx2 = 0.0
        var dy1 = 0.0
        var dy2 = 0.0
        var currentX = 0.0
        var currentY = 0.0
        var currentW = currentScreen.defaultConfiguration.bounds.width.toDouble() / 2.0
        var currentH = currentScreen.defaultConfiguration.bounds.height.toDouble() / 2.0
        var targetBounds = Rectangle(currentX.toInt(), currentY.toInt(), currentW.toInt(), currentH.toInt())
        while (true) {
            if (!moving) {
                displayBounds?.let {
                    targetBounds = it
                    dx1 = (it.x.toDouble() - currentX) / 5.0
                    dy1 = (it.y.toDouble() - currentY) / 5.0
                    dx2 = ((it.x.toDouble() + it.width.toDouble()) - (currentX + currentW)) / 5.0
                    dy2 = ((it.y.toDouble() + it.height.toDouble()) - (currentY + currentH)) / 5.0
                    moving = true
                    displayBounds = null
                }
            }

            if (moving && abs(currentX - targetBounds.x.toDouble()) < abs(dx1)
                    && abs(currentY - targetBounds.y.toDouble()) < abs(dy1)
                    && abs(currentX + currentW - targetBounds.x.toDouble() - targetBounds.width.toDouble()) < abs(dx2)
                    && abs(currentY + currentH - targetBounds.y.toDouble() - targetBounds.height.toDouble()) < abs(dy2)
            ) {
                currentX = targetBounds.x.toDouble()
                currentW = targetBounds.width.toDouble()
                currentY = targetBounds.y.toDouble()
                currentH = targetBounds.height.toDouble()
                moving = false
            } else if (moving) {
                currentW += dx2 - dx1
                currentX += dx1
                currentH += dy2 - dy1
                currentY += dy1
            }

            var image = captureGameImage()
            postDisplayImageTreatment(image)
            val finalX = maxOf(minOf(currentX.toInt(), image.width - 5), 0)
            val finalY = maxOf(minOf(currentY.toInt(), image.height - 5), 0)
            val finalW = maxOf(1, minOf(currentW.toInt(), image.width - finalX))
            val finalH = maxOf(1, minOf(currentH.toInt(), image.height - finalY))
            image = image.getSubimage(finalX, finalY, finalW, finalH)
            Platform.runLater { floaterDisplayImageView.image = buildImageView(image) }
        }
    }

    private fun buildImageView(img: BufferedImage): Image {
        val wr = WritableImage(img.width, img.height)
        val pw = wr.pixelWriter
        for (x in 0 until img.width) {
            for (y in 0 until img.height) {
                pw.setArgb(x, y, img.getRGB(x, y))
            }
        }
        return wr
    }

    private fun enableButtons(enabled: Boolean) {
        actionsPane.content.disableProperty().value = !enabled
    }

    @Synchronized
    fun processBtnExecution(execution: () -> Unit) {
        Thread {
            enableButtons(false)
            Platform.runLater { status.style = "-fx-background-color: grey;"; }
            try {
                execution.invoke()
                Platform.runLater { status.style = "-fx-background-color: green;"; }
            } catch (e: Exception) {
                Platform.runLater { status.style = "-fx-background-color: red;"; }
            } finally {
                enableButtons(true)
            }
        }.start()
    }

    @FXML
    fun cancelFishing(actionEvent: ActionEvent?) {
        fishingOn = false
    }

    @FXML
    fun selectFishingSpellPosition(actionEvent: ActionEvent?) {
        processBtnExecution {
            val point = registerMousePos()
            fishingSpellPos = Pair(point.x, point.y)
        }
    }

    private fun registerMousePos(): Point {
        val screenBounds = currentScreen.defaultConfiguration.bounds
        for (i in 3 downTo 1) {
            Thread.sleep(1000)
        }
        return MouseInfo.getPointerInfo().location
                .also {
                    it.x -= screenBounds.x
                    it.y -= screenBounds.y
                }
    }

    @FXML
    fun startFishing(actionEvent: ActionEvent?) {
        processBtnExecution {
            fishingOn = true
            while (fishingOn) {
                RobotUtil.click(
                        currentScreen.defaultConfiguration.bounds.x + fishingSpellPos.first,
                        currentScreen.defaultConfiguration.bounds.y + fishingSpellPos.second
                )
                try {
                    Thread.sleep(2000)
                    val initialFloaterLoc = findFloater(10)
                    waitUntilFloaterMove(initialFloaterLoc, 30)
                    RobotUtil.click(
                            currentScreen.defaultConfiguration.bounds.x + currentScreen.defaultConfiguration.bounds.width / 4 + initialFloaterLoc.x + initialFloaterLoc.width / 2,
                            currentScreen.defaultConfiguration.bounds.y + currentScreen.defaultConfiguration.bounds.height / 4 + initialFloaterLoc.y + initialFloaterLoc.height / 2
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Thread.sleep(500)
                this.postDisplayImageTreatment = {}
                val capture = captureGameImage()
                this.displayBounds = Rectangle(0, 0, capture.width, capture.height)
                Thread.sleep(1500)
            }
        }
    }

    private fun waitUntilFloaterMove(initialFloaterLoc: Rectangle, timeOut: Int) {
        val start = System.currentTimeMillis()
        val x = maxOf(initialFloaterLoc.x - 50, 0)
        val y = maxOf(initialFloaterLoc.y - 50, 0)
        val w = minOf(initialFloaterLoc.width + 100, currentScreen.defaultConfiguration.bounds.width / 2 - x - 5)
        val h = minOf(initialFloaterLoc.height + 100, currentScreen.defaultConfiguration.bounds.height / 2 - y - 5)
        val boundingBox = Rectangle(x, y, w, h)
        displayBounds = boundingBox
        while (System.currentTimeMillis() - start < timeOut * 1000) {
            val capture = captureGameImage()
            val newFloaterLoc = getFloaterLoc(capture) ?: continue
            postDisplayImageTreatment = {
                val graphics = it.graphics as Graphics2D
                graphics.color = Color.RED
                graphics.stroke = BasicStroke(3f)
                graphics.drawRect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height)
                graphics.drawRect(newFloaterLoc.x, newFloaterLoc.y, newFloaterLoc.width, newFloaterLoc.height)
                graphics.drawLine(
                        boundingBox.x,
                        initialFloaterLoc.y + initialFloaterLoc.height / 2,
                        boundingBox.x + boundingBox.width - 1,
                        initialFloaterLoc.y + initialFloaterLoc.height / 2
                )
            }
            if (newFloaterLoc.y > initialFloaterLoc.y + initialFloaterLoc.height / 2 || newFloaterLoc.y < initialFloaterLoc.y - initialFloaterLoc.height / 2) {
                postDisplayImageTreatment = {
                    val graphics = it.graphics
                    graphics.drawImage(checkImg, boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height, null)
                }
                return
            }
        }
        error("Floater move timeout")
    }

    private fun findFloater(timeOut: Int): Rectangle {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeOut * 1000) {
            val capture = captureGameImage()
            return getFloaterLoc(capture) ?: continue
        }
        error("Find floater timeout")
    }

    private fun getFloaterLoc(gameImage: BufferedImage): Rectangle? {
        val floaterPosPoints = ArrayList<Pair<Int, Int>>()

        val exploredByRow = mutableMapOf<Int, MutableList<Int>>()
        for (row in 0 until gameImage.height) {
            exploredByRow[row] = ArrayList()
        }
        for (row in 0 until gameImage.height) {
            for (col in 0 until gameImage.width) {
                if (exploredByRow[row]?.contains(col) == false) {
                    val color = Color(gameImage.getRGB(col, row))
                    val r = color.red
                    val g = color.green
                    val b = color.blue
                    if (r >= 1.8 * g && r >= 1.8 * b) {
                        getCluster(gameImage, row, col, { it.red >= 1.8 * it.green && it.red >= 1.8 * it.blue }, exploredByRow)
                                .takeIf { it.size > 50 }
                                ?.let { floaterPosPoints.addAll(it) }
                    }
                }
            }
        }

        if (floaterPosPoints.isEmpty()) {
            return null
        }

        val floaterTop = floaterPosPoints.minBy { it.second } ?: Pair(0, 0)
        val floaterBot = floaterPosPoints.maxBy { it.second } ?: Pair(0, 0)
        val floaterLeft = floaterPosPoints.minBy { it.first } ?: Pair(0, 0)
        val floaterRight = floaterPosPoints.maxBy { it.first } ?: Pair(0, 0)

        return Rectangle(floaterLeft.first, floaterTop.second, floaterRight.first - floaterLeft.first, floaterBot.second - floaterTop.second)
    }

    private fun getCluster(
            img: BufferedImage,
            row: Int,
            col: Int,
            isColorOk: (Color) -> Boolean,
            exploredByRow: Map<Int, MutableList<Int>>
    ): List<Pair<Int, Int>> {
        val colouredTiles = mutableListOf(Pair(col, row))
        var frontier = listOf(Pair(col, row))
        val explored = mutableListOf(Pair(col, row))
        while (frontier.isNotEmpty()) {
            val newFrontier = ArrayList<Pair<Int, Int>>()
            for (n in frontier) {
                colouredTiles.add(n)
                exploredByRow[n.second]?.add(n.first)
                val neighbors = listOf(
                        Pair(n.first + 1, n.second),
                        Pair(n.first - 1, n.second),
                        Pair(n.first, n.second + 1),
                        Pair(n.first, n.second - 1)
                )
                for (neighbor in neighbors) {
                    if (!explored.contains(neighbor) && neighbor.first >= 0 && neighbor.first < img.width && neighbor.second >= 0 && neighbor.second < img.height) {
                        explored.add(neighbor)
                        if (isColorOk(Color(img.getRGB(neighbor.first, neighbor.second)))) {
                            newFrontier.add(neighbor)
                        }
                    }
                }
            }
            frontier = newFrontier
        }
        return colouredTiles
    }


    @Synchronized
    fun captureGameImage(): BufferedImage {
        val bounds = currentScreen.defaultConfiguration.bounds
        val screenshot = RobotUtil.screenShot(bounds.x, bounds.y, bounds.width, bounds.height)
        return screenshot.getSubimage(screenshot.width / 4, screenshot.height / 4, screenshot.width / 2, screenshot.height / 2)
    }

}
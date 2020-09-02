package fr.lewon.wow.bot

import fr.lewon.wow.bot.ui.FishingBotGUIController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlin.system.exitProcess


class FishingBotFXApp : Application() {

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("/scenes/main_scene.fxml"))
        val rootElement = loader.load() as VBox
        val scene = Scene(rootElement, rootElement.prefWidth, rootElement.prefHeight)

        primaryStage.minWidth = rootElement.minWidth
        primaryStage.minHeight = rootElement.minHeight
        primaryStage.title = "WoW Fishing Bot"
        primaryStage.isAlwaysOnTop = true
        primaryStage.scene = scene

        // show the GUI
        primaryStage.show()
        // set the proper behavior on closing the application
        val controller: FishingBotGUIController = loader.getController()
        primaryStage.setOnCloseRequest { exitProcess(0) }
    }

    fun run() {
        launch()
    }

}

fun main() {
    FishingBotFXApp().run()
}
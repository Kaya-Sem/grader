package com.jaytux.grader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jaytux.grader.App
import com.jaytux.grader.data.Database

fun main(){
    Database.init()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Grader",
        ) {
            App()
        }
    }
}
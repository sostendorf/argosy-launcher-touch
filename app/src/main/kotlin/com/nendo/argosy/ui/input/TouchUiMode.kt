package com.nendo.argosy.ui.input

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * True while the app is drawing its touch-first chrome (top bar + bottom navigation) instead of
 * relying on the gamepad guide bar.
 *
 * This is a rendering switch, never an input switch: the gamepad paths stay subscribed and the
 * ViewModel-owned focus indices keep working exactly as they do on a handheld, so plugging a
 * controller into a phone mid-session hands control back without any state being rebuilt.
 */
val LocalTouchUi = staticCompositionLocalOf { false }

/**
 * A device counts as a gamepad when it reports GAMEPAD or JOYSTICK, matching the test the hotkey
 * binder already uses. Virtual devices are excluded where the platform can tell us
 * ([InputDevice.isVirtual] is API 29+); on older releases a virtual joystick would suppress the
 * touch chrome, which is the safe direction to be wrong in - it leaves the launcher exactly as it
 * ships today.
 */
private fun isGamepad(device: InputDevice): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && device.isVirtual) return false
    val sources = device.sources
    return (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
        (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
}

private fun anyGamepadAttached(): Boolean =
    InputDevice.getDeviceIds().any { id ->
        InputDevice.getDevice(id)?.let { isGamepad(it) } == true
    }

/**
 * Resolves whether the touch chrome should show for this device, right now.
 *
 * Two conditions, both live:
 * - the device is a touchscreen and not a leanback (TV) device, and
 * - no gamepad is currently attached.
 *
 * A handheld with a built-in controller (Retroid, Odin, Anbernic) reports a gamepad at all times
 * and therefore never sees the touch chrome. A phone with a controller paired mid-session loses
 * the chrome the moment the controller connects and gets it back when it disconnects, which is
 * why the InputManager listener is registered rather than the value being read once.
 */
@Composable
fun rememberTouchUiEnabled(): Boolean {
    val context = LocalContext.current
    val touchCapable = remember(context) {
        val pm = context.packageManager
        pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) &&
            !pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    var gamepadAttached by remember { mutableStateOf(anyGamepadAttached()) }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                gamepadAttached = anyGamepadAttached()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                gamepadAttached = anyGamepadAttached()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                gamepadAttached = anyGamepadAttached()
            }
        }
        inputManager.registerInputDeviceListener(listener, null)
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    return touchCapable && !gamepadAttached
}
